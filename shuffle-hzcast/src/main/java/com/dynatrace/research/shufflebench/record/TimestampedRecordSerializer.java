package com.dynatrace.research.shufflebench.record;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

public class TimestampedRecordSerializer implements StreamSerializer<TimestampedRecord> {

  private static final int TYPE_ID = 2;

  @Override
  public int getTypeId() {
    return TYPE_ID;
  }

  @Override
  public void write(ObjectDataOutput out, TimestampedRecord record) throws IOException {
    out.writeLong(record.getTimestamp());
    out.writeByteArray(record.getData());
  }

  @Override
  public TimestampedRecord read(ObjectDataInput in) throws IOException {
    final long timestamp = in.readLong();
    final byte[] data = in.readByteArray();
    return new TimestampedRecord(timestamp, data);
  }

}
