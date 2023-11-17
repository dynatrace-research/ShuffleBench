package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.consumer.*;
import com.dynatrace.research.shufflebench.record.StateSerde;
import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.api.*;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.Arrays;
import java.util.Set;

public class StatefulConsumerProcessor extends ContextualFixedKeyProcessor<String, TimestampedRecord, ConsumerEvent> {

    private static final String STATE_STORE_NAME = "consumerAggregation";

    private KeyValueStore<String, State> state;

    private final StatefulConsumer consumer;


    private StatefulConsumerProcessor(final StatefulConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void init(FixedKeyProcessorContext<String, ConsumerEvent> context) {
        super.init(context);
        this.state = context.getStateStore(STATE_STORE_NAME);
    }

    @Override
    public void process(FixedKeyRecord<String, TimestampedRecord> record) {
        final State stateValue = this.state.get(record.key());
        final ConsumerResult consumerResult = this.consumer.accept(record.value(), stateValue);
        this.state.put(record.key(), consumerResult.getState());
        consumerResult.getEvent().ifPresent(event -> {
            final FixedKeyRecord<String, ConsumerEvent> newRecord = record.withValue(event);
            super.context().forward(newRecord);
        });
    }

    public static Supplier supplier(final StatefulConsumer consumer) {
        return supplier(consumer, StoreType.ROCKS_DB);
    }
    public static Supplier supplier(final StatefulConsumer consumer, final StoreType storeType) {
        return new Supplier(consumer, storeType);
    }

    public static class Supplier implements FixedKeyProcessorSupplier<String, TimestampedRecord, ConsumerEvent> {

        private final StatefulConsumer consumer;

        private final StoreType storeType;

        public Supplier(final StatefulConsumer consumer, StoreType storeType) {
            this.consumer = consumer;
            this.storeType = storeType;
        }


        @Override
        public FixedKeyProcessor<String, TimestampedRecord, ConsumerEvent> get() {
            return new StatefulConsumerProcessor(this.consumer);
        }

        @Override
        public Set<StoreBuilder<?>> stores() {
            final StoreBuilder<KeyValueStore<String, State>> keyValueStoreBuilder =
                    Stores.keyValueStoreBuilder(
                            storeType.storeSupplier,
                            Serdes.String(),
                            new StateSerde());
            return Set.of(keyValueStoreBuilder);
        }


    }

    public enum StoreType {
        ROCKS_DB(
                "rocksDB",
                Stores.persistentKeyValueStore(STATE_STORE_NAME)
        ),
        IN_MEMORY(
                "in_memory",
                Stores.inMemoryKeyValueStore(STATE_STORE_NAME)
        );

        private final String name;

        private final KeyValueBytesStoreSupplier storeSupplier;

        StoreType(final String name, KeyValueBytesStoreSupplier storeSupplier) {
            this.name = name;
            this.storeSupplier = storeSupplier;
        }

        public static StoreType fromName(final String name) {
            return Arrays.stream(StoreType.values())
                    .filter(t -> t.name.equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(""));

        }
    }

}
