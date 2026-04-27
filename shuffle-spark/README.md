# ShuffleBench — Spark (Structured Streaming) implementation

Spark 4.1.1 / Scala 2.13 / Java 21 implementation of the ShuffleBench pipeline.

## Configuration

Configuration is read from MicroProfile Config (`META-INF/microprofile-config.properties`)
when `USE_MICROPROFILE_CONFIG=true`, otherwise from environment variables.
Each property maps to an env var by upper-casing the key and replacing `.`/`-` with `_`,
e.g. `kafka.bootstrap.servers` → `KAFKA_BOOTSTRAP_SERVERS`.

### Streaming trigger

The query trigger is selectable at runtime. **Default: Spark's standard micro-batch
trigger** (no explicit `.trigger(...)` call), matching the original benchmark behavior.

| Property | Env var | Default | Description |
|---|---|---|---|
| `spark.trigger.mode` | `SPARK_TRIGGER_MODE` | `default` | One of `default`, `processing`, `continuous`, `realtime`. |
| `spark.trigger.interval` | `SPARK_TRIGGER_INTERVAL` | – | Required when mode ≠ `default`, e.g. `200 milliseconds`. |
| `spark.trigger.realtime` | `SPARK_TRIGGER_REALTIME` | – | Legacy fallback for the realtime interval. |

Mode behavior:

- `default` — no explicit trigger; Spark fires a new micro-batch as soon as the
  previous one finishes (original benchmark behavior).
- `processing` — `Trigger.ProcessingTime(interval)`.
- `continuous` — `Trigger.Continuous(interval)`.
- `realtime` — `Trigger.RealTime(interval)` (Spark 4.x real-time mode).

Examples — enabling realtime:

```sh
# Environment variables (Docker / Kubernetes)
-e SPARK_TRIGGER_MODE=realtime -e SPARK_TRIGGER_INTERVAL="200 milliseconds"

# JVM system properties (local runs)
-Dspark.trigger.mode=realtime -Dspark.trigger.interval="200 milliseconds"
```

If a non-`default` mode is selected without an interval, the application fails
fast with a clear error.

### Other Kafka/source knobs

| Property | Description |
|---|---|
| `kafka.bootstrap.servers` | Kafka bootstrap servers. |
| `kafka.topic.input` / `kafka.topic.output` | Input/output topics. |
| `spark.max.offsets.per.trigger` | Optional Kafka source `maxOffsetsPerTrigger`. |
| `spark.min.offsets.per.trigger` | Optional Kafka source `minOffsetsPerTrigger`. |

## Build & run

```sh
# Build the shaded jar
./gradlew :shuffle-spark:shadowJar

# Build the container image
docker build -t <registry>/shufflebench/shufflebench-spark shuffle-spark/
```

The Dockerfile uses `openjdk-21-jre-headless`, Spark 4.1.0 and Hadoop 3.4.2.
Required `--add-opens` JVM flags for Java 21 are already set in `spark-defaults.conf`.

