package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.consumer.AdvancedStateConsumer;
import com.dynatrace.research.shufflebench.consumer.StatefulConsumer;
import com.dynatrace.research.shufflebench.matcher.MatcherService;
import com.dynatrace.research.shufflebench.matcher.SimpleMatcherService;
import com.dynatrace.research.shufflebench.record.*;
import io.smallrye.config.SmallRyeConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class KafkaStreamsShuffle {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStreamsShuffle.class);

  private static final String APPLICATION_ID = "shufflebench-kstreams";

  private final KafkaStreams kafkaStreamsApp;

  public KafkaStreamsShuffle() {
    final SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);

    Properties props = new Properties();
    props.put(
        StreamsConfig.APPLICATION_ID_CONFIG,
        APPLICATION_ID);
    props.put(
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
        config.getValue("kafka.bootstrap.servers", String.class));
    props.putAll(
        config.getOptionalValues("kafkastreams", String.class, String.class).orElse(Map.of()));

    final String kafkaInputTopic = config.getValue("kafka.topic.input", String.class);
    final String kafkaOutputTopic = config.getValue("kafka.topic.output", String.class);

    final Optional<Map<Double, Integer>> selectivities =
            config.getOptionalValues("matcher.selectivities", Double.class, Integer.class);
    final MatcherService<TimestampedRecord> matcherService;
    if (selectivities.isPresent()) {
      matcherService = SimpleMatcherService.createFromFrequencyMap(selectivities.get(), 0x2e3fac4f58fc98b4L);
    } else {
      final double totalSelectivity = config.getValue("matcher.zipf.total.selectivity", Double.class);
      final int numRules = config.getValue("matcher.zipf.num.rules", Integer.class);
      final double s = config.getValue("matcher.zipf.s", Double.class);
      matcherService = SimpleMatcherService.createFromZipf(
          numRules,
          totalSelectivity,
          s,
          0x2e3fac4f58fc98b4L);
    }
    final int outputRate = config.getValue("consumer.output.rate", Integer.class);
    final int stateSizeBytes = config.getValue("consumer.state.size.bytes", Integer.class);
    final boolean initCountRandom = config.getValue("consumer.init.count.random", Boolean.class);
    final long initCountSeed = config.getValue("consumer.init.count.seed", Long.class);
    final StatefulConsumerProcessor.StoreType storeType
            = StatefulConsumerProcessor.StoreType.fromName(config.getValue("kafkastreams.store.type", String.class));

    final StatefulConsumer consumer = new AdvancedStateConsumer("counter", outputRate, stateSizeBytes, initCountRandom, initCountSeed);

    final StreamsBuilder builder = new StreamsBuilder();
    builder.stream(kafkaInputTopic, Consumed.with(Serdes.ByteArray(), new RecordSerde()))
        .processValues(TimestampAssignerProcessor<byte[]>::new)
        .flatMap((k, v) -> createIterable(matcherService
                  .match(v)
                  .stream()
                  .map(e -> KeyValue.pair(e.getKey(), e.getValue()))
                  .iterator()))
        .repartition(Repartitioned.with(Serdes.String(), new TimestampedRecordSerde()))
        .processValues(StatefulConsumerProcessor.supplier(consumer, storeType))
        .to(kafkaOutputTopic, Produced.with(Serdes.String(), new ConsumerEventSerde()));
    final Topology topology = builder.build();
    this.kafkaStreamsApp = new KafkaStreams(topology, props);
  }

  public void start() {
    this.kafkaStreamsApp.start();
  }

  public void stop() {
    this.kafkaStreamsApp.close();
  }

  public static void main(String[] args) throws IOException {
    KafkaStreamsShuffle kafkaStreamsShuffle = new KafkaStreamsShuffle();
    kafkaStreamsShuffle.start();
  }

  private static <T> Iterable<T> createIterable(final Iterator<T> iterator) {
    return () -> iterator;
  }
}
