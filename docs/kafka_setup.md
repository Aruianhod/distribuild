# Kafka分布式环境配置

## 1. 环境概述

本文档详细描述了分布式实时电商推荐系统中Kafka分布式环境的配置方法。Kafka作为高吞吐量的分布式消息队列系统，在本项目中承担着数据传输的核心角色，确保用户行为数据、商品信息和推荐结果能够可靠地在各组件间流转。

### 1.1 节点规划

根据项目要求，我们配置了3个节点的Kafka集群：

| 节点名称 | 服务角色 | IP地址 |
|---------|---------|--------|
| Hadoop01 | ZooKeeper + Kafka Broker | 192.168.1.101 |
| Hadoop02 | Kafka Broker | 192.168.1.102 |
| Hadoop03 | Kafka Broker | 192.168.1.103 |

### 1.2 硬件要求

每个节点的硬件配置如下：
- CPU: 4核或以上
- 内存: 16GB或以上
- 存储: 50GB可用空间
- 网络: 千兆以太网

### 1.3 软件要求

- 操作系统: Ubuntu 20.04 LTS 或 CentOS 7
- Java: OpenJDK 11
- Kafka: 2.8.1
- ZooKeeper: 3.6.3 (内置于Kafka)

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

# 创建Kafka用户
sudo useradd -m -s /bin/bash kafka

# 创建数据目录
sudo mkdir -p /data/kafka
sudo mkdir -p /data/zookeeper
sudo chown -R kafka:kafka /data/kafka
sudo chown -R kafka:kafka /data/zookeeper
```

### 2.2 安装Kafka和ZooKeeper

在所有节点上执行以下操作：

```bash
# 切换到kafka用户
sudo su - kafka

# 下载Kafka
wget https://archive.apache.org/dist/kafka/2.8.1/kafka_2.13-2.8.1.tgz

# 解压
tar -xzf kafka_2.13-2.8.1.tgz
mv kafka_2.13-2.8.1 kafka

# 创建日志目录
mkdir -p ~/kafka/logs
```

### 2.3 配置ZooKeeper (仅在Hadoop01节点)

在Hadoop01节点上执行：

```bash
# 编辑ZooKeeper配置
cat > ~/kafka/config/zookeeper.properties << EOF
# 基本配置
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/data/zookeeper
clientPort=2181
maxClientCnxns=60

# 集群配置
server.1=Hadoop01:2888:3888
EOF
```

创建myid文件：

```bash
echo "1" > /data/zookeeper/myid
```

### 2.4 配置Kafka Broker

在所有节点上配置Kafka，但broker.id需要不同：

**Hadoop01节点**:

```bash
cat > ~/kafka/config/server.properties << EOF
# Broker基本配置
broker.id=1
listeners=PLAINTEXT://Hadoop01:9092
advertised.listeners=PLAINTEXT://Hadoop01:9092
num.network.threads=3
num.io.threads=8
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# 日志配置
log.dirs=/data/kafka
num.partitions=3
num.recovery.threads.per.data.dir=1
log.retention.hours=168
log.segment.bytes=1073741824
log.retention.check.interval.ms=300000

# ZooKeeper配置
zookeeper.connect=Hadoop01:2181
zookeeper.connection.timeout.ms=18000

# 复制配置
default.replication.factor=3
min.insync.replicas=2

# 其他配置
group.initial.rebalance.delay.ms=0
EOF
```

**Hadoop02节点**:

```bash
cat > ~/kafka/config/server.properties << EOF
# Broker基本配置
broker.id=2
listeners=PLAINTEXT://Hadoop02:9092
advertised.listeners=PLAINTEXT://Hadoop02:9092
# ... (其余配置与Hadoop01相同)
EOF
```

**Hadoop03节点**:

```bash
cat > ~/kafka/config/server.properties << EOF
# Broker基本配置
broker.id=3
listeners=PLAINTEXT://Hadoop03:9092
advertised.listeners=PLAINTEXT://Hadoop03:9092
# ... (其余配置与Hadoop01相同)
EOF
```

## 3. 启动服务

### 3.1 启动ZooKeeper (仅在Hadoop01节点)

```bash
# 切换到kafka用户
sudo su - kafka

# 启动ZooKeeper
~/kafka/bin/zookeeper-server-start.sh -daemon ~/kafka/config/zookeeper.properties

# 验证ZooKeeper是否启动
echo stat | nc localhost 2181
```

### 3.2 启动Kafka Broker (所有节点)

在每个节点上执行：

```bash
# 切换到kafka用户
sudo su - kafka

# 启动Kafka
~/kafka/bin/kafka-server-start.sh -daemon ~/kafka/config/server.properties

# 验证Kafka是否启动
~/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

## 4. 创建主题

在任一Kafka节点上执行：

```bash
# 创建用户行为主题
~/kafka/bin/kafka-topics.sh --create --bootstrap-server Hadoop01:9092 --replication-factor 3 --partitions 3 --topic user-behaviors

# 创建商品主题
~/kafka/bin/kafka-topics.sh --create --bootstrap-server Hadoop01:9092 --replication-factor 3 --partitions 3 --topic products

# 创建推荐结果主题
~/kafka/bin/kafka-topics.sh --create --bootstrap-server Hadoop01:9092 --replication-factor 3 --partitions 3 --topic recommendations

# 验证主题创建
~/kafka/bin/kafka-topics.sh --bootstrap-server Hadoop01:9092 --list
```

## 5. 验证集群

### 5.1 验证主题复制

```bash
~/kafka/bin/kafka-topics.sh --bootstrap-server Hadoop01:9092 --describe --topic user-behaviors
```

输出应显示3个分区，每个分区有3个副本。

### 5.2 测试消息发布与订阅

在一个终端中启动消费者：

```bash
~/kafka/bin/kafka-console-consumer.sh --bootstrap-server Hadoop01:9092,Hadoop02:9092,Hadoop03:9092 --topic user-behaviors --from-beginning
```

在另一个终端中启动生产者：

```bash
~/kafka/bin/kafka-console-producer.sh --bootstrap-server Hadoop01:9092,Hadoop02:9092,Hadoop03:9092 --topic user-behaviors
```

在生产者终端输入消息，应该能在消费者终端看到相同的消息。

## 6. 监控与管理

### 6.1 查看主题详情

```bash
~/kafka/bin/kafka-topics.sh --bootstrap-server Hadoop01:9092 --describe --topic user-behaviors
```

### 6.2 查看消费者组

```bash
~/kafka/bin/kafka-consumer-groups.sh --bootstrap-server Hadoop01:9092 --list
```

### 6.3 查看消费者组详情

```bash
~/kafka/bin/kafka-consumer-groups.sh --bootstrap-server Hadoop01:9092 --describe --group <group-id>
```

## 7. 故障排除

### 7.1 常见问题

1. **Kafka无法启动**
   - 检查ZooKeeper是否正常运行
   - 检查端口是否被占用
   - 查看Kafka日志: `~/kafka/logs/server.log`

2. **分区副本不同步**
   - 检查网络连接
   - 确保所有Broker都在运行
   - 使用以下命令检查未同步的分区:
     ```bash
     ~/kafka/bin/kafka-topics.sh --bootstrap-server Hadoop01:9092 --describe --under-replicated-partitions
     ```

3. **消息丢失**
   - 确保生产者配置了适当的acks值（建议acks=all）
   - 检查min.insync.replicas配置
   - 验证消费者的offset提交策略

### 7.2 日志位置

- ZooKeeper日志: `~/kafka/logs/zookeeper.out`
- Kafka日志: `~/kafka/logs/server.log`

## 8. Docker部署方案

除了手动安装，我们还提供了基于Docker的部署方案：

```yaml
version: '3'

services:
  zookeeper:
    image: wurstmeister/zookeeper
    container_name: zookeeper
    ports:
      - "2181:2181"
    networks:
      - kafka-network

  kafka1:
    image: wurstmeister/kafka
    container_name: kafka1
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_HOST_NAME: kafka1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_BROKER_ID: 1
      KAFKA_CREATE_TOPICS: "user-behaviors:3:1,products:3:1,recommendations:3:1"
    depends_on:
      - zookeeper
    networks:
      - kafka-network

  kafka2:
    image: wurstmeister/kafka
    container_name: kafka2
    ports:
      - "9093:9092"
    environment:
      KAFKA_ADVERTISED_HOST_NAME: kafka2
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_BROKER_ID: 2
    depends_on:
      - zookeeper
    networks:
      - kafka-network

  kafka3:
    image: wurstmeister/kafka
    container_name: kafka3
    ports:
      - "9094:9092"
    environment:
      KAFKA_ADVERTISED_HOST_NAME: kafka3
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_BROKER_ID: 3
    depends_on:
      - zookeeper
    networks:
      - kafka-network

networks:
  kafka-network:
    driver: bridge
```

使用以下命令启动Docker容器：

```bash
docker-compose -f kafka-docker-compose.yml up -d
```

## 9. 性能调优

### 9.1 Broker调优

```properties
# 增加网络线程数
num.network.threads=8

# 增加IO线程数
num.io.threads=16

# 增加发送缓冲区大小
socket.send.buffer.bytes=1048576

# 增加接收缓冲区大小
socket.receive.buffer.bytes=1048576

# 增加请求最大字节数
socket.request.max.bytes=104857600

# 优化日志刷盘策略
log.flush.interval.messages=10000
log.flush.interval.ms=1000
```

### 9.2 JVM调优

编辑`~/kafka/bin/kafka-server-start.sh`，修改JVM参数：

```bash
export KAFKA_HEAP_OPTS="-Xmx6G -Xms6G -XX:MetaspaceSize=96m -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:G1HeapRegionSize=16M -XX:MinMetaspaceFreeRatio=50 -XX:MaxMetaspaceFreeRatio=80"
```

## 10. 安全配置

### 10.1 启用SSL加密

生成密钥和证书：

```bash
# 创建CA
openssl req -new -x509 -keyout ca-key -out ca-cert -days 365

# 为每个Broker创建密钥对
keytool -keystore kafka.server.keystore.jks -alias localhost -validity 365 -genkey

# 导入CA证书
keytool -keystore kafka.server.truststore.jks -alias CARoot -import -file ca-cert
```

修改Broker配置：

```properties
# SSL配置
listeners=SSL://Hadoop01:9093
advertised.listeners=SSL://Hadoop01:9093
ssl.keystore.location=/path/to/kafka.server.keystore.jks
ssl.keystore.password=password
ssl.key.password=password
ssl.truststore.location=/path/to/kafka.server.truststore.jks
ssl.truststore.password=password
ssl.client.auth=required
security.inter.broker.protocol=SSL
```

### 10.2 启用SASL认证

修改Broker配置：

```properties
# SASL配置
listeners=SASL_SSL://Hadoop01:9094
advertised.listeners=SASL_SSL://Hadoop01:9094
security.inter.broker.protocol=SASL_SSL
sasl.mechanism.inter.broker.protocol=PLAIN
sasl.enabled.mechanisms=PLAIN
```

创建JAAS配置文件：

```
KafkaServer {
   org.apache.kafka.common.security.plain.PlainLoginModule required
   username="admin"
   password="admin-secret"
   user_admin="admin-secret"
   user_alice="alice-secret";
};
```

## 11. 总结

本文档详细介绍了Kafka分布式环境的配置方法，包括安装步骤、配置详情、主题创建、集群验证、监控管理、故障排除、Docker部署方案、性能调优和安全配置。通过按照本文档的指导，可以成功搭建一个高可用、高性能的Kafka分布式集群，为分布式实时电商推荐系统提供可靠的消息传输服务。
