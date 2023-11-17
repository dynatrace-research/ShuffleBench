package com.dynatrace.research.shufflebench.record;

import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * A random generator for records.
 */
public class RandomRecordGenerator implements Supplier<Record> {

  private static final int DEFAULT_RECORD_SIZE = 1024; // 1kB

  private final SplittableRandom rng;

  private final int recordSize;

  public RandomRecordGenerator(long seed) {
    this(seed, DEFAULT_RECORD_SIZE);
  }

  public RandomRecordGenerator(long seed, int recordSize) {
    this.rng = new SplittableRandom(seed);
    this.recordSize = recordSize;
  }

  @Override
  public Record get() {
    byte[] data = new byte[recordSize];
    rng.nextBytes(data);
    return new Record(data);
  }
}
