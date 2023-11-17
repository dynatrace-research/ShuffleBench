package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.consumer.State;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;

public class StateKyroSerializer extends Serializer<State> implements Serializable {

  private static final long serialVersionUID = 728071037176839228L;

  @Override
  public void write(Kryo kryo, Output output, State state) {
    final byte[] data = state.getData();
    output.writeInt(data.length);
    output.writeBytes(data);
  }

  @Override
  public State read(Kryo kryo, Input input, Class<State> type) {
    final int length = input.readInt();
    final byte[] bytes = input.readBytes(length);
    return new State(bytes);
  }

}
