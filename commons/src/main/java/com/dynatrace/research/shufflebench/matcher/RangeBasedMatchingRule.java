package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.hash4j.hashing.Hasher64;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.research.shufflebench.record.Record;

public class RangeBasedMatchingRule implements MatchingRule {

    private final long rangeOffset;

    private final long rangeWidth; // a negative value indicates 100% matching probability

    // seed should be random
    public RangeBasedMatchingRule(long seed, double matchProbability) {
        this.rangeOffset = seed;
        if (matchProbability >= 1) {
            this.rangeWidth = 0x8000000000000000L;
        } else {
            this.rangeWidth = (long) (Math.scalb(matchProbability, 63));
        }
    }

    @Override
    public boolean test(Record record) {
        if (rangeWidth < 0) {
            return true;
        } else {
            return ((extractHashValueFromRecord(record) - rangeOffset) & 0x7fffffffffffffffL) < rangeWidth;
        }
    }

    public long getRangeOffset() {
        return rangeOffset;
    }

    public long getRangeWidth() {
        return rangeWidth;
    }

    private static final Hasher64 HASHER = Hashing.komihash5_0(0x81c1f2b68968424dL);

    public static long extractHashValueFromRecord(Record record) {
        byte[] data = record.getData();
        // since data is assumed to be random bytes, it is more than sufficient to hash only the first
        // 16 bytes
        return HASHER.hashBytesToLong(record.getData(), 0, Math.min(16, data.length));
    }
}
