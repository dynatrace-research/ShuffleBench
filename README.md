# ShuffleBench

A benchmark for generic, large-scale shuffle operations on continuous stream of data, implemented with state-of-the-art stream processing frameworks.

Currently, we are using ShuffleBench for fault tolerance measurements with the following frameworks:

* [Apache Flink](https://flink.apache.org/)
* [Apache Spark (Structured Streaming)](https://spark.apache.org/)
* [Apache Kafka Streams](https://kafka.apache.org/documentation/streams/)

Additionally, a load generator and a tool for measuring and exporting the latency are provided.

## Usage

The most straightforward way to run experiments with ShuffleBench is to use the [Theodolite](https://www.theodolite.rocks/) benchmarking framework.
This allows you to run experiments on Kubernetes clusters in a fully automated, reproducible way including setting up stream processing application, starting the load generator, measuring performance metrics, and collecting the results.

Theodolite benchmark specifications for ShuffleBench can be found in [`kubernetes`](kubernetes). There, you can also find detailed instructions on how to run the benchmarks.

To engage at a lower level, you can also run the benchmark implementations and the load generator manually using the Kubernetes manifests in [`kubernetes`](kubernetes) or run the provided container images or the Java applications directly.

The YAML files used to run fault tolerance experiments can be found in [`evaluation/fault-tolerance`](evaluation/fault-tolerance). 

The YAML files applied to Chaos Mesh to inject failures can be found in [`evaluation/chaos-mesh`](evaluation/chaos-mesh). 

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

## How to Cite

If you use the fault tolerant ShuffleBench in your research, please cite:

> Adriano Vogel, Sören Henning, Esteban Perez-Wohlfeil, Otmar Ertl, and Rick Rabiser. 2024. A Comprehensive Benchmarking Analysis of Fault Recovery in Stream Processing Frameworks. In *Proceedings of the 18th ACM International Conference on Distributed and Event-based Systems (DEBS'24)*. [doi](
https://doi.org/10.1145/3629104.3666040) -->

> Sören Henning, Adriano Vogel, Michael Leichtfried, Otmar Ertl, and Rick Rabiser. 2024. ShuffleBench: A Benchmark for Large-Scale Data Shuffling Operations with Distributed Stream Processing Frameworks. In *Proceedings of the 2024 ACM/SPEC International Conference on Performance Engineering (ICPE '24)*. [doi](
https://doi.org/10.48550/arXiv.2403.04570) <!-- DOI: [10.1145/3629526.3645036](https://doi.org/10.1145/3629526.3645036) -->


