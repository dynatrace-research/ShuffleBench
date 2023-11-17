package com.dynatrace.research.shufflebench;

import org.apache.kafka.common.serialization.Serializer;

public class NullKeySerializer implements Serializer<Void> {

  @Override
  public byte[] serialize(String topic, Void nothing) {
    return null;
  }

}
