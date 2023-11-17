package com.dynatrace.research.shufflebench.record;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;

public class TimestampedRecordSerde implements Serde<TimestampedRecord> {

  @Override
  public Serializer<TimestampedRecord> serializer() {
    return new TimestampedRecordSerializer();
  }

  @Override
  public Deserializer<TimestampedRecord> deserializer() {
    return new TimestampedRecordDeserializer();
  }

  public static class TimestampedRecordSerializer implements Serializer<TimestampedRecord> {

    @Override
    public byte[] serialize(String topic, TimestampedRecord record) {
      final ByteBuffer buffer = ByteBuffer.wrap(new byte[Long.BYTES + record.getData().length]);
      buffer.putLong(record.getTimestamp());
      buffer.put(record.getData());
      return buffer.array();
    }

  }

  public static class TimestampedRecordDeserializer implements Deserializer<TimestampedRecord> {

    @Override
    public TimestampedRecord deserialize(String topic, byte[] bytes) {
      final ByteBuffer buffer = ByteBuffer.wrap(bytes);
      final long timestamp = buffer.getLong();
      final byte[] data = new byte[buffer.remaining()];
      buffer.get(data);
      return new TimestampedRecord(timestamp, data);
    }

  }
}
