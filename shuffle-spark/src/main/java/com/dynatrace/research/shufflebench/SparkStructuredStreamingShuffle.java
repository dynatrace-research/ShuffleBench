/*
* Runs on IntelliJ require the following VM options (to run in Java 21):
* --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED
*  */

package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.consumer.*;
import com.dynatrace.research.shufflebench.matcher.MatcherService;
import com.dynatrace.research.shufflebench.matcher.SimpleMatcherService;
import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import io.smallrye.config.SmallRyeConfig;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.FlatMapGroupsWithStateFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.execution.streaming.RealTimeTrigger;
import org.apache.spark.sql.streaming.*;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import scala.Tuple2;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;

public class SparkStructuredStreamingShuffle {

    private static final String APPLICATION_ID = "shufflebench-sparkStructuredStreaming";

    public static void main(String[] args) throws StreamingQueryException, TimeoutException {

        SparkSession spark = SparkSession.builder()
                //.master("local[1]")
                .appName(APPLICATION_ID)
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .config("spark.kryo.registrator", CustomKryoRegistrator.class.getName())
                .config("spark.sql.streaming.stateStore.providerClass",
                        "org.apache.spark.sql.execution.streaming.state.RocksDBStateStoreProvider")
                .config("spark.serializer.objectStreamReset", 0)
                .config("spark.sql.streaming.metricsEnabled", "false")
                //.config("","false")

                .getOrCreate();
        //Config config = ConfigProviderResolver.instance().getConfig();
        // Try to load MicroProfile Config, but fall back to environment variables
        Config config = loadMicroProfileConfig();
        SmallRyeConfig smallRyeConfig = tryUnwrapSmallRye(config);

        final String kafkaBootstrapServers = getRequiredString(config, "kafka.bootstrap.servers");
        final String kafkaInputTopic = getRequiredString(config, "kafka.topic.input");
        final String kafkaOutputTopic = getRequiredString(config, "kafka.topic.output");
        // Trigger configuration:
        //   spark.trigger.mode      = default | processing | continuous | realtime   (default: "default" = Spark micro-batch)
        //   spark.trigger.interval  = e.g. "200 milliseconds"  (used by processing/continuous/realtime)
        //   spark.trigger.realtime  = legacy fallback for the realtime interval
        final String sparkTriggerMode = getOptionalString(config, "spark.trigger.mode").orElse("default");
        final String sparkTriggerInterval = getOptionalString(config, "spark.trigger.interval")
                .or(() -> getOptionalString(config, "spark.trigger.realtime"))
                .orElse(null);
        Optional<Integer> maxOffsetsPerTrigger = getOptionalInt(config, "spark.max.offsets.per.trigger");
        Optional<Integer> minOffsetsPerTrigger = getOptionalInt(config, "spark.min.offsets.per.trigger");
        DataStreamReader kafkaReader = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("subscribe", kafkaInputTopic)
                .option("startingOffsets", "latest");

        if (maxOffsetsPerTrigger.isPresent()) {
            kafkaReader = kafkaReader.option("maxOffsetsPerTrigger", maxOffsetsPerTrigger.get());
        }
        if (minOffsetsPerTrigger.isPresent()) {
            kafkaReader = kafkaReader.option("minOffsetsPerTrigger", minOffsetsPerTrigger.get());
        }

        final Dataset<Row> kafkaStream = kafkaReader
                .load()
                .select("value", "timestamp");

        // Extract config values to local final variables (these ARE serializable)
        final Map<Double, Integer> selectivities =
                smallRyeConfig != null
                        ? smallRyeConfig.getOptionalValues("matcher.selectivities", Double.class, Integer.class).orElse(null)
                        : null;
        final double totalSelectivity = getRequiredDouble(config, "matcher.zipf.total.selectivity");
        final int numRules = getRequiredInt(config, "matcher.zipf.num.rules");
        final double s = getRequiredDouble(config, "matcher.zipf.s");
        final int outputRate = getRequiredInt(config, "consumer.output.rate");
        final int stateSizeBytes = getRequiredInt(config, "consumer.state.size.bytes");
        final boolean initCountRandom = getRequiredBoolean(config, "consumer.init.count.random");
        final long initCountSeed = getRequiredLong(config, "consumer.init.count.seed");

        Dataset<Tuple2<String, TimestampedRecord>> streamingWithKeys = kafkaStream
                .as(Encoders.tuple(Encoders.BINARY(), Encoders.TIMESTAMP()))
                .map(
                        (MapFunction<Tuple2<byte[], Timestamp>, TimestampedRecord>) timestampedArray ->
                                new TimestampedRecord(timestampedArray._2.getTime(), timestampedArray._1),
                        // If Kryo still gives trouble here, switch to javaSerialization
                        Encoders.kryo(TimestampedRecord.class)
                        // Encoders.javaSerialization(TimestampedRecord.class)
                )
                .flatMap(
                        new MatcherFlatMapFunction(selectivities, totalSelectivity, numRules, s),
                        Encoders.tuple(Encoders.STRING(),
                                // Same note: switch to javaSerialization if needed
                                Encoders.kryo(TimestampedRecord.class))
                        // Encoders.tuple(Encoders.STRING(), Encoders.javaSerialization(TimestampedRecord.class))
                );

        Dataset<Tuple2<String, byte[]>> statefulAggregation = streamingWithKeys
                .groupByKey(
                        (MapFunction<Tuple2<String, TimestampedRecord>, String>) value -> value._1,
                        Encoders.STRING()
                )
                .flatMapGroupsWithState(
                        new StatefulUpdateFunction(outputRate, stateSizeBytes, initCountRandom, initCountSeed),
                        OutputMode.Update(),
                        // IMPORTANT: use Java serialization for state encoder
                        Encoders.javaSerialization(State.class),
                        // IMPORTANT: use Java serialization for the ConsumerEvent in the output tuple
                        Encoders.tuple(Encoders.STRING(), Encoders.javaSerialization(ConsumerEvent.class)),
                        GroupStateTimeout.NoTimeout()
                )
                .map(
                        (MapFunction<Tuple2<String, ConsumerEvent>, Tuple2<String, byte[]>>)
                                stateWithKey -> new Tuple2<>(stateWithKey._1, stateWithKey._2.getData()),
                        Encoders.tuple(Encoders.STRING(), Encoders.BINARY())
                );

        DataStreamWriter<Row> writer = statefulAggregation
                .toDF("key", "value")
                .writeStream()
                .outputMode(OutputMode.Update())
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("topic", kafkaOutputTopic)
                .option("checkpointLocation", "/tmp/spark/checkpoint");

        Trigger trigger = createTrigger(sparkTriggerMode, sparkTriggerInterval);
        if (trigger != null) {
            writer = writer.trigger(trigger);
        }
        StreamingQuery query = writer.start();

        System.out.println("App build marker: " + System.getenv("IMAGE_BUILD_ID"));

        query.awaitTermination();
    }

    /**
     * Serializable FlatMap function for matching records.
     * Uses transient field for MatcherService to avoid serialization issues.
     */
    private static class MatcherFlatMapFunction
            implements FlatMapFunction<TimestampedRecord, Tuple2<String, TimestampedRecord>>, Serializable {

        private static final long serialVersionUID = 1L;

        private final Map<Double, Integer> selectivities;
        private final double totalSelectivity;
        private final int numRules;
        private final double s;
        private final long seed = 0x2e3fac4f58fc98b4L;

        // Transient - will be initialized on each executor
        private transient MatcherService<TimestampedRecord> matcherService;

        public MatcherFlatMapFunction(Map<Double, Integer> selectivities,
                                      double totalSelectivity,
                                      int numRules,
                                      double s) {
            this.selectivities = selectivities;
            this.totalSelectivity = totalSelectivity;
            this.numRules = numRules;
            this.s = s;
        }

        /**
         * Lazy initialization of MatcherService on executor.
         * This avoids serializing the service itself.
         */
        private MatcherService<TimestampedRecord> getMatcherService() {
            if (matcherService == null) {
                if (selectivities != null) {
                    matcherService = SimpleMatcherService.createFromFrequencyMap(
                            selectivities,
                            seed
                    );
                } else {
                    matcherService = SimpleMatcherService.createFromZipf(
                            numRules,
                            totalSelectivity,
                            s,
                            seed
                    );
                }
            }
            return matcherService;
        }

        @Override
        public Iterator<Tuple2<String, TimestampedRecord>> call(TimestampedRecord record) throws Exception {
            List<Tuple2<String, TimestampedRecord>> tuples = new ArrayList<>();
            Collection<Map.Entry<String, TimestampedRecord>> entries = getMatcherService().match(record);

            for (Map.Entry<String, TimestampedRecord> entry : entries) {
                tuples.add(new Tuple2<>(entry.getKey(), entry.getValue()));
            }

            return tuples.iterator();
        }
    }

    /**
     * Serializable FlatMapGroupsWithState function for stateful processing.
     * Uses transient field for StatefulConsumer to avoid serialization issues.
     */
    private static class StatefulUpdateFunction
            implements FlatMapGroupsWithStateFunction<String,
            Tuple2<String, TimestampedRecord>,
            State,
            Tuple2<String, ConsumerEvent>>,
            Serializable {

        private static final long serialVersionUID = 1L;

        private final int outputRate;
        private final int stateSizeBytes;
        private final boolean initCountRandom;
        private final long initCountSeed;

        // Transient - will be initialized on each executor
        private transient StatefulConsumer consumer;

        public StatefulUpdateFunction(int outputRate,
                                      int stateSizeBytes,
                                      boolean initCountRandom,
                                      long initCountSeed) {
            this.outputRate = outputRate;
            this.stateSizeBytes = stateSizeBytes;
            this.initCountRandom = initCountRandom;
            this.initCountSeed = initCountSeed;
        }

        /**
         * Lazy initialization of StatefulConsumer on executor.
         * This avoids serializing the consumer itself.
         */
        private StatefulConsumer getConsumer() {
            if (consumer == null) {
                consumer = new AdvancedStateConsumer(
                        "counter",
                        outputRate,
                        stateSizeBytes,
                        initCountRandom,
                        initCountSeed
                );
            }
            return consumer;
        }

        @Override
        public Iterator<Tuple2<String, ConsumerEvent>> call(
                String key,
                Iterator<Tuple2<String, TimestampedRecord>> values,
                GroupState<State> state) throws Exception {

            // Get or create state
            final State consumerState = state.exists() ? state.get() : new State();
            final List<Tuple2<String, ConsumerEvent>> forwardEvents = new ArrayList<>();

            // Process all values for this key
            while (values.hasNext()) {
                final Tuple2<String, TimestampedRecord> recordWithKey = values.next();
                final ConsumerResult consumerResult = getConsumer().accept(recordWithKey._2, consumerState);

                // Update state after each record
                state.update(consumerResult.getState());

                // Add event if present
                consumerResult.getEvent().ifPresent(event ->
                        forwardEvents.add(new Tuple2<>(key, event))
                );
            }

            return forwardEvents.iterator();
        }
    }

    private static Config loadMicroProfileConfig() {
        boolean useMpConfig = Boolean.parseBoolean(System.getenv("USE_MICROPROFILE_CONFIG"));
        if (!useMpConfig) {
            System.out.println("USE_MICROPROFILE_CONFIG not set; using environment variables.");
            return null;
        }
        try {
            return ConfigProviderResolver.instance().getConfig();
        } catch (Throwable t) {
            System.out.println("MicroProfile Config not available, falling back to environment variables.");
            return null;
        }
    }

    private static SmallRyeConfig tryUnwrapSmallRye(Config config) {
        if (config == null) {
            return null;
        }
        try {
            return config.unwrap(SmallRyeConfig.class);
        } catch (Throwable t) {
            System.out.println("SmallRyeConfig not available; continuing without it.");
            return null;
        }
    }

    private static String getRequiredString(Config config, String key) {
        if (config != null) {
            return config.getValue(key, String.class);
        }
        return getRequiredEnv(key);
    }

    private static int getRequiredInt(Config config, String key) {
        if (config != null) {
            return config.getValue(key, Integer.class);
        }
        return Integer.parseInt(getRequiredEnv(key));
    }

    private static long getRequiredLong(Config config, String key) {
        if (config != null) {
            return config.getValue(key, Long.class);
        }
        return Long.parseLong(getRequiredEnv(key));
    }

    private static double getRequiredDouble(Config config, String key) {
        if (config != null) {
            return config.getValue(key, Double.class);
        }
        return Double.parseDouble(getRequiredEnv(key));
    }

    private static boolean getRequiredBoolean(Config config, String key) {
        if (config != null) {
            return config.getValue(key, Boolean.class);
        }
        return Boolean.parseBoolean(getRequiredEnv(key));
    }

    private static Optional<Integer> getOptionalInt(Config config, String key) {
        if (config != null) {
            return config.getOptionalValue(key, Integer.class);
        }
        String val = System.getenv(toEnvKey(key));
        return (val == null || val.isBlank()) ? Optional.empty() : Optional.of(Integer.parseInt(val));
    }

    private static Optional<String> getOptionalString(Config config, String key) {
        if (config != null) {
            return config.getOptionalValue(key, String.class);
        }
        String val = System.getenv(toEnvKey(key));
        return (val == null || val.isBlank()) ? Optional.empty() : Optional.of(val);
    }

    /**
     * Create a Spark streaming trigger based on the configured mode.
     * Supported modes: "default" (Spark micro-batch, no explicit trigger),
     * "processing", "continuous", "realtime".
     * Returns {@code null} for the "default" mode so the caller can skip
     * calling {@code .trigger(...)} on the writer.
     */
    private static Trigger createTrigger(String mode, String interval) {
        switch (mode.toLowerCase(Locale.ROOT)) {
            case "default":
            case "":
                System.out.println("Using Spark default (micro-batch) trigger");
                return null;
            case "processing":
                requireInterval(mode, interval);
                System.out.println("Using ProcessingTime trigger with interval: " + interval);
                return Trigger.ProcessingTime(interval);
            case "continuous":
                requireInterval(mode, interval);
                System.out.println("Using Continuous trigger with interval: " + interval);
                return Trigger.Continuous(interval);
            case "realtime":
                requireInterval(mode, interval);
                System.out.println("Using RealTime trigger with interval: " + interval);
                return Trigger.RealTime(interval);
            default:
                throw new IllegalArgumentException(
                        "Unknown spark.trigger.mode: '" + mode + "'. "
                                + "Expected one of: default, processing, continuous, realtime.");
        }
    }

    private static void requireInterval(String mode, String interval) {
        if (interval == null || interval.isBlank()) {
            throw new IllegalStateException(
                    "spark.trigger.mode='" + mode + "' requires spark.trigger.interval "
                            + "(or legacy spark.trigger.realtime) to be set.");
        }
    }

    private static String getRequiredEnv(String key) {
        String envKey = toEnvKey(key);
        String val = System.getenv(envKey);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Missing config: " + key + " (env " + envKey + ")");
        }
        return val;
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }
}
