package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.research.shufflebench.record.Record;

import java.util.Collection;
import java.util.Map;

public interface MatcherService<T extends Record> {

  /**
   * Adds a new matching rule
   *
   * @param id           an ID for the matching rule
   * @param matchingRule the matching rule
   */
  void addMatchingRule(String id, MatchingRule matchingRule);

  /**
   * Removes a matching rule
   *
   * @param id the ID of the matching rule to be deleted
   * @return true if a round was found deleted, false otherwise
   */
  boolean removeMatchingRule(String id);

  /**
   * Finds the IDs of all corresponding consumers for this record.
   *
   * @param record The record to be matched
   */
  Collection<Map.Entry<String, T>> match(T record);
}
