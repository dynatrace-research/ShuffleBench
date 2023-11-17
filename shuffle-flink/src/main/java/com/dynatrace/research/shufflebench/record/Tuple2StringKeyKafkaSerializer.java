package com.dynatrace.research.shufflebench.record;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

public class Tuple2StringKeyKafkaSerializer extends Tuple2KeyKafkaSerializer<String> {

    public Tuple2StringKeyKafkaSerializer() {
        super(new StringSerializer());
    }

}
