package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.research.shufflebench.record.Record;

import java.util.function.Predicate;

/**
 * A stream query that is a predicate.
 */
public interface MatchingRule extends Predicate<Record> {
}
