package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.consumer.ConsumerEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class ConsumerEventSerde implements Serde<ConsumerEvent> {

  @Override
  public Serializer<ConsumerEvent> serializer() {
    return new ConsumerEventSerializer();
  }

  @Override
  public Deserializer<ConsumerEvent> deserializer() {
    return new ConsumerEventDeserializer();
  }

  public static class ConsumerEventSerializer implements Serializer<ConsumerEvent> {

    @Override
    public byte[] serialize(String topic, ConsumerEvent event) {
      return event.getData();
    }

  }

  public static class ConsumerEventDeserializer implements Deserializer<ConsumerEvent> {

    @Override
    public ConsumerEvent deserialize(String topic, byte[] data) {
      return new ConsumerEvent(data);
    }

  }
}
