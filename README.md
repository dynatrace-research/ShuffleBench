# ShuffleBench

A benchmark for generic, large-scale shuffle operations on continuous stream of data, implemented with state-of-the-art stream processing frameworks.

Currently, we provide implementations for the following frameworks:

* [Apache Flink](https://flink.apache.org/)
* [Apache Spark (Structured Streaming)](https://spark.apache.org/)
* [Apache Kafka Streams](https://kafka.apache.org/documentation/streams/)
* [Hazlecast (with its Jet engine)](https://hazelcast.com/)

Additionally, a load generator and a tool for measuring and exporting the latency are provided.

## Usage

The most straightforward way to run experiments with ShuffleBench is to use the [Theodolite](https://www.theodolite.rocks/) benchmarking framework.
This allows you to run experiments on Kubernetes clusters in a fully automated, reproducible way including setting up stream processing application, starting the load generator, measuring performance metrics, and collecting the results.

Theodolite benchmark specifications for ShuffleBench can be found in [`kubernetes`](kubernetes). There, you can also find detailed instructions on how to run the benchmarks.

To engage at a lower level, you can also run the benchmark implementations and the load generator manually using the Kubernetes manifests in [`kubernetes`](kubernetes) or run the provided container images or the Java applications directly.

## Build and Package Project

Gradle is used to build, test, and package the benchmark implementations, the load generator, and the latency exporter tool.
To build all subprojects, run:

```sh
./gradlew build
```

## Build and Publish Images

Except the Shufflebench implementations for Spark, all implementations can be packaged as container images and pushed to a registry using Jib by running:

```sh
ORG_GRADLE_PROJECT_imageRepository=<your.registry.com>/shufflebench ./gradlew jib
```

For Spark, we have to build and push the image manually (e.g., using the Docker deamon):

```sh
docker build -t <your.registry.com>/shufflebench/shufflebench-spark shuffle-spark/
docker push <your.registry.com>/shufflebench/shufflebench-spark
```
