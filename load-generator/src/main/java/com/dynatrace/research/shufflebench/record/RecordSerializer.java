package com.dynatrace.research.shufflebench.record;

import org.apache.kafka.common.serialization.Serializer;

public class RecordSerializer implements Serializer<Record> {

  @Override
  public byte[] serialize(String topic, Record record) {
    return record.getData();
  }

}
