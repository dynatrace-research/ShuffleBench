package com.dynatrace.research.shufflebench.consumer;

import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;


public class CountingConsumer implements StatefulConsumer {

  private static final long serialVersionUID = 0L;

  private static final VarHandle LONG_VAR_HANDLE =
      MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

  private static final int DEFAULT_STATE_SIZE = Long.BYTES;

  private static final Logger LOGGER = LoggerFactory.getLogger(CountingConsumer.class);

  private final String name;

  private final int outputRate;

  private final int stateSizeInBytes;

  public CountingConsumer(String name, int outputRate) {
    this(name, outputRate, DEFAULT_STATE_SIZE);
  }

  public CountingConsumer(String name, int outputRate, int stateSizeInBytes) {
    this.name = requireNonNull(name);
    this.outputRate = outputRate;
    this.stateSizeInBytes = requireStateSizeGteDefault(stateSizeInBytes);
  }

  @Override
  public ConsumerResult accept(TimestampedRecord record, State state) {
    if (state == null) {
      state = new State();
    }

    byte[] data = state.getData();
    if (data == null) {
      data = new byte[stateSizeInBytes];
      state.setData(data);
    }

    long c = (long) LONG_VAR_HANDLE.get(data, 0);
    c += 1;
    LONG_VAR_HANDLE.set(data, 0, c);

    LOGGER.debug("{}: count = {}", name, c);

    if (c % this.outputRate == 0) {
      final ConsumerEvent event = new ConsumerEvent(Arrays.copyOf(data, data.length));
      return new ConsumerResult(state, event);
    } else {
      return new ConsumerResult(state);
    }
  }

  private static int requireStateSizeGteDefault(int stateSize) {
    if (stateSize < DEFAULT_STATE_SIZE) {
      throw new IllegalArgumentException("State size must be at least " + DEFAULT_STATE_SIZE + ".");
    }
    return stateSize;
  }

}
