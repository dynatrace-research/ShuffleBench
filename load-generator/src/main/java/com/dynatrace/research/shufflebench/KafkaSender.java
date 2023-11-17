package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.record.Record;
import com.dynatrace.research.shufflebench.record.RecordSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class KafkaSender implements Consumer<Record>, Closeable {

  private static final Logger LOGGER = LogManager.getLogger(KafkaSender.class);

  private final KafkaProducer<Void, Record> kafkaProducer;

  private final String topic;

  public KafkaSender(final String bootstrapServers, final String topic) {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    this.kafkaProducer = new KafkaProducer<>(properties, new NullKeySerializer(), new RecordSerializer());
    this.topic = topic;
  }

  @Override
  public void accept(Record record) {
    ProducerRecord<Void, Record> producerRecord = new ProducerRecord<>(this.topic, record);
    LOGGER.debug("Send record to Kafka: {}", producerRecord);
    Future<RecordMetadata> result = this.kafkaProducer.send(producerRecord);
  }

  @Override
  public void close() {
    this.kafkaProducer.close();
  }
}
