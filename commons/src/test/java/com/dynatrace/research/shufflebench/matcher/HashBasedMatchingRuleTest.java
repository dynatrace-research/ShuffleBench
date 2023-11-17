package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.research.shufflebench.record.RandomRecordGenerator;
import org.hipparchus.stat.inference.AlternativeHypothesis;
import org.hipparchus.stat.inference.BinomialTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HashBasedMatchingRuleTest {

  @Test
  void testHashBasedFilter() {

    RandomRecordGenerator recordGenerator = new RandomRecordGenerator(0x9fb8b9541b84f4ccL);

    double probability = 0.3;
    HashBasedMatchingRule filter = new HashBasedMatchingRule(0xe27febeedfc37287L, probability);

    int numTrials = 10000;
    int count = 0;
    for (int i = 0; i < numTrials; ++i) {
      if (filter.test(recordGenerator.get())) count += 1;
    }

    assertThat(
        new BinomialTest()
            .binomialTest(numTrials, count, probability, AlternativeHypothesis.TWO_SIDED))
        .isGreaterThan(0.01);
  }
}
