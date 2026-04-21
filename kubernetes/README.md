# Running ShuffleBench in Kubernetes

This directory contains everything needed to run ShuffleBench on Kubernetes. It covers:

- **Infrastructure**: Amazon EKS cluster definition and AWS-specific storage configuration
- **Manifests**: Kubernetes manifests for all four stream processing frameworks (Apache Flink, Apache Spark Structured Streaming, Apache Kafka Streams, Hazelcast Jet), the load generator, and the latency exporter
- **Helm values**: Configuration for the [Theodolite](https://www.theodolite.rocks/) benchmarking framework (including Kafka and Prometheus)
- **Theodolite benchmark definitions**: Benchmark CRDs for automated, reproducible experiments with all four frameworks

---

## Directory Layout

```
kubernetes/
├── README.md                              # This file
├── cluster.yaml                           # EKS cluster definition (eksctl)
├── values.yaml                            # Base Theodolite Helm values (Kafka, Prometheus)
├── values-nodegroups.yaml                 # Node affinity / topology scheduling
├── values-aws-kafka-storage.yaml          # Persistent EBS volumes for Kafka
├── theodolite-benchmark-kstreams.yaml     # Theodolite Benchmark CRD for Kafka Streams
├── theodolite-benchmark-hzcast.yaml       # Theodolite Benchmark CRD for Hazelcast
├── theodolite-benchmark-flink.yaml        # Theodolite Benchmark CRD for Flink
├── theodolite-benchmark-spark.yaml        # Theodolite Benchmark CRD for Spark
├── aws/
│   └── kafka-storage-class.yaml           # AWS EBS storage class for Kafka
├── shuffle-kstreams/                      # Kafka Streams SUT manifests
├── shuffle-hzcast/                        # Hazelcast SUT manifests
├── shuffle-flink/                         # Flink SUT manifests (JobManager + TaskManager)
├── shuffle-sparkStructuredStreaming/       # Spark SUT manifests (Master + Worker)
├── shuffle-load-generator/                # Load generator StatefulSet
└── shuffle-latency-exporter/              # Latency measurement exporter
```

---

## Prerequisites

| Tool | Purpose |
|------|---------|
| [`kubectl`](https://kubernetes.io/docs/tasks/tools/) | Interact with the Kubernetes cluster |
| [`helm`](https://helm.sh/docs/intro/install/) | Install Theodolite (which deploys Kafka, Prometheus, and the benchmark operator) |
| [`eksctl`](https://eksctl.io/) | Create and manage the EKS cluster (only for AWS Option 1) |

You also need:
- A Kubernetes cluster (see options below) with `kubectl` configured to access it

---

## 1 — Set Up a Kubernetes Cluster

ShuffleBench can run on any Kubernetes cluster. We describe three options ranging from a full AWS production setup to a lightweight local cluster.

### Option 1: 10-Node AWS Cluster (Exact Setup as in ICPE'24 Paper)

The `cluster.yaml` file defines an [Amazon EKS](https://aws.amazon.com/eks/) cluster with three node groups:

| Node Group | Instance Type | Count | Purpose |
|------------|--------------|-------|---------|
| `infra` | m6i.xlarge | 4 | Monitoring infrastructure |
| `sut` | m6i.xlarge | 3 | Stream processing applications (system under test) |
| `kafka` | m6i.2xlarge | 3 | Kafka brokers |

> **Cost warning**: Both the EKS cluster and the EBS volumes are rather heavy-sized and might cause high costs when run for a longer time.

You may want to adjust the cluster name in `cluster.yaml` before creating the cluster:

```sh
eksctl create cluster -f cluster.yaml
```

Cluster creation takes about 20 minutes. Once it is ready, create the `kafka` storage class so that Kafka can use persistent [Amazon EBS](https://aws.amazon.com/ebs/) volumes:

```sh
kubectl apply -f aws/kafka-storage-class.yaml
```

### Option 2: Vendor-Neutral Cluster with Node Groups

This option works with any Kubernetes cluster (cloud or on-premises) where nodes are labeled with the following roles:

| Label | Purpose |
|-------|---------|
| `type=infra` | Monitoring and infrastructure components |
| `type=kafka` | Kafka brokers |
| `type=sut` | Stream processing applications |

Nodes can be labeled with:

```sh
kubectl label node <NODE-NAME> type=<TYPE>
```

Or use the labeling features of your cloud provider. The Helm values in `values-nodegroups.yaml` use these labels to schedule pods onto the appropriate nodes.

If you do not have sufficiently fast disks available, you could add more Kafka nodes to the cluster. Make sure to label them with `type=kafka` and set `strimzi.kafka.replicas` accordingly when installing the Helm chart.

### Option 3: Lightweight Local Cluster

For quick experiments or development, a local cluster works well. We successfully ran ShuffleBench on an Ubuntu 22.04 laptop with 32 GB memory and a 14-core CPU. A simple cluster can be created with [k3d](https://k3d.io/):

```sh
k3d cluster create
```

Alternatively, [Docker Desktop](https://docs.docker.com/desktop/) with Kubernetes enabled also works.

> **Note**: Results on a local cluster will differ from a distributed production setup. For meaningful benchmarks, use a multi-node cluster.

---

## 2 — Install Theodolite

[Theodolite](https://www.theodolite.rocks/) is a benchmarking framework for cloud-native applications in Kubernetes. ShuffleBench uses Theodolite for two purposes:

1. **Infrastructure setup**: Theodolite's Helm chart deploys and configures Apache Kafka (via [Strimzi](https://strimzi.io/)), Prometheus, and related monitoring components.
2. **Benchmark automation**: Theodolite provides [Kubernetes CRDs](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/) (`Benchmark` and `Execution`) that automate the full benchmark lifecycle: deploying the system under test, starting the load generator, collecting performance metrics from Prometheus, evaluating service-level objectives (SLOs), and storing the results.

Choose the installation option matching your cluster setup from [Section 1](#1--set-up-a-kubernetes-cluster).

### Option 1: AWS Setup with Node Groups and Persistent Storage

```sh
helm repo add theodolite https://www.theodolite.rocks
helm repo update
helm install theodolite theodolite/theodolite \
  -f https://raw.githubusercontent.com/cau-se/theodolite/main/helm/preconfigs/extended-metrics.yaml \
  -f values.yaml \
  -f values-nodegroups.yaml \
  -f values-aws-kafka-storage.yaml
```

### Option 2: Vendor-Neutral Setup with Node Groups (No AWS-Specific Storage)

```sh
helm repo add theodolite https://www.theodolite.rocks
helm repo update
helm install theodolite theodolite/theodolite \
  -f https://raw.githubusercontent.com/cau-se/theodolite/main/helm/preconfigs/extended-metrics.yaml \
  -f values.yaml \
  -f values-nodegroups.yaml
```

### Option 3: Lightweight Setup (Minimal Resources, No Node Groups)

```sh
helm repo add theodolite https://www.theodolite.rocks
helm repo update
helm install theodolite theodolite/theodolite \
  -f https://raw.githubusercontent.com/cau-se/theodolite/main/helm/preconfigs/minimal.yaml \
  -f values.yaml
```

### Verify Installation

The installation may take a few minutes. Before continuing, make sure all pods are up and running:

```sh
kubectl get pods -w
```

In particular, wait for the Kafka cluster to be ready:

```sh
kubectl wait kafka/theodolite-kafka --for=condition=Ready --timeout=5m
```

---

## 3 — Manual Deployment (Without Theodolite Automation)

Use this approach to run a specific ShuffleBench configuration manually without Theodolite's benchmark automation. This is useful for debugging, exploring configurations, or quick one-off tests. Note that Theodolite still needs to be installed (see [Section 2](#2--install-theodolite)) since it provides the Kafka and Prometheus infrastructure.

### 3.1 Deploy a Stream Processing Framework

Each framework has its own directory with Kubernetes manifests. Deploy one of the following:

```sh
# Kafka Streams
kubectl apply -f shuffle-kstreams/

# Hazelcast Jet
kubectl apply -f shuffle-hzcast/

# Apache Flink
kubectl apply -f shuffle-flink/

# Apache Spark Structured Streaming
kubectl apply -f shuffle-sparkStructuredStreaming/
```

### 3.2 Deploy the Latency Exporter (Optional)

If you want to measure end-to-end latency, deploy the latency exporter **before** starting the load generator:

```sh
kubectl apply -f shuffle-latency-exporter/
```

### 3.3 Deploy the Load Generator

```sh
kubectl apply -f shuffle-load-generator/
```

### 3.4 Adjust Configurations

You will likely want to adjust the manifests before deploying.

> **Tip**: The manifests reference pre-built images from `ghcr.io/dynatrace-research/shufflebench/`. If you want to benchmark a custom build, update the `image` fields in the deployment manifests to point to your own registry. See the main [README](../README.md) for build and publish instructions.

Key configuration parameters (set via environment variables in the deployment manifests) include:

| Parameter | Manifest | Default | Description |
|-----------|----------|---------|-------------|
| `NUM_RECORDS_PER_SOURCE_SECOND` | load generator | `250000` | Records generated per second per load generator pod |
| `RECORD_SIZE_BYTES` | load generator | `1024` | Size of each generated record in bytes |
| `replicas` | load generator | `4` | Number of load generator pods |
| `replicas` | SUT deployment | `9` | Number of stream processor instances |
| `MATCHER_ZIPF_NUM_RULES` | SUT deployment | `1000000` | Number of matching rules (shuffle fan-out) |
| `MATCHER_ZIPF_TOTAL_SELECTIVITY` | SUT deployment | `0.2` | Fraction of rules that match per record |

Framework-specific parameters (e.g., `KAFKASTREAMS__COMMIT_INTERVAL_MS__`, `FLINK_PARALLELISM`, `SPARK_MAX_OFFSETS_PER_TRIGGER`) are documented in the respective deployment manifests.

### 3.5 Monitor

Port-forward to Prometheus to inspect metrics:

```sh
kubectl port-forward svc/prometheus-operated 9090:9090
# Open http://localhost:9090 and query, e.g.:
#   sum(rate(kafka_consumergroup_current_offset{topic='input'}[10s]))   (throughput)
#   histogram_quantile(0.99, sum(rate(shufflebench_latency_seconds_bucket[20s])) by (le))   (p99 latency)
```

### 3.6 Tear Down

Remove the components in reverse order, allowing the load generator to drain before removing the SUT:

```sh
kubectl delete -f shuffle-load-generator/
# Wait ~30 seconds for the load generator to stop
kubectl delete -f shuffle-kstreams/          # or whichever framework you deployed
kubectl delete -f shuffle-latency-exporter/  # if deployed
```

---

## 4 — Automated Benchmarks with Theodolite

For systematic experiments across different configurations, use Theodolite's benchmark automation. Theodolite deploys the system under test and load generator, runs experiments for a specified duration with multiple repetitions, collects metrics from Prometheus, evaluates SLOs, and stores the results — all automatically.

### 4.1 Install Benchmark Definitions

Theodolite reads the SUT and load generator manifests from Kubernetes ConfigMaps. Create them from the manifest directories and apply the Benchmark CRDs:

```sh
# (Re-)create ConfigMaps — run this whenever manifests change
kubectl delete configmaps --ignore-not-found=true \
  shufflebench-resources-load-generator \
  shufflebench-resources-latency-exporter \
  shufflebench-resources-kstreams \
  shufflebench-resources-hzcast \
  shufflebench-resources-flink \
  shufflebench-resources-spark

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

Verify that all benchmarks are registered:

```sh
kubectl get benchmarks
```

### 4.2 Understanding the Benchmark Definitions

Each `theodolite-benchmark-*.yaml` defines how Theodolite runs experiments for a given framework:

| Framework | Benchmark Name | Parallelism Mechanism | Load Parameter |
|-----------|---------------|----------------------|----------------|
| Kafka Streams | `shuffle-kstreams` | Deployment replicas | `NUM_RECORDS_PER_SOURCE_SECOND` |
| Hazelcast Jet | `shuffle-hzcast` | Deployment replicas | `NUM_RECORDS_PER_SOURCE_SECOND` |
| Apache Flink | `shuffle-flink` | TaskManager replicas + `FLINK_PARALLELISM` env var | `NUM_RECORDS_PER_SOURCE_SECOND` |
| Spark Structured Streaming | `shuffle-spark` | Worker replicas | `NUM_RECORDS_PER_SOURCE_SECOND` (+ `SPARK_MAX_OFFSETS_PER_TRIGGER`) |

**Collected metrics (SLOs)** — Each benchmark collects the following metrics via Prometheus:
- **Lag trend**: Whether consumer lag is increasing (indicates the SUT cannot keep up)
- **Throughput**: Message consumption rate from Kafka consumer group offsets
- **Latency percentiles**: End-to-end latency at p05, p10, p15, ..., p95, p99, p100 (computed over 20-second sliding windows as well as over the entire experiment duration)

All metrics use a 120-second warmup period to exclude startup effects.

### 4.3 Run Benchmark Executions

To run an experiment, create a Theodolite `Execution` resource. An Execution specifies which benchmark to run, the load intensity, the number of instances, the experiment duration, and the number of repetitions.

Example Execution YAML (ad-hoc throughput test for Kafka Streams):

```yaml
apiVersion: theodolite.rocks/v1beta1
kind: execution
metadata:
  name: kstreams-example
spec:
  benchmark: shuffle-kstreams
  load:
    loadType: "MessagesPerSecond"
    loadValues: [250000]
  resources:
    resourceType: "Instances"
    resourceValues: [9]
  execution:
    duration: 900 # 15 minutes per repetition
    repetitions: 3
    strategy:
      name: "LinearSearch"
      restrictions: []
    metric: "capacity"
  slos:
    - name: "throughput"
  configOverrides:
    - patcher:
        type: "EnvVarPatcher"
        resource: "shuffle-kstreams-deployment.yaml"
        properties:
          container: "shuffle-kstreams"
          variableName: "MATCHER_ZIPF_NUM_RULES"
      value: "1000000"
```

Apply it to start the experiment:

```sh
kubectl apply -f <your-execution-file>.yaml
```

Watch the execution progress:

```sh
kubectl get executions -w
```

Theodolite automatically queues multiple Execution resources and runs them one at a time.

### 4.4 Retrieve Results

Theodolite stores experiment results in a persistent volume inside its operator pod. Copy them to your local machine:

```sh
mkdir -p results
kubectl cp \
  $(kubectl get pod -l app=theodolite -o jsonpath="{.items[0].metadata.name}"):results \
  ./results \
  -c results-access
```

Results are organized by execution name and contain per-repetition metric data in CSV format (throughput, latency percentiles, lag trend).

---

## 5 — Uninstall

### Remove Theodolite (and Kafka, Prometheus)

```sh
helm uninstall theodolite
```

This also deletes the EBS volumes created for Kafka (if using AWS persistent storage).

### Remove the EKS Cluster (AWS Only)

```sh
eksctl delete cluster -f cluster.yaml --disable-nodegroup-eviction
```

> **Note**: The `--disable-nodegroup-eviction` flag is required to avoid the deletion getting stuck on pod disruption budgets.
