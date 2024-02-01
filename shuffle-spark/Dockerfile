ARG java_image_tag=11-jre

FROM openjdk:11

# define spark and hadoop versions
ENV SPARK_VERSION=3.4.1
ENV HADOOP_VERSION=3.3.1

# download and install hadoop
RUN mkdir -p /opt && \
    cd /opt && \
    curl http://archive.apache.org/dist/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}.tar.gz | \
        tar -zx hadoop-${HADOOP_VERSION}/lib/native && \
    ln -s hadoop-${HADOOP_VERSION} hadoop && \
    echo Hadoop ${HADOOP_VERSION} native libraries installed in /opt/hadoop/lib/native
RUN export HADOOP_COMMON_LIB_NATIVE_DIR="/opt/hadoop/lib/native"
# download and install spark
RUN mkdir -p /opt && \
    cd /opt && \
    curl http://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop3.tgz | \
        tar -zx && \
    ln -s spark-${SPARK_VERSION}-bin-hadoop3 spark && \
    echo Spark ${SPARK_VERSION} installed in /opt

COPY build/libs/shuffle-spark-1.0-SNAPSHOT.jar .

ADD common.sh spark-master spark-worker /
RUN cd /
RUN chmod +x spark-master
RUN chmod +x spark-worker
ADD spark-defaults.conf /opt/spark/conf/spark-defaults.conf
ENV PATH $PATH:/opt/spark/bin
