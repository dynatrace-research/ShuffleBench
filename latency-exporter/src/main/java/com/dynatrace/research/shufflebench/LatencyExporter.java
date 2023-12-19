package com.dynatrace.research.shufflebench;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class LatencyExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatencyExporter.class);

    private final String kafkaBootstrapServers;

    private final String kafkaTopic;

    private final String kafkaIsolationLevel;

    private final String prometheusExporterPath;

    private final int prometheusExporterPort;

    private boolean keepRunning = true;

    public LatencyExporter() {
        final Config config = ConfigProvider.getConfig();
        prometheusExporterPath = config.getValue("prometheus.exporter.path", String.class);
        prometheusExporterPort = config.getValue("prometheus.exporter.port", Integer.class);
        kafkaBootstrapServers = config.getValue("kafka.bootstrap.servers", String.class);
        kafkaTopic = config.getValue("kafka.topic", String.class);
        kafkaIsolationLevel = config.getOptionalValue("kafka.isolation.level", String.class).orElse(null);
    }

    public void startBlocking() {
        final MeterRegistry registry = getRegistry();
        Timer latencyTimer = Timer.builder("shufflebench.latency")
                .description("time between trigger event and log append time") // optional
                //.tags("region", "test") // optional
                .publishPercentiles(0.5, 0.95, 0.99, 0.999) // median and 95th, 99th and 99.9th percentile
                .publishPercentileHistogram()
                //.serviceLevelObjectives(Duration.ofMillis(100))
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(registry);
        final Timer negativeLatencyTimer = Timer.builder("shufflebench.latency.negatives")
                .description("observed negative latencies") // optional
                //.tags("region", "test") // optional
                .publishPercentiles(0.5, 0.95, 0.99, 0.999) // median and 95th, 99th and 99.9th percentile
                .publishPercentileHistogram()
                //.serviceLevelObjectives(Duration.ofMillis(100))
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(registry);

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "shufflebench-latency-exporter");
        if (this.kafkaIsolationLevel != null) {
            props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, this.kafkaIsolationLevel);
        }
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props, new StringDeserializer(), new ByteArrayDeserializer())) {
            consumer.subscribe(List.of(this.kafkaTopic));
            while (keepRunning) {
                final ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, byte[]> record : records) {
                    final String topic = record.topic();
                    final int partition = record.partition();
                    final long logAppendTime = record.timestamp();
                    final long eventTime = ByteBuffer.wrap(record.value()).getLong(3 * Long.BYTES);
                    final long latency = logAppendTime - eventTime;
                    LOGGER.debug("Observed latency of {} ms", latency);
                    if (latency >= 0) {
                        latencyTimer.record(latency, TimeUnit.MILLISECONDS);
                    } else {
                        negativeLatencyTimer.record(-latency, TimeUnit.MILLISECONDS);
                        /*registry.timer(
                            "shufflebench.latency",
                            List.of(
                                    Tag.of("topic", topic),
                                    Tag.of("partition", Integer.toString(partition))))
                            .record(latency, TimeUnit.MILLISECONDS);*/
                    }
                }
            }
        }
    }

    public void stop() throws InterruptedException, IOException {
        this.keepRunning = false;
    }

    private MeterRegistry getRegistry() {
        final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(this.prometheusExporterPort), 0);
            server.createContext(this.prometheusExporterPath, httpExchange -> {
                String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prometheusRegistry;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        final LatencyExporter latencyExporter = new LatencyExporter();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shut down latency exporter.");
            try {
                latencyExporter.stop();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }));
        latencyExporter.startBlocking();
        latencyExporter.stop();


    }

}
