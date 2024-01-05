package com.dynatrace.research.shufflebench.consumer;

import com.dynatrace.hash4j.hashing.Hasher32;
import com.dynatrace.hash4j.hashing.Hasher64;
import com.dynatrace.hash4j.hashing.Hashing;
import com.dynatrace.hash4j.random.PseudoRandomGenerator;
import com.dynatrace.hash4j.random.PseudoRandomGeneratorProvider;
import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.SplittableRandom;

import static java.util.Objects.requireNonNull;


public class AdvancedStateConsumer implements StatefulConsumer {

  private static final long serialVersionUID = 0L;

  private static final int DEFAULT_STATE_SIZE = 4 * Long.BYTES;

  private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedStateConsumer.class);

  private final String name;

  private final int outputRate;

  private final int stateSizeInBytes;

  private final boolean initCountRandom;

  private final Hasher64 hasher;

  public AdvancedStateConsumer(String name, int outputRate) {
    this(name, outputRate, DEFAULT_STATE_SIZE);
  }

  public AdvancedStateConsumer(String name, int outputRate, int stateSizeInBytes) {
    this(name, outputRate, stateSizeInBytes, false, 0);
  }

  public AdvancedStateConsumer(String name, int outputRate, int stateSizeInBytes, boolean initCountRandom, long seed) {
    this.name = requireNonNull(name);
    this.outputRate = outputRate;
    this.stateSizeInBytes = requireStateSizeGteDefault(stateSizeInBytes);
    this.initCountRandom = initCountRandom;
    this.hasher = Hashing.komihash4_3(seed);
  }

  @Override
  public ConsumerResult accept(TimestampedRecord record, State state) {
    if (state == null) {
      state = new State();
    }

    byte[] data = state.getData();
    long countInit = -1; // No count init per default
    if (data == null) {
      data = new byte[stateSizeInBytes];
      state.setData(data);

      if (initCountRandom) {
        // Take first 32 bytes of record (or less if record is smaller) as seed for random
        final long seedForRandom = hasher.hashBytesToLong(record.getData(), 0, Math.min(record.getData().length, 32));
        final SplittableRandom random = new SplittableRandom(seedForRandom);
        countInit = random.nextInt(outputRate);
      }

    }

    final ByteBuffer stateBuffer = ByteBuffer.wrap(data);
    final long count = ((countInit == -1) ? stateBuffer.getLong() : countInit) + 1;
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
    final int bytesToCopy = Math.min(stateBuffer.remaining(), record.getData().length);
    stateBuffer.put(record.getData(), 0, bytesToCopy); // fill with data from record

    LOGGER.debug("{}: count = {}", name, count);

    if (count == this.outputRate) {
      final ConsumerEvent event = new ConsumerEvent(Arrays.copyOf(data, data.length));
      Arrays.fill(data, (byte) 0); // reset the state byte buffer to 0s
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
