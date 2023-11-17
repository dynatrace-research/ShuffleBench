package com.dynatrace.research.shufflebench.consumer;

import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CountingConsumerTest {

  @Test
  void test() {
    CountingConsumer consumer = new CountingConsumer("consumer", 1);
    State state = null;

    ConsumerResult consumerResult  = consumer.accept(new TimestampedRecord(0, null), state);

    assertThat(consumerResult.getState().getData()).isEqualTo(new byte[]{0, 0, 0, 0, 0, 0, 0, 1});

    consumerResult = consumer.accept(new TimestampedRecord(0, null), consumerResult.getState());

    assertThat(consumerResult.getState().getData()).isEqualTo(new byte[]{0, 0, 0, 0, 0, 0, 0, 2});
  }
}
