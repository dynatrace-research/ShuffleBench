package com.dynatrace.research.shufflebench.record;

import com.dynatrace.research.shufflebench.util.Util;

public class Record {

  private final byte[] data;

  public Record(final byte[] data) {
    this.data = data;
  }

  public byte[] getData() {
    return this.data;
  }

  @Override
  public String toString() {
    return "Record{" + "data=" + Util.bytesToHex(this.data, 0, 16) + "...}";
  }
}
