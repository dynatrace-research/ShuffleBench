package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.consumer.ConsumerEvent;
import com.dynatrace.research.shufflebench.consumer.ConsumerResult;
import com.dynatrace.research.shufflebench.consumer.State;
import com.dynatrace.research.shufflebench.consumer.StatefulConsumer;
import com.dynatrace.research.shufflebench.record.TimestampedRecord;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class AggregateFunction
        extends KeyedProcessFunction<String, Tuple2<String, TimestampedRecord>, Tuple2<String, ConsumerEvent>> {

    private final StatefulConsumer consumer;

    private ValueState<State> state;

    public AggregateFunction(StatefulConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        state = super.getRuntimeContext().getState(new ValueStateDescriptor<>("state", State.class));
    }

    @Override
    public void processElement(
            Tuple2<String, TimestampedRecord> recordWithKey,
            Context ctx,
            Collector<Tuple2<String, ConsumerEvent>> out) throws Exception {

        final State stateValue = this.state.value();
        final ConsumerResult consumerResult = this.consumer.accept(recordWithKey.f1, stateValue);
        this.state.update(consumerResult.getState());
        consumerResult.getEvent().ifPresent(event -> out.collect(Tuple2.of(recordWithKey.f0, event)));
    }

    @Override
    public void onTimer(
            long timestamp,
            OnTimerContext ctx,
            Collector<Tuple2<String, ConsumerEvent>> out) throws Exception {
        // nothing to do per default
    }
}
