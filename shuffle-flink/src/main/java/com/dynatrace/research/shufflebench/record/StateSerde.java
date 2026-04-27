package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.consumer.State;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka serde for {@link State} values.
 */
public class StateSerde implements Serde<State> {

  /**
   * Creates a state serde.
   */
  public StateSerde() {
  }

  @Override
  public Serializer<State> serializer() {
    return new StateSerializer();
  }

  @Override
  public Deserializer<State> deserializer() {
    return new StateDeserializer();
  }

  /**
   * Serializer for {@link State} instances.
   */
  public static class StateSerializer implements Serializer<State> {

    /**
     * Creates a state serializer.
     */
    public StateSerializer() {
    }

    @Override
    public byte[] serialize(String topic, State state) {
      return state.getData();
    }

  }

  /**
   * Deserializer for {@link State} instances.
   */
  public static class StateDeserializer implements Deserializer<State> {

    /**
     * Creates a state deserializer.
     */
    public StateDeserializer() {
    }

    @Override
    public State deserialize(String topic, byte[] data) {
      final State state = new State();
      state.setData(data);
      return state;
    }

  }
}
