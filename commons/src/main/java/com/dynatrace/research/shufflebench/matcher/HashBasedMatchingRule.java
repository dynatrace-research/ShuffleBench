package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.hash4j.hashing.Hasher64;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.research.shufflebench.record.Record;

/**
 * Matches records with a certain probability based on a hash value.
 */
public class HashBasedMatchingRule implements MatchingRule {
  private final Hasher64 hasher;

  private final long hashThreshold;

  public HashBasedMatchingRule(long seed, double samplingRate) {
    this.hasher = Hashing.komihash4_3(seed);
    hashThreshold = (long) (samplingRate * Long.MAX_VALUE);
  }

  @Override
  public boolean test(Record record) {
    byte[] data = record.getData();
    // since data is assumed to be random bytes, it is more than sufficient to hash only the first
    // 16 bytes
    long hash = hasher.hashBytesToLong(record.getData(), 0, Math.min(16, data.length));
    return (hash & 0x7FFFFFFFFFFFFFFFL) <= hashThreshold; // get rid of the sign
  }

}
