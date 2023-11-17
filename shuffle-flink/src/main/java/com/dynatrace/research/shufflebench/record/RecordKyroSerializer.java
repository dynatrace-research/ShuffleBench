package com.dynatrace.research.shufflebench.record;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;

public class RecordKyroSerializer extends Serializer<Record> implements Serializable {

  private static final long serialVersionUID = 728071037176839227L;

  @Override
  public void write(Kryo kryo, Output output, Record record) {
    final byte[] data = record.getData();
    output.writeInt(data.length);
    output.writeBytes(data);
  }

  @Override
  public Record read(Kryo kryo, Input input, Class<Record> type) {
    final int length = input.readInt();
    final byte[] bytes = input.readBytes(length);
    return new Record(bytes);
  }

}
