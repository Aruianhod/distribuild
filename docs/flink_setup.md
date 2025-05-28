# Flink分布式环境配置

## 1. 环境概述

本文档详细描述了分布式实时电商推荐系统中Flink分布式环境的配置方法。Flink作为高性能的分布式流处理框架，在本项目中负责实时处理用户行为数据并执行推荐算法，是整个系统的计算核心。

### 1.1 节点规划

根据项目要求，我们配置了1个JobManager和3个TaskManager的Flink集群：

| 节点名称 | 服务角色 | IP地址 |
|---------|---------|--------|
| Hadoop01 | JobManager + TaskManager | 192.168.1.101 |
| Hadoop02 | TaskManager | 192.168.1.102 |
| Hadoop03 | TaskManager | 192.168.1.103 |

### 1.2 硬件要求

每个节点的硬件配置如下：
- CPU: 4核或以上
- 内存: 16GB或以上
- 存储: 50GB可用空间
- 网络: 千兆以太网

### 1.3 软件要求

- 操作系统: Ubuntu 20.04 LTS 或 CentOS 7
- Java: OpenJDK 11
- Flink: 1.15.0
- Hadoop: 3.2.2 (可选，用于HDFS支持)

## 2. 安装步骤

### 2.1 准备工作

在所有节点上执行以下操作：

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装Java
sudo apt install -y openjdk-11-jdk

# 验证Java安装
java -version

# 创建Flink用户
sudo useradd -m -s /bin/bash flink

# 创建数据目录
sudo mkdir -p /data/flink
sudo chown -R flink:flink /data/flink
```

### 2.2 安装Flink

在所有节点上执行以下操作：

```bash
# 切换到flink用户
sudo su - flink

# 下载Flink
wget https://archive.apache.org/dist/flink/flink-1.15.0/flink-1.15.0-bin-scala_2.12.tgz

# 解压
tar -xzf flink-1.15.0-bin-scala_2.12.tgz
mv flink-1.15.0 flink

# 创建日志目录
mkdir -p ~/flink/log
```

### 2.3 配置Flink

#### 2.3.1 基本配置

在所有节点上编辑`~/flink/conf/flink-conf.yaml`：

```yaml
# JobManager配置
jobmanager.rpc.address: Hadoop01
jobmanager.rpc.port: 6123
jobmanager.memory.process.size: 4096m

# TaskManager配置
taskmanager.memory.process.size: 8192m
taskmanager.numberOfTaskSlots: 4

# 高可用性配置
high-availability: zookeeper
high-availability.zookeeper.quorum: Hadoop01:2181
high-availability.storageDir: hdfs:///flink/ha
high-availability.zookeeper.path.root: /flink

# Web UI配置
rest.port: 8081
rest.address: 0.0.0.0

# 检查点配置
state.backend: filesystem
state.checkpoints.dir: hdfs:///flink/checkpoints
state.savepoints.dir: hdfs:///flink/savepoints

# 日志配置
env.log.dir: /home/flink/flink/log

# 并行度配置
parallelism.default: 4

# 重启策略
restart-strategy: fixed-delay
restart-strategy.fixed-delay.attempts: 3
restart-strategy.fixed-delay.delay: 10s
```

#### 2.3.2 配置Masters文件

在所有节点上编辑`~/flink/conf/masters`：

```
Hadoop01:8081
```

#### 2.3.3 配置Workers文件

在所有节点上编辑`~/flink/conf/workers`：

```
Hadoop01
Hadoop02
Hadoop03
```

#### 2.3.4 配置Kafka连接器

下载Kafka连接器JAR文件并放入Flink的lib目录：

```bash
wget https://repo.maven.apache.org/maven2/org/apache/flink/flink-connector-kafka_2.12/1.15.0/flink-connector-kafka_2.12-1.15.0.jar -P ~/flink/lib/
```

## 3. 启动服务

### 3.1 启动Flink集群

在JobManager节点(Hadoop01)上执行：

```bash
# 切换到flink用户
sudo su - flink

# 启动Flink集群
~/flink/bin/start-cluster.sh

# 验证集群状态
~/flink/bin/flink list
```

### 3.2 验证Web UI

在浏览器中访问`http://Hadoop01:8081`，应该能看到Flink的Web UI界面，显示集群状态、已注册的TaskManager和任务列表。

## 4. 提交作业

### 4.1 提交JAR作业

```bash
~/flink/bin/flink run -c org.recommendation.RecommendationJob /path/to/recommendation-system.jar
```

### 4.2 使用SavePoint

```bash
# 从SavePoint恢复作业
~/flink/bin/flink run -s hdfs:///flink/savepoints/savepoint-xxx -c org.recommendation.RecommendationJob /path/to/recommendation-system.jar

# 触发SavePoint
~/flink/bin/flink savepoint <jobId> hdfs:///flink/savepoints/
```

### 4.3 取消作业

```bash
~/flink/bin/flink cancel <jobId>
```

## 5. 监控与管理

### 5.1 查看作业状态

```bash
~/flink/bin/flink list
```

### 5.2 查看作业详情

```bash
~/flink/bin/flink list -a
```

### 5.3 监控指标

Flink提供了多种监控指标，可以通过以下方式查看：

1. **Web UI**: 访问`http://Hadoop01:8081`
2. **REST API**: 访问`http://Hadoop01:8081/jobs`
3. **Metrics Reporter**: 配置Prometheus、InfluxDB等

## 6. 故障排除

### 6.1 常见问题

1. **JobManager无法启动**
   - 检查ZooKeeper是否正常运行
   - 检查端口是否被占用
   - 查看JobManager日志: `~/flink/log/flink-flink-jobmanager-*.log`

2. **TaskManager无法注册到JobManager**
   - 检查网络连接
   - 确保TaskManager配置了正确的JobManager地址
   - 查看TaskManager日志: `~/flink/log/flink-flink-taskmanager-*.log`

3. **作业执行失败**
   - 检查作业日志
   - 验证Kafka连接器配置
   - 确保有足够的资源（内存、槽位）

### 6.2 日志位置

- JobManager日志: `~/flink/log/flink-flink-jobmanager-*.log`
- TaskManager日志: `~/flink/log/flink-flink-taskmanager-*.log`
- Web UI日志: `~/flink/log/flink-flink-webmonitor-*.log`

## 7. Docker部署方案

除了手动安装，我们还提供了基于Docker的部署方案：

```yaml
version: '3'

services:
  jobmanager:
    image: flink:1.15.0
    container_name: jobmanager
    ports:
      - "8081:8081"
    command: jobmanager
    environment:
      - JOB_MANAGER_RPC_ADDRESS=jobmanager
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 4
        parallelism.default: 4
        state.backend: filesystem
        state.checkpoints.dir: file:///opt/flink/checkpoints
    volumes:
      - ./checkpoints:/opt/flink/checkpoints
    networks:
      - flink-network

  taskmanager1:
    image: flink:1.15.0
    container_name: taskmanager1
    depends_on:
      - jobmanager
    command: taskmanager
    environment:
      - JOB_MANAGER_RPC_ADDRESS=jobmanager
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 4
        parallelism.default: 4
        state.backend: filesystem
        state.checkpoints.dir: file:///opt/flink/checkpoints
    volumes:
      - ./checkpoints:/opt/flink/checkpoints
    networks:
      - flink-network

  taskmanager2:
    image: flink:1.15.0
    container_name: taskmanager2
    depends_on:
      - jobmanager
    command: taskmanager
    environment:
      - JOB_MANAGER_RPC_ADDRESS=jobmanager
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 4
        parallelism.default: 4
        state.backend: filesystem
        state.checkpoints.dir: file:///opt/flink/checkpoints
    volumes:
      - ./checkpoints:/opt/flink/checkpoints
    networks:
      - flink-network

  taskmanager3:
    image: flink:1.15.0
    container_name: taskmanager3
    depends_on:
      - jobmanager
    command: taskmanager
    environment:
      - JOB_MANAGER_RPC_ADDRESS=jobmanager
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 4
        parallelism.default: 4
        state.backend: filesystem
        state.checkpoints.dir: file:///opt/flink/checkpoints
    volumes:
      - ./checkpoints:/opt/flink/checkpoints
    networks:
      - flink-network

networks:
  flink-network:
    driver: bridge
```

使用以下命令启动Docker容器：

```bash
docker-compose -f flink-docker-compose.yml up -d
```

## 8. 性能调优

### 8.1 内存配置

```yaml
# JobManager内存配置
jobmanager.memory.process.size: 4096m
jobmanager.memory.heap.size: 3072m
jobmanager.memory.off-heap.size: 1024m

# TaskManager内存配置
taskmanager.memory.process.size: 8192m
taskmanager.memory.framework.heap.size: 512m
taskmanager.memory.task.heap.size: 4096m
taskmanager.memory.managed.size: 2048m
taskmanager.memory.network.min: 512m
taskmanager.memory.network.max: 1024m
```

### 8.2 并行度调优

```yaml
# 全局并行度
parallelism.default: 4

# 算子级并行度
env.parallelism.max: 8
```

### 8.3 网络配置

```yaml
# 网络缓冲区配置
taskmanager.network.memory.fraction: 0.1
taskmanager.network.memory.min: 64mb
taskmanager.network.memory.max: 1gb

# 网络请求超时
akka.ask.timeout: 100s
```

### 8.4 检查点配置

```yaml
# 检查点配置
execution.checkpointing.interval: 60000
execution.checkpointing.min-pause: 10000
execution.checkpointing.timeout: 600000
execution.checkpointing.max-concurrent-checkpoints: 1
```

## 9. 与Kafka集成

### 9.1 Kafka连接器配置

创建Kafka消费者：

```java
Properties properties = new Properties();
properties.setProperty("bootstrap.servers", "Hadoop01:9092,Hadoop02:9092,Hadoop03:9092");
properties.setProperty("group.id", "flink-consumer-group");
properties.setProperty("auto.offset.reset", "latest");
properties.setProperty("enable.auto.commit", "false");

FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(
    "user-behaviors",
    new SimpleStringSchema(),
    properties
);

// 设置消费起始位置
consumer.setStartFromLatest();

// 添加到数据流
DataStream<String> stream = env.addSource(consumer);
```

创建Kafka生产者：

```java
Properties properties = new Properties();
properties.setProperty("bootstrap.servers", "Hadoop01:9092,Hadoop02:9092,Hadoop03:9092");
properties.setProperty("transaction.timeout.ms", "5000");
properties.setProperty("acks", "all");

FlinkKafkaProducer<String> producer = new FlinkKafkaProducer<>(
    "recommendations",
    new SimpleStringSchema(),
    properties,
    FlinkKafkaProducer.Semantic.EXACTLY_ONCE
);

// 添加到数据流
stream.addSink(producer);
```

### 9.2 容错配置

```java
// 启用检查点
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
env.enableCheckpointing(60000); // 每60秒执行一次检查点
env.getCheckpointConfig().setCheckpointTimeout(600000); // 检查点超时时间
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(10000); // 检查点之间的最小间隔
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1); // 最大并发检查点数
env.getCheckpointConfig().setExternalizedCheckpointCleanup(
    CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
); // 作业取消时保留检查点
```

## 10. 总结

本文档详细介绍了Flink分布式环境的配置方法，包括安装步骤、配置详情、作业提交、监控管理、故障排除、Docker部署方案、性能调优和与Kafka的集成。通过按照本文档的指导，可以成功搭建一个高性能、高可用的Flink分布式集群，为分布式实时电商推荐系统提供强大的流处理能力。
