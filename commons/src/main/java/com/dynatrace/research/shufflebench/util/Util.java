package com.dynatrace.research.shufflebench.util;

public final class Util {

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes, int off, int len) {
    char[] hexChars = new char[len * 2];
    for (int j = 0; j < len; j++) {
      int v = bytes[off + j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }
}
