# Running ShuffleBench in Kubernetes

***Make sure to adjust all images in the Kubernetes manifests to the registry you published you images to!***

The following document provides documentation on how to set up a Kubernetes cluster in AWS and run the ShuffleBench implementations:

## Setup Kubernetes Cluster in AWS

The `cluster.yaml` file defines an [Amazon EKS](https://aws.amazon.com/eks/) Kubernetes cluster consisting of 3 node groups.
To set it up, **adjust the cluster name in the file** and run:

```sh
eksctl create cluster -f cluster.yaml # Make sure to adjust the cluster name before
```

EKS does not set up the Kubernetes metric server by default, which means that `kubectl top` does not work out-of-the-box.
To install it, run:

```sh
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

To later run Kafka, we need to create the `kafka` storage class. For AWS, this can be done by running:

```sh
kubectl apply -f aws/kafka-storage-class.yaml
```

When installing Kafka, this storage class ensures that corresponding [Amazon EBS](https://aws.amazon.com/ebs/) volumes are created.

**Hint: Both the EKS cluster and the EBS volumes are rather heavy-sized and, thus, might cause high costs when run for longer time.**

## Install Theodolite

[Theodolite](https://www.theodolite.rocks/) is a framework for running scalability benchmark in Kubernetes.
We use it currently for two reasons:
* Setting up our benchmark infrastructure including Kafka, Prometheus and Prometheus-related monitoring.
* Defining benchmarks for throughput, latency, and scalability experiments.

Not all Theodolite features we need, are already released. While this will happen soon, we currently have to install Theodolite by first cloning its git repository:

```sh
git clone git@github.com:cau-se/theodolite.git
helm dependencies update theodolite/helm
helm install theodolite theodolite/helm -f https://raw.githubusercontent.com/cau-se/theodolite/main/helm/preconfigs/extended-metrics.yaml -f values.yaml -f values-aws-nodegroups.yaml
```

Once the required Theodolite version has been released (v0.9), we can install it with:

```sh
helm repo add theodolite https://www.theodolite.rocks
helm repo update
helm install theodolite theodolite/theodolite -f https://raw.githubusercontent.com/cau-se/theodolite/main/helm/preconfigs/extended-metrics.yaml -f values.yaml -f values-aws-nodegroups.yaml
```

## Manually Run Benchmark Implementations

The `shuffle-kstreams`, `shuffle-hzcast`, `shuffle-flink`, and `shuffle-load-generator` directories contain Kubernetes manifest files to run the individual components. For example, to deploy the Kafka Streams implementation with the load generator, run:

```sh
kubectl apply -f shuffle-latency-exporter # Only required if latency should be measured
kubectl apply -f shuffle-kstreams
# Maybe wait some time
kubectl apply -f shuffle-load-generator
```

You might want to adjust the manifests before to, for example, test different load intensities, numbers of replicas, or framework-specific configurations.

To remove the components run:

```sh
kubectl delete -f shuffle-load-generator
# Maybe wait some time
kubectl delete -f shuffle-kstreams
kubectl delete -f shuffle-latency-exporter
```

Doing this manually for many different configurations can be exhaustive work. We now see how to automate this with Theodolite.

## Install Theodolite Benchmarks

Theodolite automates benchmarking in Kubernetes by providing [Kubernetes CRDs](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/) for benchmarks and their executions. We provide one benchmark for each evaluated framework. To install them, run the following commands:

```sh
# Delete configmaps if already created before
kubectl delete configmaps --ignore-not-found=true shufflebench-resources-load-generator shufflebench-resources-latency-exporter shufflebench-resources-kstreams shufflebench-resources-hzcast shufflebench-resources-flink shufflebench-resources-spark 
kubectl create configmap shufflebench-resources-load-generator --from-file ./shuffle-load-generator/
kubectl create configmap shufflebench-resources-latency-exporter --from-file ./shuffle-latency-exporter/
kubectl create configmap shufflebench-resources-kstreams --from-file ./shuffle-kstreams/
kubectl create configmap shufflebench-resources-hzcast --from-file ./shuffle-hzcast/
kubectl create configmap shufflebench-resources-flink --from-file ./shuffle-flink/
kubectl create configmap shufflebench-resources-spark --from-file ./shuffle-sparkStructuredStreaming/

kubectl apply -f theodolite-benchmark-kstreams.yaml
kubectl apply -f theodolite-benchmark-hzcast.yaml
kubectl apply -f theodolite-benchmark-flink.yaml
kubectl apply -f theodolite-benchmark-spark.yaml
```

## Run Theodolite Benchmarks

To execute the installed benchmarks, Theodolite `Execution`s have to be deployed.

*Coming soon...*

## Uninstall

To uninstall Theodolite, it is sufficient to run:

```sh
helm uninstall theodolite
```

This will also delete the EBS volumes created for Kafka.

To remove the EKS cluster, run (mind the `--disable-nodegroup-eviction` option!):

```sh
eksctl delete cluster -f cluster.yaml --disable-nodegroup-eviction
```
