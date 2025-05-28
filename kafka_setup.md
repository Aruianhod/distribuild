# Kafka分布式环境配置方案

## 1. 环境概述

根据需求，我们将在3个节点上配置Kafka分布式环境，节点分布如下：

| 节点名称 | 部署服务 |
|---------|---------|
| Hadoop01 | JobManager, TaskManager, Kafka Broker, ZooKeeper |
| Hadoop02 | TaskManager, Kafka Broker |
| Hadoop03 | TaskManager, Kafka Broker |

操作系统：Ubuntu 20.04 或 CentOS 7
硬件要求：
- CPU: 至少2核CPU，推荐4核以上
- 内存: 最少8GB RAM，推荐16GB及以上
- 存储: 至少50GB可用空间
- 网络: 多节点配置需要稳定的网络连接

软件版本：
- Apache Kafka: 3.6.0
- Apache Flink: 1.17
- ZooKeeper: 使用Kafka自带的ZooKeeper

## 2. 前置准备

### 2.1 配置主机名和IP映射

在所有节点上执行以下操作：

```bash
# 编辑hosts文件
sudo nano /etc/hosts
```

添加以下内容：

```
192.168.1.101 hadoop01
192.168.1.102 hadoop02
192.168.1.103 hadoop03
```

### 2.2 安装Java环境

Kafka和ZooKeeper都需要Java环境，在所有节点上执行：

```bash
# 安装OpenJDK 11
sudo apt update
sudo apt install -y openjdk-11-jdk

# 或者在CentOS上
# sudo yum install -y java-11-openjdk-devel

# 验证Java安装
java -version
```

### 2.3 创建用户和目录

在所有节点上创建kafka用户和相关目录：

```bash
# 创建kafka用户
sudo useradd -m -s /bin/bash kafka

# 创建数据和日志目录
sudo mkdir -p /opt/kafka /var/lib/kafka/data /var/log/kafka
sudo chown -R kafka:kafka /opt/kafka /var/lib/kafka /var/log/kafka
```

## 3. ZooKeeper配置（仅在Hadoop01节点）

### 3.1 下载和安装ZooKeeper

```bash
# 切换到kafka用户
sudo su - kafka

# 下载ZooKeeper
wget https://dlcdn.apache.org/zookeeper/zookeeper-3.8.0/apache-zookeeper-3.8.0-bin.tar.gz

# 解压
tar -xzf apache-zookeeper-3.8.0-bin.tar.gz -C /opt/kafka/

# 创建软链接
ln -s /opt/kafka/apache-zookeeper-3.8.0-bin /opt/kafka/zookeeper

# 创建数据目录
mkdir -p /var/lib/zookeeper/data
```

### 3.2 配置ZooKeeper

```bash
# 创建配置文件
cp /opt/kafka/zookeeper/conf/zoo_sample.cfg /opt/kafka/zookeeper/conf/zoo.cfg

# 编辑配置文件
nano /opt/kafka/zookeeper/conf/zoo.cfg
```

修改以下配置：

```properties
# 数据目录
dataDir=/var/lib/zookeeper/data
# 客户端连接端口
clientPort=2181
# 集群配置（单节点模式）
#server.1=hadoop01:2888:3888
```

### 3.3 创建ZooKeeper服务

创建systemd服务文件：

```bash
# 退出kafka用户
exit

# 创建服务文件
sudo nano /etc/systemd/system/zookeeper.service
```

添加以下内容：

```
[Unit]
Description=Apache ZooKeeper server
Documentation=http://zookeeper.apache.org
Requires=network.target remote-fs.target
After=network.target remote-fs.target

[Service]
Type=simple
User=kafka
ExecStart=/opt/kafka/zookeeper/bin/zkServer.sh start-foreground
ExecStop=/opt/kafka/zookeeper/bin/zkServer.sh stop
Restart=on-abnormal

[Install]
WantedBy=multi-user.target
```

启动ZooKeeper服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable zookeeper
sudo systemctl start zookeeper
sudo systemctl status zookeeper
```

## 4. Kafka配置（所有节点）

### 4.1 下载和安装Kafka

在所有节点上执行：

```bash
# 切换到kafka用户
sudo su - kafka

# 下载Kafka
wget https://dlcdn.apache.org/kafka/3.6.0/kafka_2.13-3.6.0.tgz

# 解压
tar -xzf kafka_2.13-3.6.0.tgz -C /opt/kafka/

# 创建软链接
ln -s /opt/kafka/kafka_2.13-3.6.0 /opt/kafka/kafka

# 创建日志目录
mkdir -p /var/log/kafka
```

### 4.2 配置Kafka Broker

在所有节点上创建Kafka配置文件：

```bash
# 编辑配置文件
nano /opt/kafka/kafka/config/server.properties
```

对于Hadoop01节点，修改以下配置：

```properties
# Broker ID（每个节点必须唯一）
broker.id=1
# 监听地址
listeners=PLAINTEXT://hadoop01:9092
# 发布地址
advertised.listeners=PLAINTEXT://hadoop01:9092
# 日志目录
log.dirs=/var/lib/kafka/data
# ZooKeeper连接
zookeeper.connect=hadoop01:2181
# 默认分区数
num.partitions=3
# 默认副本因子
default.replication.factor=3
# 允许自动创建主题
auto.create.topics.enable=true
```

对于Hadoop02节点，修改以下配置：

```properties
# Broker ID（每个节点必须唯一）
broker.id=2
# 监听地址
listeners=PLAINTEXT://hadoop02:9092
# 发布地址
advertised.listeners=PLAINTEXT://hadoop02:9092
# 日志目录
log.dirs=/var/lib/kafka/data
# ZooKeeper连接
zookeeper.connect=hadoop01:2181
# 默认分区数
num.partitions=3
# 默认副本因子
default.replication.factor=3
# 允许自动创建主题
auto.create.topics.enable=true
```

对于Hadoop03节点，修改以下配置：

```properties
# Broker ID（每个节点必须唯一）
broker.id=3
# 监听地址
listeners=PLAINTEXT://hadoop03:9092
# 发布地址
advertised.listeners=PLAINTEXT://hadoop03:9092
# 日志目录
log.dirs=/var/lib/kafka/data
# ZooKeeper连接
zookeeper.connect=hadoop01:2181
# 默认分区数
num.partitions=3
# 默认副本因子
default.replication.factor=3
# 允许自动创建主题
auto.create.topics.enable=true
```

### 4.3 创建Kafka服务

在所有节点上创建systemd服务文件：

```bash
# 退出kafka用户
exit

# 创建服务文件
sudo nano /etc/systemd/system/kafka.service
```

添加以下内容：

```
[Unit]
Description=Apache Kafka Server
Documentation=http://kafka.apache.org/documentation.html
Requires=network.target remote-fs.target
After=network.target remote-fs.target zookeeper.service

[Service]
Type=simple
User=kafka
ExecStart=/opt/kafka/kafka/bin/kafka-server-start.sh /opt/kafka/kafka/config/server.properties
ExecStop=/opt/kafka/kafka/bin/kafka-server-stop.sh
Restart=on-abnormal

[Install]
WantedBy=multi-user.target
```

启动Kafka服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable kafka
sudo systemctl start kafka
sudo systemctl status kafka
```

## 5. 创建Kafka主题

在Hadoop01节点上创建用于传递用户行为数据和推荐结果的Kafka主题：

```bash
# 切换到kafka用户
sudo su - kafka

# 创建用户行为数据主题
/opt/kafka/kafka/bin/kafka-topics.sh --create --topic user-behavior --bootstrap-server hadoop01:9092,hadoop02:9092,hadoop03:9092 --partitions 3 --replication-factor 3

# 创建推荐结果主题
/opt/kafka/kafka/bin/kafka-topics.sh --create --topic recommendation-results --bootstrap-server hadoop01:9092,hadoop02:9092,hadoop03:9092 --partitions 3 --replication-factor 3

# 查看主题列表
/opt/kafka/kafka/bin/kafka-topics.sh --list --bootstrap-server hadoop01:9092

# 查看主题详情
/opt/kafka/kafka/bin/kafka-topics.sh --describe --topic user-behavior --bootstrap-server hadoop01:9092
/opt/kafka/kafka/bin/kafka-topics.sh --describe --topic recommendation-results --bootstrap-server hadoop01:9092
```

## 6. 测试Kafka消息发布与订阅

### 6.1 测试消息发布

在Hadoop01节点上发布测试消息：

```bash
# 启动生产者控制台
/opt/kafka/kafka/bin/kafka-console-producer.sh --topic user-behavior --bootstrap-server hadoop01:9092

# 输入测试消息（JSON格式）
{"userId": "user1", "itemId": "item1", "action": "click", "timestamp": 1621234567890}
{"userId": "user2", "itemId": "item2", "action": "purchase", "timestamp": 1621234567891}
```

### 6.2 测试消息订阅

在Hadoop02或Hadoop03节点上订阅消息：

```bash
# 启动消费者控制台
/opt/kafka/kafka/bin/kafka-console-consumer.sh --topic user-behavior --from-beginning --bootstrap-server hadoop01:9092
```

## 7. 验证Kafka集群状态

在任意节点上检查Kafka集群状态：

```bash
# 查看Broker列表
/opt/kafka/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server hadoop01:9092

# 查看消费者组
/opt/kafka/kafka/bin/kafka-consumer-groups.sh --list --bootstrap-server hadoop01:9092

# 查看主题分区和副本状态
/opt/kafka/kafka/bin/kafka-topics.sh --describe --bootstrap-server hadoop01:9092
```

## 8. 故障排除

### 8.1 检查日志

```bash
# 查看ZooKeeper日志
sudo tail -f /var/log/zookeeper/zookeeper.log

# 查看Kafka日志
sudo tail -f /var/log/kafka/server.log
```

### 8.2 常见问题解决

1. 连接被拒绝：检查防火墙设置，确保端口开放
   ```bash
   sudo ufw status
   sudo ufw allow 2181/tcp  # ZooKeeper端口
   sudo ufw allow 9092/tcp  # Kafka端口
   ```

2. 主题创建失败：检查ZooKeeper连接和权限
   ```bash
   # 测试ZooKeeper连接
   echo stat | nc hadoop01 2181
   ```

3. 副本同步问题：检查网络连接和磁盘空间
   ```bash
   # 检查磁盘空间
   df -h
   ```

## 9. 性能调优

根据实际负载情况，可以调整以下参数：

```properties
# 增加处理线程数
num.network.threads=3
num.io.threads=8

# 调整Socket缓冲区大小
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400

# 调整请求最大大小
message.max.bytes=1000000

# 调整日志刷盘策略
log.flush.interval.messages=10000
log.flush.interval.ms=1000
```

## 10. 安全配置（可选）

如需启用安全认证，可以配置以下内容：

```properties
# 启用SASL认证
listeners=SASL_PLAINTEXT://hadoop01:9092
advertised.listeners=SASL_PLAINTEXT://hadoop01:9092
security.inter.broker.protocol=SASL_PLAINTEXT
sasl.mechanism.inter.broker.protocol=PLAIN
sasl.enabled.mechanisms=PLAIN

# 配置JAAS
listeners.name.sasl_plaintext.plain.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  username="admin" \
  password="admin-secret" \
  user_admin="admin-secret";
```

## 11. 监控配置（可选）

可以使用JMX和Prometheus监控Kafka集群：

```properties
# 启用JMX
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
```

## 12. 总结

完成上述步骤后，我们已经成功配置了一个具有3个节点的Kafka分布式环境，包括：
- 在Hadoop01节点上配置ZooKeeper服务
- 在所有节点上配置Kafka Broker服务
- 创建用于传递用户行为数据和推荐结果的Kafka主题
- 测试Kafka消息发布与订阅功能
- 验证Kafka集群状态

这个Kafka分布式环境将作为我们实时电商推荐系统的消息队列基础设施，用于传递用户行为数据和推荐结果。
