package com.dynatrace.research.shufflebench.record;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka serde for {@link Record} values.
 */
public class RecordSerde implements Serde<Record> {

  /**
   * Creates a record serde.
   */
  public RecordSerde() {
  }

  @Override
  public Serializer<Record> serializer() {
    return new RecordSerializer();
  }

  @Override
  public Deserializer<Record> deserializer() {
    return new RecordDeserializer();
  }

  /**
   * Serializer for {@link Record} instances.
   */
  public static class RecordSerializer implements Serializer<Record> {

    /**
     * Creates a record serializer.
     */
    public RecordSerializer() {
    }

    @Override
    public byte[] serialize(String topic, Record record) {
      return record.getData();
    }

  }

  /**
   * Deserializer for {@link Record} instances.
   */
  public static class RecordDeserializer implements Deserializer<Record> {

    /**
     * Creates a record deserializer.
     */
    public RecordDeserializer() {
    }

    @Override
    public Record deserialize(String topic, byte[] data) {
      return new Record(data);
    }

  }
}
