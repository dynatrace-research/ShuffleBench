package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.consumer.ConsumerEvent;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class Tuple2ConsumerEventValueKafkaSerializer extends Tuple2ValueKafkaSerializer<ConsumerEvent> {

    public Tuple2ConsumerEventValueKafkaSerializer() {
        super(new ConsumerEventSerde.ConsumerEventSerializer());
    }

}
