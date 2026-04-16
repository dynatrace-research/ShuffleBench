# ShuffleBench

A benchmark for generic, large-scale shuffle operations on continuous stream of data, implemented with state-of-the-art stream processing frameworks. For a detailed description, see our [ICPE'24 paper](https://arxiv.org/abs/2403.04570).

We provide implementations for the following frameworks:

* [Apache Flink](https://flink.apache.org/)
* [Apache Spark (Structured Streaming)](https://spark.apache.org/)
* [Apache Kafka Streams](https://kafka.apache.org/documentation/streams/)
* [Hazelcast (with its Jet engine)](https://hazelcast.com/)

Additionally, a load generator for producing synthetic input data at configurable rates and a latency exporter for measuring end-to-end latency in a framework-independent way are provided.

## Usage

The most straightforward way to run experiments with ShuffleBench is to use the [Theodolite](https://www.theodolite.rocks/) benchmarking framework.
This allows you to run experiments on Kubernetes clusters in a fully automated, reproducible way including setting up the stream processing application, starting the load generator, measuring performance metrics, and collecting the results.

Theodolite benchmark specifications for ShuffleBench can be found in [`kubernetes`](kubernetes). There, you can also find detailed instructions on how to run the benchmarks.

To engage at a lower level, you can also run the benchmark implementations and the load generator manually using the Kubernetes manifests in [`kubernetes`](kubernetes) or run the provided container images or the Java applications directly.

## Build and Package Project

Gradle is used to build, test, and package the benchmark implementations, the load generator, and the latency exporter tool.
To build all subprojects, run:

```sh
./gradlew build
```

## Build and Publish Images

Except the ShuffleBench implementation for Spark, all implementations can be packaged as container images and pushed to a registry using Jib by running:

```sh
ORG_GRADLE_PROJECT_imageRepository=<your.registry.com>/shufflebench ./gradlew jib
```

For Spark, the image has to be built and pushed manually (e.g., using the Docker daemon):

```sh
docker build -t <your.registry.com>/shufflebench/shufflebench-spark shuffle-spark/
docker push <your.registry.com>/shufflebench/shufflebench-spark
```

## How to Cite

If you use ShuffleBench in your research, please cite:

> Sören Henning, Adriano Vogel, Michael Leichtfried, Otmar Ertl, and Rick Rabiser. 2024. ShuffleBench: A Benchmark for Large-Scale Data Shuffling Operations with Distributed Stream Processing Frameworks. In *Proceedings of the 15th ACM/SPEC International Conference on Performance Engineering (ICPE '24)*. Association for Computing Machinery, New York, NY, USA, 2–13. DOI: [10.1145/3629526.3645036](https://doi.org/10.1145/3629526.3645036)
