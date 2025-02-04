package com.dynatrace.research.shufflebench;

import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.research.shufflebench.record.RandomRecordGenerator;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class KafkaLoadGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaLoadGenerator.class);

  private final int executionTimeMs;

  private final String seedString;

  private final String kafkaBootstrapServers;

  private final String kafkaTopic;

  private final int numSources;
  private final int recordsPerSecondAndSource;

  private final int recordSizeInBytes;

  private final ScheduledExecutorService executor;

  private final List<KafkaSender> openKafkaSenders = new ArrayList<>();
  private final List<RecordSource> openRecordSources = new ArrayList<>();

  public KafkaLoadGenerator(
          int executionTimeMs,
          String seedString,
          String kafkaBootstrapServers,
          String kafkaTopic,
          int numSources,
          int recordsPerSecondAndSource,
          int recordSizeInBytes,
          int threadPoolSize
  ) {
    this.executionTimeMs = executionTimeMs;
    this.seedString = seedString;
    this.kafkaBootstrapServers = kafkaBootstrapServers;
    this.kafkaTopic = kafkaTopic;
    this.numSources = numSources;
    this.recordsPerSecondAndSource = recordsPerSecondAndSource;
    this.recordSizeInBytes = recordSizeInBytes;
    this.executor = new ScheduledThreadPoolExecutor(threadPoolSize);
  }

  public void startBlocking() throws InterruptedException {
    final KafkaSender kafkaSender = new KafkaSender(this.kafkaBootstrapServers, this.kafkaTopic);
    for (int sourceId = 0; sourceId < numSources; sourceId++) {
      final long seed = Hashing.komihash4_3().hashStream().putString(seedString).putInt(sourceId).getAsLong();
      final RecordSource recordSource = new RecordSource(
          executor,
          this.recordsPerSecondAndSource,
          kafkaSender,
          // new StaticRecordGenerator(),
          new RandomRecordGenerator(seed, recordSizeInBytes),
          "source" + sourceId);
      openRecordSources.add(recordSource);
    }
    openKafkaSenders.add(kafkaSender);
    Thread.sleep(this.executionTimeMs);
  }

  public void stop() throws InterruptedException, IOException {
    for (RecordSource recordSource : openRecordSources) {
      recordSource.stop();
    }
    openRecordSources.clear();
    for (KafkaSender kafkaSender : openKafkaSenders) {
      kafkaSender.close();
    }
    openKafkaSenders.clear();
    executor.shutdown();
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    final Config config = ConfigProvider.getConfig();
    final KafkaLoadGenerator kafkaLoadGenerator = new KafkaLoadGenerator(
            config.getValue("execution.time.ms", Integer.class),
            config.getValue("seed.string", String.class),
            config.getValue("kafka.bootstrap.servers", String.class),
            config.getValue("kafka.topic", String.class),
            config.getValue("num.sources", Integer.class),
            config.getValue("num.records.per.source.second", Integer.class),
            config.getValue("record.size.bytes", Integer.class),
            config.getValue("thread.pool.size", Integer.class)
    );
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOGGER.info("Shut down load generator.");
      try {
        kafkaLoadGenerator.stop();
      } catch (InterruptedException | IOException e) {
        throw new RuntimeException(e);
      }
    }));
    kafkaLoadGenerator.startBlocking();
    kafkaLoadGenerator.stop();
  }
}
