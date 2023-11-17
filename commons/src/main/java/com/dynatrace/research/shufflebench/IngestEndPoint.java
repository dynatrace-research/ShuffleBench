package com.dynatrace.research.shufflebench;

import com.dynatrace.research.shufflebench.record.Record;

import java.util.UUID;
import java.util.function.Consumer;

public interface IngestEndPoint extends Consumer<Record> {

  @Override
  void accept(Record record);

  UUID getUUID();
}
