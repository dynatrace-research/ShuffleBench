package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.consumer.AdvancedStateConsumer;
import com.dynatrace.research.shufflebench.consumer.SerializableStatefulConsumer;
import com.dynatrace.research.shufflebench.consumer.State;
import com.dynatrace.research.shufflebench.consumer.StatefulConsumer;
import com.dynatrace.research.shufflebench.matcher.MatcherService;
import com.dynatrace.research.shufflebench.matcher.SimpleMatcherService;
import com.dynatrace.research.shufflebench.record.*;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.config.EdgeConfig;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.kafka.KafkaSinks;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.spi.properties.ClusterProperty;
import io.smallrye.config.SmallRyeConfig;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hazelcast.jet.pipeline.ServiceFactories.nonSharedService;

public class HazelcastShuffle {

  private static final String APPLICATION_ID = "shufflebench-hzcast";

  private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastShuffle.class);

  private final Pipeline pipeline;

  private HazelcastInstance hazelcast;

  public HazelcastShuffle() {
    final SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
    final Properties kafkaProps = new Properties();
    kafkaProps.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        config.getValue("kafka.bootstrap.servers", String.class));

    final Properties kafkaConsumerProps = new Properties();
    kafkaConsumerProps.putAll(kafkaProps);
    kafkaConsumerProps.put(
        ConsumerConfig.GROUP_ID_CONFIG,
        APPLICATION_ID);
    // props.put(
    //         ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
    //        true);
    kafkaConsumerProps.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        ByteArrayDeserializer.class);
    kafkaConsumerProps.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        RecordSerde.RecordDeserializer.class);
    kafkaConsumerProps.putAll(
            config.getOptionalValues("kafka.consumer", String.class, String.class).orElse(Map.of()));

    final Properties kafkaProducerProps = new Properties();
    kafkaProducerProps.putAll(kafkaProps);
    kafkaProducerProps.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class);
    kafkaProducerProps.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        ConsumerEventSerde.ConsumerEventSerializer.class);
    kafkaProducerProps.putAll(
            config.getOptionalValues("kafka.producer", String.class, String.class).orElse(Map.of()));

    final String kafkaInputTopic = config.getValue("kafka.topic.input", String.class);
    final String kafkaOutputTopic = config.getValue("kafka.topic.output", String.class);

    final SmallRyeConfig smallRyeConfig = config.unwrap(SmallRyeConfig.class);
    final Map<Double, Integer> selectivities =
        smallRyeConfig.getOptionalValues("matcher.selectivities", Double.class, Integer.class).orElse(null);
    final double totalSelectivity = config.getValue("matcher.zipf.total.selectivity", Double.class);
    final int numRules = config.getValue("matcher.zipf.num.rules", Integer.class);
    final double s = config.getValue("matcher.zipf.s", Double.class);
    final int outputRate = config.getValue("consumer.output.rate", Integer.class);
    final int stateSizeBytes = config.getValue("consumer.state.size.bytes", Integer.class);
    final boolean initCountRandom = config.getValue("consumer.init.count.random", Boolean.class);
    final long initCountSeed = config.getValue("consumer.init.count.seed", Long.class);

    ServiceFactory<?, MatcherService<TimestampedRecord>> matcherServiceFactory = nonSharedService(
        pctx -> {
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
    
    final StatefulConsumer consumer = new SerializableStatefulConsumer(
            () -> new AdvancedStateConsumer("counter", outputRate, stateSizeBytes, initCountRandom, initCountSeed));

    this.pipeline = Pipeline.create();
    pipeline.readFrom(KafkaSources.<Void, Record, TimestampedRecord>kafka(
            kafkaConsumerProps,
            kafkaRecord -> new TimestampedRecord(kafkaRecord.timestamp(), kafkaRecord.value().getData()),
            kafkaInputTopic))
        .withoutTimestamps()
        .flatMapUsingService(
            matcherServiceFactory,
            (matcher, record) -> Traversers.traverseStream(
                    matcher.match(record)
                            .stream()
                            // The returned Map.Entry objects are not serializable
                            .map(e -> Util.entry(e.getKey(), e.getValue()))
            )
        )
        .groupingKey(record -> record.getKey())
        .flatMapStateful(
            State::new,
            (state, key, recordWithKey) -> Traversers.traverseStream(
                      consumer.accept(recordWithKey.getValue(), state) // Implicitly updates state
                              .getEvent()
                              .stream()
                              .map(event -> Util.entry(key, event))
              )
        )
        .writeTo(KafkaSinks.kafka(kafkaProducerProps, kafkaOutputTopic));
  }

  public void start() {
    final Config appConfig = ConfigProvider.getConfig();
    com.hazelcast.config.Config config = buildHazelcastConfig(appConfig);
    this.hazelcast = Hazelcast.newHazelcastInstance(config);
    final JobConfig jobConfig = new JobConfig();
    jobConfig.setName(APPLICATION_ID);
    jobConfig.registerSerializer(Record.class, RecordSerializer.class);
    jobConfig.registerSerializer(TimestampedRecord.class, TimestampedRecordSerializer.class);
    jobConfig.registerSerializer(State.class, StateSerializer.class);
    //jobConfig.registerSerializer(ConsumerEvent.class, ConsumerEventSerde.ConsumerEventSerializer.class);
    final Optional<String> processingGuaranteeName = appConfig.getOptionalValue("hazelcast.jet.job.processingGuarantee", String.class);
    if (processingGuaranteeName.isPresent()) {
      final ProcessingGuarantee processingGuarantee = ProcessingGuarantee.valueOf(processingGuaranteeName.get());
      jobConfig.setProcessingGuarantee(processingGuarantee);
      LOGGER.info("Set processing guarantee to {}.", processingGuarantee);
    }
    final Optional<Integer> snapshotIntervalMillis = appConfig.getOptionalValue("hazelcast.jet.job.snapshotIntervalMillis", Integer.class);
    if (snapshotIntervalMillis.isPresent()) {
      jobConfig.setSnapshotIntervalMillis(snapshotIntervalMillis.get());
      LOGGER.info("Set snapshot interval to {} ms.", snapshotIntervalMillis.get());
    }
    Job job = this.hazelcast.getJet().newJobIfAbsent(this.pipeline, jobConfig);
    job.join();
  }

  public void stop() {
    this.hazelcast.shutdown();
  }

  private static com.hazelcast.config.Config buildHazelcastConfig(Config appConfig) {
    com.hazelcast.config.Config config = new com.hazelcast.config.Config();
    config.setProperty(ClusterProperty.LOGGING_TYPE.getName(), "slf4j");
    config.setClusterName(APPLICATION_ID);
    JoinConfig joinConfig = config.getNetworkConfig()
        .setPort(appConfig.getValue("hazelcast.port", Integer.class))
        .setPortAutoIncrement(appConfig.getValue("hazelcast.port.auto.increment", Boolean.class))
        .getJoin();
    joinConfig.getMulticastConfig().setEnabled(false);
    //joinConfig.getTcpIpConfig().addMember(appConfig.getValue("hazelcast.bootstrap.server", String.class));
    Optional<String> kubernetesDnsName = appConfig.getOptionalValue("hazelcast.kubernetes.dns.name", String.class);
    Optional<String> bootstrapServer = appConfig.getOptionalValue("hazelcast.bootstrap.server", String.class);
    if (kubernetesDnsName.isPresent()) {
      joinConfig.getKubernetesConfig()
          .setEnabled(true)
          .setProperty("service-dns", kubernetesDnsName.get());
      LOGGER.info("Use Kubernetes DNS name '{}'.", kubernetesDnsName.get());
    } else if (bootstrapServer.isPresent()) {
      joinConfig.getTcpIpConfig().addMember(bootstrapServer.get());
      LOGGER.info("Use bootstrap server '{}'.", bootstrapServer.get());
    }
    config.getJetConfig().setEnabled(true);
    Optional<Integer> backupCount = appConfig.getOptionalValue("hazelcast.jet.instance.backup-count", Integer.class);
    if (backupCount.isPresent()) {
      config.getJetConfig().setBackupCount(backupCount.get());
      LOGGER.info("Set backup count to '{}'.", backupCount.get());
    }
    EdgeConfig edgeConfig = new EdgeConfig();
    Optional<Integer> queueSize = appConfig.getOptionalValue("hazelcast.jet.edge-defaults.queue-size", Integer.class);
    if (queueSize.isPresent()) {
      edgeConfig.setQueueSize(queueSize.get());
      LOGGER.info("Set default edge queue size to '{}'.", queueSize.get());
    }
    Optional<Integer> packetSizeLimit = appConfig.getOptionalValue("hazelcast.jet.edge-defaults.packet-size-limit", Integer.class);
    if (packetSizeLimit.isPresent()) {
      edgeConfig.setPacketSizeLimit(packetSizeLimit.get());
      LOGGER.info("Set default edge queue size to '{}'.", packetSizeLimit.get());
    }
    Optional<Integer> receiveWindowMultiplier = appConfig.getOptionalValue("hazelcast.jet.edge-defaults.receive-window-multiplier", Integer.class);
    if (receiveWindowMultiplier.isPresent()) {
      edgeConfig.setQueueSize(receiveWindowMultiplier.get());
      LOGGER.info("Set default receive window multiplier to '{}'.", receiveWindowMultiplier.get());
    }
    config.getJetConfig().setDefaultEdgeConfig(edgeConfig);
    return config;
  }

  public static void main(String[] args) {
    HazelcastShuffle hazelcastShuffle = new HazelcastShuffle();
    hazelcastShuffle.start();
  }
}
