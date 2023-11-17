package com.dynatrace.research.shufflebench.record;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class Tuple2KeyKafkaSerializer<T> implements Serializer<Tuple2<T,?>> {

    private final Serializer<T> keySerializer;

    public Tuple2KeyKafkaSerializer(Serializer<T> keySerializer) {
        this.keySerializer = keySerializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        keySerializer.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String topic, Tuple2<T, ?> tuple) {
        return keySerializer.serialize(topic, tuple.f0);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Tuple2<T, ?> tuple) {
        return keySerializer.serialize(topic, headers, tuple.f0);
    }

    @Override
    public void close() {
        keySerializer.close();
    }
}
