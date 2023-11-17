package com.dynatrace.research.shufflebench.record;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

public class RecordSerializer implements StreamSerializer<Record> {

  private static final int TYPE_ID = 1;

  @Override
  public int getTypeId() {
    return TYPE_ID;
  }

  @Override
  public void write(ObjectDataOutput out, Record record) throws IOException {
    out.writeByteArray(record.getData());
  }

  @Override
  public Record read(ObjectDataInput in) throws IOException {
    return new Record(in.readByteArray());
  }

}
