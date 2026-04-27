package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.consumer.ConsumerEvent;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.Nullable;

public class ConsumerEventKafkaRecordSerializationSchema
        implements KafkaRecordSerializationSchema<Tuple2<String, ConsumerEvent>> {

    private final String topic;
    private transient StringSerializer keySerializer;
    private transient ConsumerEventSerde.ConsumerEventSerializer valueSerializer;

    public ConsumerEventKafkaRecordSerializationSchema(String topic) {
        this.topic = topic;
    }

    private void ensureInitialized() {
        if (keySerializer == null) {
            keySerializer = new StringSerializer();
        }
        if (valueSerializer == null) {
            valueSerializer = new ConsumerEventSerde.ConsumerEventSerializer();
        }
    }

    @Nullable
    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            Tuple2<String, ConsumerEvent> element,
            KafkaSinkContext context,
            Long timestamp) {
        ensureInitialized();
        byte[] key = keySerializer.serialize(topic, element.f0);
        byte[] value = valueSerializer.serialize(topic, element.f1);
        return new ProducerRecord<>(topic, key, value);
    }
}

