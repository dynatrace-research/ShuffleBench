package com.dynatrace.research.shufflebench.consumer;

import com.dynatrace.research.shufflebench.record.TimestampedRecord;

import java.io.Serializable;
import java.util.function.Supplier;

public class SerializableStatefulConsumer implements StatefulConsumer, Serializable {

    private transient StatefulConsumer consumer;

    private final SerializableStatefulConsumer.SerializableSupplier consumerFactory;

    public SerializableStatefulConsumer(SerializableStatefulConsumer.SerializableSupplier consumerFactory) {
        this.consumerFactory = consumerFactory;
        this.buildConsumerIfAbsent();
    }

    @Override
    public ConsumerResult accept(TimestampedRecord record, State state) {
        this.buildConsumerIfAbsent();
        return this.consumer.accept(record, state);
    }

    private void buildConsumerIfAbsent() {
        if (this.consumer == null) {
            this.consumer = this.consumerFactory.get();
        }
    }

    public interface SerializableSupplier extends Supplier<StatefulConsumer>, Serializable {
    }

}
