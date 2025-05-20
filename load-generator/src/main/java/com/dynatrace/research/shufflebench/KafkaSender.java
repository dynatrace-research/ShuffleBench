package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.record.Record;
import com.dynatrace.research.shufflebench.record.RecordSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class KafkaSender implements Consumer<Record>, Closeable {

  private static final Logger LOGGER = LogManager.getLogger(KafkaSender.class);

  private final KafkaProducer<Void, Record> kafkaProducer;

  private final String topic;

  private final Consumer<Record> callback; // May be null

  public KafkaSender(final String bootstrapServers, Map<String, String> kafkaProducerConfig, final String topic) {
    this(bootstrapServers, topic, kafkaProducerConfig, null);
  }

  public KafkaSender(final String bootstrapServers, final String topic, Map<String, String> kafkaProducerConfig, final Consumer<Record> callback) {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.putAll(kafkaProducerConfig);
    this.kafkaProducer = new KafkaProducer<>(properties, new NullKeySerializer(), new RecordSerializer());
    this.topic = topic;
    this.callback = callback;
  }

  @Override
  public void accept(Record record) {
    ProducerRecord<Void, Record> producerRecord = new ProducerRecord<>(this.topic, record);
    LOGGER.debug("Send record to Kafka: {}", producerRecord);
    Future<RecordMetadata> result = this.produceKafkaRecord(producerRecord);
  }

  private Future<RecordMetadata> produceKafkaRecord(ProducerRecord<Void, Record> producerRecord) {
    if (callback == null) {
      return this.kafkaProducer.send(producerRecord);
    } else {
      return this.kafkaProducer.send(producerRecord, (metadata, exception) -> callback.accept(producerRecord.value()));
    }
  }

  @Override
  public void close() {
    this.kafkaProducer.close();
  }
}
