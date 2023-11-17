package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.consumer.State;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

public class StateSerializer implements StreamSerializer<State> {

  private static final int TYPE_ID = 3;

  @Override
  public int getTypeId() {
    return TYPE_ID;
  }

  @Override
  public void write(ObjectDataOutput out, State state) throws IOException {
    out.writeByteArray(state.getData());
  }

  @Override
  public State read(ObjectDataInput in) throws IOException {
    return new State(in.readByteArray());
  }

}
