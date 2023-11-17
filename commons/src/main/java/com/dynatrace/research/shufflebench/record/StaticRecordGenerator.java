package com.dynatrace.research.shufflebench.record;

import java.util.function.Supplier;

/**
 * A static generator for records.
 */
public class StaticRecordGenerator implements Supplier<Record> {

  private static final byte[] RECORD = new byte[1024]; // 1kB

  @Override
  public Record get() {
    return new Record(RECORD);
  }
}
