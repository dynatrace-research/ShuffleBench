package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.record.Record;
import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

public class TimestampAssignerFunction extends ProcessFunction<Record, TimestampedRecord> {

    @Override
    public void processElement(Record record, Context ctx, Collector<TimestampedRecord> out) throws Exception {
        out.collect(new TimestampedRecord(ctx.timestamp(), record.getData()));
    }

}
