package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.record.Record;
import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import org.apache.kafka.streams.processor.api.*;

public class TimestampAssignerProcessor<K> extends ContextualFixedKeyProcessor<K, Record, TimestampedRecord> {

    @Override
    public void process(FixedKeyRecord<K, Record> record) {
        final TimestampedRecord timestampedRecord = new TimestampedRecord(record.timestamp(), record.value().getData());
        super.context().forward(record.withValue(timestampedRecord));
    }

    public static FixedKeyProcessorSupplier<?, Record, TimestampedRecord> supplier() {
        return TimestampAssignerProcessor::new;
    }

}
