package com.dynatrace.research.shufflebench.record;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;

/**
 * Kryo serializer for benchmark {@link com.dynatrace.research.shufflebench.record.Record} values.
 */
public class RecordKyroSerializer extends Serializer<com.dynatrace.research.shufflebench.record.Record> implements Serializable {

  private static final long serialVersionUID = 728071037176839227L;

  /**
   * Creates a record Kryo serializer.
   */
  public RecordKyroSerializer() {
  }

  @Override
  public void write(Kryo kryo, Output output, com.dynatrace.research.shufflebench.record.Record record) {
    final byte[] data = record.getData();
    output.writeInt(data.length);
    output.writeBytes(data);
  }

  @Override
  public com.dynatrace.research.shufflebench.record.Record read(
          Kryo kryo,
          Input input,
          Class<? extends com.dynatrace.research.shufflebench.record.Record> type) {
    final int length = input.readInt();
    final byte[] bytes = input.readBytes(length);
    return new com.dynatrace.research.shufflebench.record.Record(bytes);
  }
}
