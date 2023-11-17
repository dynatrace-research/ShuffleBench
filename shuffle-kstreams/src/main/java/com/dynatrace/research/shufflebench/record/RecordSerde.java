package com.dynatrace.research.shufflebench.record;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class RecordSerde implements Serde<Record> {

  @Override
  public Serializer<Record> serializer() {
    return new RecordSerializer();
  }

  @Override
  public Deserializer<Record> deserializer() {
    return new RecordDeserializer();
  }

  public static class RecordSerializer implements Serializer<Record> {

    @Override
    public byte[] serialize(String topic, Record record) {
      return record.getData();
    }

  }

  public static class RecordDeserializer implements Deserializer<Record> {

    @Override
    public Record deserialize(String topic, byte[] data) {
      return new Record(data);
    }

  }
}
