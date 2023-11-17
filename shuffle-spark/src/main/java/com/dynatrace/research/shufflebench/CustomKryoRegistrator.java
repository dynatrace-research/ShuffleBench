package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.consumer.State;
import com.dynatrace.research.shufflebench.record.*;
import com.esotericsoftware.kryo.Kryo;
import org.apache.spark.serializer.KryoRegistrator;

public class CustomKryoRegistrator implements KryoRegistrator {

  @Override
  public void registerClasses(Kryo kryo) {
    kryo.register(Record.class, new RecordKyroSerializer());
    kryo.register(TimestampedRecord.class, new TimestampedRecordKyroSerializer());
    kryo.register(State.class, new StateKyroSerializer());
  }

}
