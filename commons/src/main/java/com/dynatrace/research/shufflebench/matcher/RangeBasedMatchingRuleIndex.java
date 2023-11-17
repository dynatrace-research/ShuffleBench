package com.dynatrace.research.shufflebench.matcher;

import com.dynatrace.research.shufflebench.record.Record;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class RangeBasedMatchingRuleIndex {

    private static final int NUM_SLOT_EXPONENT = 16;
    private static final int NUM_SLOTS = 1 << NUM_SLOT_EXPONENT;
    private static final int SLOT_WIDTH_EXPONENT = 63 - NUM_SLOT_EXPONENT;
    private static final long SLOT_WIDTH = 1L << SLOT_WIDTH_EXPONENT;

    private final List<List<String>> index = Stream.generate(ArrayList<String>::new).limit(NUM_SLOTS).collect(toList());

    private final Map<String, RangeBasedMatchingRule> matchingRules = new HashMap<>();

    private void forEachOverlappingSlot(RangeBasedMatchingRule matchingRule, IntConsumer slotIndexConsumer) {
        long offset = matchingRule.getRangeOffset();
        long rangeWidth = matchingRule.getRangeWidth();
        if (rangeWidth < 0L) {
            for (int slotIdx = 0; slotIdx < NUM_SLOTS; ++slotIdx) {
                slotIndexConsumer.accept(slotIdx);
            }
        } else {
            for (long l = 0; Long.compareUnsigned(l, rangeWidth + SLOT_WIDTH) < 0; l += SLOT_WIDTH) {
                int slotIdx = (int) (((offset + l) & 0x7fffffffffffffffL) >>> SLOT_WIDTH_EXPONENT);
                slotIndexConsumer.accept(slotIdx);
            }
        }
    }

    public void add(String id, RangeBasedMatchingRule matchingRule) {
        remove(id); // to make sure that there is no rule with same id
        matchingRules.put(id, matchingRule);
        forEachOverlappingSlot(matchingRule, slotIdx -> index.get(slotIdx).add(id));
    }

    public boolean remove(String id) {
        RangeBasedMatchingRule matchingRule = matchingRules.remove(id);
        if (matchingRule != null) {
            forEachOverlappingSlot(matchingRule, slotIdx -> {
                List<String> list = index.get(slotIdx);
                list.remove(id);
            });
            return true;
        } else {
            return false;
        }
    }

    public void forEachMatchingConsumer(Record record, Consumer<String> consumer) {
        long value = RangeBasedMatchingRule.extractHashValueFromRecord(record);
        int slotIdx = (int) ((value & 0x7fffffffffffffffL) >>> SLOT_WIDTH_EXPONENT);
        for(String id : index.get(slotIdx)) {
            if (matchingRules.get(id).test(record)) consumer.accept(id);
        }
    }
}
