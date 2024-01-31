package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.research.shufflebench.matcher.MatcherService;
import com.dynatrace.research.shufflebench.matcher.MatchingRule;
import com.dynatrace.research.shufflebench.record.Record;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class SerializableMatcherService<T extends Record> implements MatcherService<T>, Serializable {

  private static final long serialVersionUID = 4976702883853651904L;

  private transient MatcherService<T> matcherService;

  private final SerializableSupplier<T> matcherServiceFactory;

  public SerializableMatcherService(SerializableSupplier<T> matcherServiceFactory) {
    this.matcherServiceFactory = matcherServiceFactory;
    this.buildMatcherServiceIfAbsent();
  }

  @Override
  public void addMatchingRule(String id, MatchingRule matchingRule) {
    throw new UnsupportedOperationException("A SerializableMatcherService cannot be changed after it has been constructed.");
  }

  @Override
  public boolean removeMatchingRule(String id) {
    throw new UnsupportedOperationException("A SerializableMatcherService cannot be changed after it has been constructed.");
  }

  @Override
  public Collection<Map.Entry<String, T>> match(T record) {
    this.buildMatcherServiceIfAbsent();
    return this.matcherService.match(record);
  }

  private void buildMatcherServiceIfAbsent() {
    if (this.matcherService == null) {
      this.matcherService = this.matcherServiceFactory.get();
    }
  }

  public interface SerializableSupplier<T extends Record> extends Supplier<MatcherService<T>>, Serializable {
  }

}
