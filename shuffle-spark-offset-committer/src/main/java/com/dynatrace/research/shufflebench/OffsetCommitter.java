package com.dynatrace.research.shufflebench;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class OffsetCommitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OffsetCommitter.class);

    private static final Path OFFSETS_PATHS = Paths.get("checkpoint", "offsets");

    private static final String OFFSET_FILENAME_REGEX = "\\d+";

    private final String kafkaBootstrapServers;

    private final String kafkaConsumerGroupId;

    private final Path checkpointBase;

    private final long commitIntervalMs;

    private final ScheduledExecutorService executorService;

    private final ObjectMapper mapper = new ObjectMapper();

    public OffsetCommitter() {
        final Config config = ConfigProvider.getConfig();
        this.kafkaBootstrapServers = config.getValue("kafka.bootstrap.servers", String.class);
        this.kafkaConsumerGroupId = config.getValue("kafka.consumer.group.id", String.class);
        this.checkpointBase = Paths.get(config.getValue("spark.checkpoint.base.path", String.class));
        this.commitIntervalMs = config.getValue("commit.interval.ms", Long.class);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startBlocking() {
        final Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaBootstrapServers);
        try (AdminClient adminClient = AdminClient.create(properties)) {
            executorService.scheduleAtFixedRate(
                    () -> commitOffsets(adminClient),
                    this.commitIntervalMs,
                    this.commitIntervalMs,
                    TimeUnit.MILLISECONDS);
            boolean result = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopBlocking() {
        this.executorService.shutdown();
        try {
            boolean result = this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void commitOffsets(AdminClient adminClient) {
        try {
            final Path latestOffsetsFile = this.getLatestOffsetsFile();
            final String offsetsJson = this.readOffsetsJson(latestOffsetsFile);
            final Map<TopicPartition, OffsetAndMetadata> offsets = this.parseOffsetsJson(offsetsJson);
            adminClient.alterConsumerGroupOffsets(kafkaConsumerGroupId, offsets)
                    .all()
                    .get();
            LOGGER.debug("Offsets committed for {} topic partitions.", offsets.size());
        } catch (NoOffsetFileException | IllegalOffsetFileException | InterruptedException | ExecutionException e) {
            LOGGER.error("Could not commit offsets.", e);
        }
    }

    private Path getLatestOffsetsFile() {
        final Path offsetDir = checkpointBase.resolve(OFFSETS_PATHS);
        if (!Files.exists(offsetDir)) {
            throw new NoOffsetFileException("Offset directory '" + offsetDir + "' does not exist");
        }
        try (Stream<Path> files = Files.list(offsetDir)) {
            return files
                    .filter(f -> f.getFileName().toString().matches(OFFSET_FILENAME_REGEX))
                    .filter(f -> !Files.isDirectory(f))
                    //.max(Comparator.comparingLong(f -> f.toFile().lastModified()))
                    .max(Comparator.comparingInt(f -> Integer.parseInt(f.getFileName().toString())))
                    .orElseThrow(() -> new NoOffsetFileException("No offset files found in '" + offsetDir + "'"));
        } catch (IOException e) {
            throw new NoOffsetFileException("Cannot read offset files", e);
        }
    }

    private String readOffsetsJson(Path offsetFile) {
        try (Stream<String> lines = Files.lines(offsetFile)) {
            return lines
                    .skip(2)
                    .findFirst()
                    .orElseThrow(() -> new IllegalOffsetFileException("Cannot read offsets from file '" + offsetFile + "'"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<TopicPartition, OffsetAndMetadata> parseOffsetsJson(String offsetJson) {
        final Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        final JsonNode rootNode;
        try {
            rootNode = mapper.readTree(offsetJson);
        } catch (JsonProcessingException e) {
            throw new IllegalOffsetFileException("Illegal Spark Kafka offset data", e);
        }
        if (!rootNode.isObject()) {
            throw new IllegalOffsetFileException("Illegal Spark Kafka offset data");
        }
        final Iterator<Map.Entry<String, JsonNode>> topics = rootNode.fields();
        while (topics.hasNext()) {
            final Map.Entry<String, JsonNode> topicWithOffsets = topics.next();
            final String topic = topicWithOffsets.getKey();
            if (!topicWithOffsets.getValue().isObject()) {
                throw new IllegalOffsetFileException("Illegal Spark Kafka offset data");
            }
            final Iterator<Map.Entry<String, JsonNode>> partitions = topicWithOffsets.getValue().fields();
            while (partitions.hasNext()) {
                final Map.Entry<String, JsonNode> partitionWithOffset = partitions.next();
                final int partition = Integer.parseInt(partitionWithOffset.getKey());
                if (!partitionWithOffset.getValue().isNumber()) {
                    throw new IllegalOffsetFileException("Illegal Spark Kafka offset data");
                }
                final long offsetValue = partitionWithOffset.getValue().asLong();
                offsets.put(
                        new TopicPartition(topic, partition),
                        new OffsetAndMetadata(offsetValue));
            }
        }
        return offsets;
    }


    public static void main(String[] args) {
        final OffsetCommitter offsetCommitter = new OffsetCommitter();
        Runtime.getRuntime().addShutdownHook(new Thread(offsetCommitter::stopBlocking));
        offsetCommitter.startBlocking();
    }

}
