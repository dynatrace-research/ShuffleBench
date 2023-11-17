package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.research.shufflebench.record.Record;
import org.hipparchus.stat.inference.AlternativeHypothesis;
import org.hipparchus.stat.inference.BinomialTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RangeBasedMatchingRuleIndexTest {

    private static long getSeed(String id) {
        return Hashing.komihash5_0().hashCharsToLong(id);
    }

    @Test
    void testIndex() {

        int numMatcherPerProbability = 10;
        double[] probabilities = IntStream.range(0, 9).mapToDouble(i -> Math.pow(0.5, i)).toArray();

        RangeBasedMatchingRuleIndex index = new RangeBasedMatchingRuleIndex();

        int idCounter = 0;
        Map<String, Double> id2probability = new HashMap<>();
        Map<String, Integer> id2MatchCount = new HashMap<>();
        for(double probability : probabilities) {
            for(int i = 0; i < numMatcherPerProbability; ++i)   {
                idCounter += 1;
                String id = idCounter + " (matching probability = " + probability + ")";
                index.add(id, new RangeBasedMatchingRule(getSeed(id), probability));
                id2probability.put(id, probability);
                id2MatchCount.put(id, 0);
            }
        }

        int numCycles = 100000;

        SplittableRandom random = new SplittableRandom(0x3feb3939ecc5531fL);

        for(int i =0; i < numCycles; ++i) {
            byte[] data = new byte[8];
            random.nextBytes(data);
            Record record = new Record(data);
            index.forEachMatchingConsumer(record, s -> id2MatchCount.merge(s, 1, Integer::sum));
        }

        for(Map.Entry<String, Double> entry : id2probability.entrySet()) {
            assertThat(
                    new BinomialTest()
                            .binomialTest(numCycles, id2MatchCount.get(entry.getKey()), entry.getValue(), AlternativeHypothesis.TWO_SIDED))
                    .isGreaterThan(0.01);
        }
    }

    @Test
    void testAddAndRemove() {

        RangeBasedMatchingRuleIndex index = new RangeBasedMatchingRuleIndex();
        RangeBasedMatchingRule rule = new RangeBasedMatchingRule(1, 0.5);
        index.add("1", rule);

        assertThat(index.remove("2")).isFalse();
        assertThat(index.remove("1")).isTrue();
        assertThat(index.remove("1")).isFalse();
    }
}
