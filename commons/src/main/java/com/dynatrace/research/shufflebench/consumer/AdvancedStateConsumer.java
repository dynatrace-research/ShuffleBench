package com.dynatrace.research.shufflebench.consumer;

import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;


public class AdvancedStateConsumer implements StatefulConsumer {

  private static final long serialVersionUID = 0L;

  private static final int DEFAULT_STATE_SIZE = 4 * Long.BYTES;

  private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedStateConsumer.class);

  private final String name;

  private final int outputRate;

  private final int stateSizeInBytes;

  public AdvancedStateConsumer(String name, int outputRate) {
    this(name, outputRate, DEFAULT_STATE_SIZE);
  }

  public AdvancedStateConsumer(String name, int outputRate, int stateSizeInBytes) {
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

    final ByteBuffer stateBuffer = ByteBuffer.wrap(data);
    final long count = stateBuffer.getLong() + 1;
    final long sum = stateBuffer.getLong();

    stateBuffer.rewind();
    stateBuffer.putLong(count);
    stateBuffer.putLong(sum + ByteBuffer.wrap(record.getData()).getLong()); // Is allowed to overflow
    if (count == 1) {
      stateBuffer.putLong(record.getTimestamp()); // start timestamp
    } else {
      stateBuffer.position(stateBuffer.position() + Long.BYTES); // start timestamp
    }
    stateBuffer.putLong(record.getTimestamp()); // end timestamp
    stateBuffer.put(record.getData(), 0, stateBuffer.remaining()); // fill with data from record

    LOGGER.debug("{}: count = {}", name, count);

    if (count == this.outputRate) {
      final ConsumerEvent event = new ConsumerEvent(Arrays.copyOf(data, data.length));
      Arrays.fill(data, (byte) 0);
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
