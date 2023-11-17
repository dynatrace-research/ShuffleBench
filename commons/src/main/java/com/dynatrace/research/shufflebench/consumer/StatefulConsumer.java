package com.dynatrace.research.shufflebench.consumer;

import com.dynatrace.research.shufflebench.record.TimestampedRecord;

import java.io.Serializable;

/**
 * Implementations must be stateless. The state must be kept in the {@link State} object.
 */
@FunctionalInterface
public interface StatefulConsumer extends Serializable {

  /**
   * @param record a new data record
   * @param state  the current state
   * @return the updated state
   */
  ConsumerResult accept(TimestampedRecord record, State state);
}
