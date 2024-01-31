package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.consumer.*;
import com.dynatrace.research.shufflebench.matcher.MatcherService;
import com.dynatrace.research.shufflebench.matcher.SerializableMatcherService;
import com.dynatrace.research.shufflebench.matcher.SimpleMatcherService;
import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import io.smallrye.config.SmallRyeConfig;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.FlatMapGroupsWithStateFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.internal.config.Kryo;
import org.apache.spark.sql.streaming.*;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import scala.Tuple2;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeoutException;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
public class SparkStructuredStreamingShuffle {

    private static final String APPLICATION_ID = "shufflebench-sparkStructuredStreaming";

    public static void main(String[] args) throws StreamingQueryException, TimeoutException {

        SparkSession spark = SparkSession.builder()
                .appName(APPLICATION_ID)
                .config(Kryo.KRYO_REGISTRATION_REQUIRED().key(), true)
                .config(Kryo.KRYO_USER_REGISTRATORS().key(), CustomKryoRegistrator.class.getName())
                .config("spark.sql.streaming.stateStore.providerClass", "org.apache.spark.sql.execution.streaming.state.RocksDBStateStoreProvider")
                .getOrCreate();

        final Config config = ConfigProvider.getConfig();
        final SmallRyeConfig smallRyeConfig = config.unwrap(SmallRyeConfig.class);

        final String kafkaBootstrapServers = config.getValue("kafka.bootstrap.servers", String.class);
        final String kafkaInputTopic = config.getValue("kafka.topic.input", String.class);
        final String kafkaOutputTopic = config.getValue("kafka.topic.output", String.class);

        Optional<Integer> maxOffsetsPerTrigger = config.getOptionalValue("spark.max.offsets.per.trigger", Integer.class);
        DataStreamReader kafkaReader = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("subscribe", kafkaInputTopic)
                .option("startingOffsets", "latest");
        if (maxOffsetsPerTrigger.isPresent()) {
            kafkaReader = kafkaReader.option("maxOffsetsPerTrigger", maxOffsetsPerTrigger.get());
        }
        final Dataset<Row> kafkaStream = kafkaReader
                .load()
                .select("value", "timestamp");

        final Map<Double, Integer> selectivities =
                smallRyeConfig.getOptionalValues("matcher.selectivities", Double.class, Integer.class).orElse(null);
        final double totalSelectivity = config.getValue("matcher.zipf.total.selectivity", Double.class);
        final int numRules = config.getValue("matcher.zipf.num.rules", Integer.class);
        final double s = config.getValue("matcher.zipf.s", Double.class);
        final MatcherService<TimestampedRecord> matcherService = new SerializableMatcherService<>(
                () -> {
                    if (selectivities != null) {
                        return SimpleMatcherService.createFromFrequencyMap(selectivities, 0x2e3fac4f58fc98b4L);
                    } else {
                        return SimpleMatcherService.createFromZipf(
                                numRules,
                                totalSelectivity,
                                s,
                                0x2e3fac4f58fc98b4L);
                    }
                });
        final int outputRate = config.getValue("consumer.output.rate", Integer.class);
        final int stateSizeBytes = config.getValue("consumer.state.size.bytes", Integer.class);
        final boolean initCountRandom = config.getValue("consumer.init.count.random", Boolean.class);
        final long initCountSeed = config.getValue("consumer.init.count.seed", Long.class);

        final StatefulConsumer consumer = new SerializableStatefulConsumer(
                () -> new AdvancedStateConsumer("counter", outputRate, stateSizeBytes, initCountRandom, initCountSeed));

        Dataset<Tuple2<String, TimestampedRecord>> streamingWithKeys = kafkaStream
                .as(Encoders.tuple(Encoders.BINARY(), Encoders.TIMESTAMP()))
                .map(
                        (MapFunction<Tuple2<byte[], Timestamp>, TimestampedRecord>) timestampedArray -> new TimestampedRecord(timestampedArray._2.getTime(), timestampedArray._1),
                        Encoders.kryo(TimestampedRecord.class)
                ).flatMap((FlatMapFunction<TimestampedRecord, Tuple2<String, TimestampedRecord>>)
                            x -> {
                                // return matcherService.match(x)
                                //        .stream()
                                //        .map(entry -> new Tuple2<>(entry.getKey(), entry.getValue()))
                                //        .iterator();
                                List<Tuple2<String, TimestampedRecord>> tuples = new ArrayList<>();
                                Collection<Map.Entry<String, TimestampedRecord>> entries = matcherService.match(x);
                                for (Map.Entry<String, TimestampedRecord> entry : entries) {
                                    tuples.add(new Tuple2<>(entry.getKey(), entry.getValue()));
                                }
                                return tuples.iterator();
                            },
                    Encoders.tuple(Encoders.STRING(), Encoders.kryo(TimestampedRecord.class))
                );

        FlatMapGroupsWithStateFunction<String, Tuple2<String, TimestampedRecord>, State, Tuple2<String, ConsumerEvent>> updateState = (key, values, state) -> {
            // Maybe the exists() check is unnecessary here, as the consumer can
            final State consumerState = state.exists() ? state.get() : new State();
            final List<Tuple2<String, ConsumerEvent>> forwardEvents = new ArrayList<>();
            while (values.hasNext()) {
                final Tuple2<String, TimestampedRecord> recordWithKey = values.next();
                final ConsumerResult consumerResult = consumer.accept(recordWithKey._2, consumerState);
                // Might be possible to move the update out of the loop,
                // but don't know about impact on processing guarantees/fault tolerance
                state.update(consumerResult.getState());
                consumerResult.getEvent().ifPresent(event -> forwardEvents.add(new Tuple2<>(key, event)));
            }
            return forwardEvents.iterator();
        };

        Dataset<Tuple2<String, byte[]>> statefulAggregation = streamingWithKeys
                //.as(Encoders.tuple(Encoders.STRING(), Encoders.kryo(TimestampedRecord.class)))
                .groupByKey((MapFunction<Tuple2<String, TimestampedRecord>, String>) value -> value._1, Encoders.STRING())
                .flatMapGroupsWithState(
                        updateState,
                        OutputMode.Update(),
                        Encoders.kryo(State.class),
                        Encoders.tuple(Encoders.STRING(), Encoders.kryo(ConsumerEvent.class)),
                        GroupStateTimeout.NoTimeout())
                .map(
                        (MapFunction<Tuple2<String, ConsumerEvent>, Tuple2<String, byte[]>>) stateWithKey -> new Tuple2<>(stateWithKey._1, stateWithKey._2.getData()),
                        Encoders.tuple(Encoders.STRING(), Encoders.BINARY()));

        StreamingQuery query =  statefulAggregation
                .toDF("key", "value")
                .writeStream()
                .outputMode(OutputMode.Update())
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaBootstrapServers)
                .option("topic", kafkaOutputTopic)
                .option("checkpointLocation", "/tmp/spark/checkpoint") //saves the checkpoints on /tmp/spark
                .start();
        query.awaitTermination();
    }
}