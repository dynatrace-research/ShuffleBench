package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.util.Util;

public class TimestampedRecord extends Record {

  private final long timestamp;

  public TimestampedRecord(final long timestamp, final byte[] data) {
    super(data);
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  @Override
  public String toString() {
    return "Record{" + "timestamp="+ this.timestamp + "," + "data=" + Util.bytesToHex(super.getData(), 0, 16) + "...}";
  }
}
