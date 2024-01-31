package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.consumer.*;
import com.dynatrace.research.shufflebench.matcher.MatcherService;
import com.dynatrace.research.shufflebench.matcher.SerializableMatcherService;
import com.dynatrace.research.shufflebench.matcher.SimpleMatcherService;
import com.dynatrace.research.shufflebench.record.*;
import io.smallrye.config.SmallRyeConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class FlinkShuffle {

  private static final Logger LOGGER = LoggerFactory.getLogger(FlinkShuffle.class);

  private static final String APPLICATION_ID = "shufflebench-flink";

  private final StreamExecutionEnvironment env;

  public FlinkShuffle() {
    final Config config = ConfigProvider.getConfig();

    final String kafkaBootstrapServers = config.getValue("kafka.bootstrap.servers", String.class);
    final String kafkaInputTopic = config.getValue("kafka.topic.input", String.class);
    final String kafkaOutputTopic = config.getValue("kafka.topic.output", String.class);

    final int parallelism = config.getValue("flink.parallelism", Integer.class);
    final boolean autoCommit = config.getValue("flink.kafka.enable.auto.commit", Boolean.class);
    final Optional<String> deliveryGuarantee = config.getOptionalValue("flink.delivery.guarantee", String.class);
    final boolean checkpointingEnabled = config.getValue("flink.checkpointing.enable", Boolean.class);
    final int checkpointingIntervalMs = config.getValue("flink.checkpointing.interval.ms", Integer.class);

    final SmallRyeConfig smallRyeConfig = config.unwrap(SmallRyeConfig.class);
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

    this.env = StreamExecutionEnvironment.getExecutionEnvironment();

    this.env.getConfig().registerTypeWithKryoSerializer(Record.class, new RecordKyroSerializer());
    this.env.getConfig().registerTypeWithKryoSerializer(TimestampedRecord.class, new  TimestampedRecordKyroSerializer());
    this.env.getConfig().registerTypeWithKryoSerializer(State.class, new StateKyroSerializer());
    //this.env.getConfig().addDefaultKryoSerializer(Record.class, new RecordKyroSerializer());
    this.env.getConfig().getRegisteredTypesWithKryoSerializers().forEach(
            (type, serializer) -> LOGGER.info("Registered Kryo serializer for type '{}'.", type)
    );
    //this.env.getConfig().disableGenericTypes();
    this.env.getConfig().disableAutoTypeRegistration();
    this.env.getConfig().enableForceKryo();
    this.env.getConfig().enableObjectReuse();
    this.env.setParallelism(parallelism);
    if (checkpointingEnabled) {
      // CheckpointingMode.EXACTLY_ONCE is the default
      this.env.enableCheckpointing(checkpointingIntervalMs);
    }

    KafkaSource<Record> source = KafkaSource.<Record>builder()
        .setBootstrapServers(kafkaBootstrapServers)
        .setTopics(kafkaInputTopic)
        .setGroupId(APPLICATION_ID)
        .setStartingOffsets(OffsetsInitializer.earliest())
        .setDeserializer(KafkaRecordDeserializationSchema.valueOnly(RecordSerde.RecordDeserializer.class))
        .setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(autoCommit))
        .build();

    KafkaSinkBuilder<Tuple2<String, ConsumerEvent>> sinkBuilder = KafkaSink.<Tuple2<String,ConsumerEvent>>builder()
        .setBootstrapServers(kafkaBootstrapServers)
        .setRecordSerializer(KafkaRecordSerializationSchema.<Tuple2<String,ConsumerEvent>>builder()
            .setTopic(kafkaOutputTopic)
            .setKafkaKeySerializer(Tuple2StringKeyKafkaSerializer.class)
            .setKafkaValueSerializer(Tuple2ConsumerEventValueKafkaSerializer.class)
            .build());
    KafkaSink<Tuple2<String,ConsumerEvent>> sink;
    if (deliveryGuarantee.isPresent()) {
      sink = sinkBuilder
          .setDeliveryGuarantee(DeliveryGuarantee.valueOf(deliveryGuarantee.get()))
          .setTransactionalIdPrefix(APPLICATION_ID)
          .build();
    } else {
      sink = sinkBuilder.build();
    }

    env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
        .process(new TimestampAssignerFunction())
        .flatMap(
            (record, out) -> matcherService
                .match(record)
                .forEach(entry -> out.collect(Tuple2.of(entry.getKey(), entry.getValue()))),
            TypeInformation.of(new TypeHint<Tuple2<String, TimestampedRecord>>() {
            })
        )
        .keyBy(tuple -> tuple.f0)
        .process(new AggregateFunction(consumer))
        .sinkTo(sink);
  }

  public void start() {
    try {
      this.env.execute();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void stop() {
    try {
      this.env.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    FlinkShuffle flinkShuffle = new FlinkShuffle();
    flinkShuffle.start();
  }
}
