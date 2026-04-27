FROM ubuntu:22.04

# Set environment variables for Spark and Hadoop versions
ENV SPARK_VERSION=4.1.1
ENV HADOOP_VERSION=3.4.2

# Set environment variables for non-interactive installation
ENV DEBIAN_FRONTEND=noninteractive

# Update and install required packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    wget \
    tar \
    bash \
    openjdk-21-jre-headless \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Verify Java installation
RUN java --version

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Install Hadoop
RUN mkdir -p /opt && \
    cd /opt && \
    curl -fSL --retry 5 --retry-delay 10 --retry-all-errors \
        https://dlcdn.apache.org/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}.tar.gz \
        -o hadoop.tar.gz && \
    tar -xzf hadoop.tar.gz && \
    rm hadoop.tar.gz && \
    ln -s hadoop-${HADOOP_VERSION} hadoop && \
    echo "Hadoop ${HADOOP_VERSION} installed in /opt"

# Set Hadoop native library path
ENV HADOOP_COMMON_LIB_NATIVE_DIR="/opt/hadoop/lib/native"

# Install Spark
RUN mkdir -p /opt && \
    cd /opt && \
    curl -fSL --retry 5 --retry-delay 10 --retry-all-errors \
        https://dlcdn.apache.org/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop3.tgz \
        -o spark.tgz && \
    tar -xzf spark.tgz && \
    rm spark.tgz && \
    ln -s spark-${SPARK_VERSION}-bin-hadoop3 spark && \
    echo "Spark ${SPARK_VERSION} installed in /opt"


# Copy application JAR and scripts
COPY build/libs/shuffle-spark-1.0-SNAPSHOT-all.jar /
COPY --chmod=0755 common.sh /common.sh
COPY --chmod=0755 spark-master /spark-master
COPY --chmod=0755 spark-worker /spark-worker

# Make scripts executable
RUN chmod +x spark-master spark-worker

# Add Spark configuration
ADD spark-defaults.conf /opt/spark/conf/spark-defaults.conf

# Update PATH to include Spark binaries
ENV PATH=$PATH:/opt/spark/bin

# Set the working directory
WORKDIR /

# Default command (optional)
CMD ["/spark-master"]

# Default to env-var config unless explicitly enabled.
# Set USE_MICROPROFILE_CONFIG=true only when a ConfigProviderResolver is on the classpath.
ENV USE_MICROPROFILE_CONFIG=false

# Build marker to verify deployed image (override at build time, e.g. --build-arg IMAGE_BUILD_ID=...)
ENV IMAGE_BUILD_ID=local-dev
