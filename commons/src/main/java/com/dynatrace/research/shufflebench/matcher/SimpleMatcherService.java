package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.research.shufflebench.record.Record;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SimpleMatcherService<T extends Record> implements MatcherService<T> {

  private final Map<String, MatchingRule> matchingRuleEntries = new HashMap<>();

  private final RangeBasedMatchingRuleIndex rangeBasedMatchingRuleIndex = new RangeBasedMatchingRuleIndex();


  @Override
  public void addMatchingRule(String id, MatchingRule matchingRule) {
    if (matchingRule instanceof RangeBasedMatchingRule) {
      matchingRuleEntries.remove(id);
      rangeBasedMatchingRuleIndex.add(id, (RangeBasedMatchingRule) matchingRule);
    } else {
      rangeBasedMatchingRuleIndex.remove(id);
      matchingRuleEntries.put(id, matchingRule);
    }
  }

  @Override
  public boolean removeMatchingRule(String id) {
    return (matchingRuleEntries.remove(id) != null) || rangeBasedMatchingRuleIndex.remove(id);
  }

  @Override
  public Collection<Map.Entry<String, T>> match(T record) {
    List<Map.Entry<String, T>> result = new ArrayList<>();

    rangeBasedMatchingRuleIndex.forEachMatchingConsumer(record, id -> result.add(Map.entry(id, record)));

    for (Map.Entry<String, MatchingRule> entry : matchingRuleEntries.entrySet()) {
      if (entry.getValue().test(record)) {
        result.add(Map.entry(entry.getKey(), record));
      }
    }

    return result;
  }

  public static <T extends Record> SimpleMatcherService<T> createFromZipf(
      final int numRules,
      final double totalSelectivity,
      final double s,
      final long seed
  ) {
    double weightsTotal = 0.0;
    final double[] weigths = new double[numRules];
    for (int k = 1; k <= numRules; k++) {
      weigths[k - 1] = 1 / Math.pow(k, s);
      weightsTotal += weigths[k - 1];
    }
    final double finalWeightsTotal = weightsTotal;
    final Stream<Map.Entry<Double, Integer>> frequencyStream = IntStream.range(0, numRules)
        .mapToObj(ruleId -> Map.entry(
            (weigths[ruleId] / finalWeightsTotal) * totalSelectivity, // selectivity
            1 // frequency
        ));
    return createFromFrequencyStream(frequencyStream, seed);
  }

  public static <T extends Record> SimpleMatcherService<T> createFromFrequencyMap(Map<Double, Integer> selectivities, final long seed) {
    return createFromFrequencyStream(selectivities.entrySet().stream(), seed);
  }

  public static <T extends Record> SimpleMatcherService<T> createFromFrequencyStream(Stream<Map.Entry<Double, Integer>> selectivities, final long seed) {
    final SimpleMatcherService<T> matcherService = new SimpleMatcherService<>();
    final AtomicInteger ruleCounter = new AtomicInteger(0);
    selectivities.forEach(entry -> {
      final int numRules = entry.getValue();
      final double selectivity = entry.getKey();
      for (int i = 0; i < numRules; i++) {
        final int ruleNumber = ruleCounter.getAndIncrement();
        matcherService.addMatchingRule(
            "consumer_" + ruleNumber,
            new RangeBasedMatchingRule(Hashing.komihash4_3().hashStream().putLong(seed).putInt(ruleNumber).getAsLong(), selectivity));
      }
    });
    return matcherService;
  }

}
