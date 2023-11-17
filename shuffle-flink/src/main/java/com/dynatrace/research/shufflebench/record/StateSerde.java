package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.consumer.State;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class StateSerde implements Serde<State> {

  @Override
  public Serializer<State> serializer() {
    return new StateSerializer();
  }

  @Override
  public Deserializer<State> deserializer() {
    return new StateDeserializer();
  }

  public static class StateSerializer implements Serializer<State> {

    @Override
    public byte[] serialize(String topic, State state) {
      return state.getData();
    }

  }

  public static class StateDeserializer implements Deserializer<State> {

    @Override
    public State deserialize(String topic, byte[] data) {
      final State state = new State();
      state.setData(data);
      return state;
    }

  }
}
