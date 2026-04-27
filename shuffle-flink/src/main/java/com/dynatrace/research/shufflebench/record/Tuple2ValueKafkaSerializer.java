package com.dynatrace.research.shufflebench.record;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Delegating Kafka serializer for the value component of a Flink {@link Tuple2}.
 *
 * @param <T> value type
 */
public class Tuple2ValueKafkaSerializer<T> implements Serializer<Tuple2<?, T>> {

    private final Serializer<T> valueSerializer;

    /**
     * Creates a tuple-value serializer.
     *
     * @param valueSerializer serializer used for the second tuple field
     */
    public Tuple2ValueKafkaSerializer(Serializer<T> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        valueSerializer.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String topic, Tuple2<?, T> tuple) {
        return valueSerializer.serialize(topic, tuple.f1);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Tuple2<?, T> tuple) {
        return valueSerializer.serialize(topic, headers, tuple.f1);
    }

    @Override
    public void close() {
        valueSerializer.close();
    }
}
