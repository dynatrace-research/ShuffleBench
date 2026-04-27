package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.consumer.ConsumerEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka serde for {@link ConsumerEvent} values.
 */
public class ConsumerEventSerde implements Serde<ConsumerEvent> {

  /**
   * Creates a consumer-event serde.
   */
  public ConsumerEventSerde() {
  }

  @Override
  public Serializer<ConsumerEvent> serializer() {
    return new ConsumerEventSerializer();
  }

  @Override
  public Deserializer<ConsumerEvent> deserializer() {
    return new ConsumerEventDeserializer();
  }

  /**
   * Serializer for {@link ConsumerEvent} instances.
   */
  public static class ConsumerEventSerializer implements Serializer<ConsumerEvent> {

    /**
     * Creates a consumer-event serializer.
     */
    public ConsumerEventSerializer() {
    }

    @Override
    public byte[] serialize(String topic, ConsumerEvent event) {
      return event.getData();
    }

  }

  /**
   * Deserializer for {@link ConsumerEvent} instances.
   */
  public static class ConsumerEventDeserializer implements Deserializer<ConsumerEvent> {

    /**
     * Creates a consumer-event deserializer.
     */
    public ConsumerEventDeserializer() {
    }

    @Override
    public ConsumerEvent deserialize(String topic, byte[] data) {
      return new ConsumerEvent(data);
    }

  }
}
