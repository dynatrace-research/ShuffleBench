package com.dynatrace.research.shufflebench.record;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;

public class TimestampedRecordKyroSerializer extends Serializer<TimestampedRecord> implements Serializable {

  private static final long serialVersionUID = 728071037176839227L;

  @Override
  public void write(Kryo kryo, Output output, TimestampedRecord record) {
    output.writeLong(record.getTimestamp());
    final byte[] data = record.getData();
    output.writeInt(data.length);
    output.writeBytes(data);
  }

  @Override
  public TimestampedRecord read(Kryo kryo, Input input, Class<TimestampedRecord> type) {
    final long timestamp = input.readLong();
    final int length = input.readInt();
    final byte[] data = input.readBytes(length);
    return new TimestampedRecord(timestamp, data);
  }

}
