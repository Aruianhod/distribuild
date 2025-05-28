# Flink分布式环境配置方案

## 1. 环境概述

根据需求，我们将在3个节点上配置Flink分布式环境，节点分布如下：

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
- Apache Flink: 1.17
- Apache Kafka: 3.6.0（已在前一步骤中配置）

## 2. 前置准备

### 2.1 确认Java环境

Flink需要Java环境，在所有节点上确认Java已安装：

```bash
# 验证Java安装
java -version

# 如果未安装，则安装OpenJDK 11
sudo apt update
sudo apt install -y openjdk-11-jdk

# 或者在CentOS上
# sudo yum install -y java-11-openjdk-devel
```

### 2.2 创建用户和目录

在所有节点上创建flink用户和相关目录：

```bash
# 创建flink用户
sudo useradd -m -s /bin/bash flink

# 创建数据和日志目录
sudo mkdir -p /opt/flink /var/lib/flink/data /var/log/flink
sudo chown -R flink:flink /opt/flink /var/lib/flink /var/log/flink
```

## 3. 下载和安装Flink

在所有节点上执行以下操作：

```bash
# 切换到flink用户
sudo su - flink

# 下载Flink
wget https://dlcdn.apache.org/flink/flink-1.17.0/flink-1.17.0-bin-scala_2.12.tgz

# 解压
tar -xzf flink-1.17.0-bin-scala_2.12.tgz -C /opt/flink/

# 创建软链接
ln -s /opt/flink/flink-1.17.0 /opt/flink/current

# 创建日志目录
mkdir -p /var/log/flink
```

## 4. 配置Flink集群

### 4.1 配置Flink主配置文件

在所有节点上编辑Flink配置文件：

```bash
# 编辑配置文件
nano /opt/flink/current/conf/flink-conf.yaml
```

对于Hadoop01节点（JobManager），修改以下配置：

```yaml
# JobManager配置
jobmanager.rpc.address: hadoop01
jobmanager.rpc.port: 6123
jobmanager.memory.process.size: 1600m
jobmanager.web.address: 0.0.0.0
jobmanager.web.port: 8081

# TaskManager配置
taskmanager.memory.process.size: 1728m
taskmanager.numberOfTaskSlots: 2

# 高可用性配置（可选）
# high-availability: zookeeper
# high-availability.zookeeper.quorum: hadoop01:2181
# high-availability.zookeeper.path.root: /flink
# high-availability.cluster-id: /cluster_one

# 检查点和状态后端配置
state.backend: filesystem
state.checkpoints.dir: file:///var/lib/flink/data/checkpoints
state.savepoints.dir: file:///var/lib/flink/data/savepoints

# 日志配置
env.log.dir: /var/log/flink

# REST端点配置
rest.port: 8081
rest.address: 0.0.0.0

# 历史服务器配置
jobmanager.archive.fs.dir: file:///var/lib/flink/data/completed-jobs/
historyserver.web.address: 0.0.0.0
historyserver.web.port: 8082
historyserver.archive.fs.dir: file:///var/lib/flink/data/completed-jobs/
```

对于Hadoop02和Hadoop03节点（TaskManager），修改以下配置：

```yaml
# JobManager配置
jobmanager.rpc.address: hadoop01
jobmanager.rpc.port: 6123
jobmanager.memory.process.size: 1600m

# TaskManager配置
taskmanager.memory.process.size: 1728m
taskmanager.numberOfTaskSlots: 4

# 状态后端配置
state.backend: filesystem
state.checkpoints.dir: file:///var/lib/flink/data/checkpoints
state.savepoints.dir: file:///var/lib/flink/data/savepoints

# 日志配置
env.log.dir: /var/log/flink
```

### 4.2 配置Masters和Workers文件

在所有节点上配置masters文件：

```bash
# 编辑masters文件
nano /opt/flink/current/conf/masters
```

添加以下内容：

```
hadoop01:8081
```

在所有节点上配置workers文件：

```bash
# 编辑workers文件
nano /opt/flink/current/conf/workers
```

添加以下内容：

```
hadoop01
hadoop02
hadoop03
```

### 4.3 配置环境变量

在所有节点上配置Flink环境变量：

```bash
# 编辑.bashrc文件
nano ~/.bashrc
```

添加以下内容：

```bash
# Flink环境变量
export FLINK_HOME=/opt/flink/current
export PATH=$PATH:$FLINK_HOME/bin
```

应用环境变量：

```bash
source ~/.bashrc
```

## 5. 配置Flink与Kafka的连接器

### 5.1 下载Kafka连接器

在所有节点上下载Flink Kafka连接器：

```bash
# 下载Flink Kafka连接器
wget https://repo.maven.apache.org/maven2/org/apache/flink/flink-connector-kafka/1.17.0/flink-connector-kafka-1.17.0.jar -P /opt/flink/current/lib/

# 下载Flink JSON连接器（用于处理JSON格式数据）
wget https://repo.maven.apache.org/maven2/org/apache/flink/flink-json/1.17.0/flink-json-1.17.0.jar -P /opt/flink/current/lib/
```

## 6. 创建Flink服务

### 6.1 创建JobManager服务（仅在Hadoop01节点）

```bash
# 退出flink用户
exit

# 创建服务文件
sudo nano /etc/systemd/system/flink-jobmanager.service
```

添加以下内容：

```
[Unit]
Description=Apache Flink JobManager
After=network.target

[Service]
Type=forking
User=flink
ExecStart=/opt/flink/current/bin/jobmanager.sh start
ExecStop=/opt/flink/current/bin/jobmanager.sh stop
Restart=on-abnormal

[Install]
WantedBy=multi-user.target
```

### 6.2 创建TaskManager服务（所有节点）

```bash
# 创建服务文件
sudo nano /etc/systemd/system/flink-taskmanager.service
```

添加以下内容：

```
[Unit]
Description=Apache Flink TaskManager
After=network.target

[Service]
Type=forking
User=flink
ExecStart=/opt/flink/current/bin/taskmanager.sh start
ExecStop=/opt/flink/current/bin/taskmanager.sh stop
Restart=on-abnormal

[Install]
WantedBy=multi-user.target
```

### 6.3 启动Flink服务

在Hadoop01节点上启动JobManager服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable flink-jobmanager
sudo systemctl start flink-jobmanager
sudo systemctl status flink-jobmanager
```

在所有节点上启动TaskManager服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable flink-taskmanager
sudo systemctl start flink-taskmanager
sudo systemctl status flink-taskmanager
```

## 7. 验证Flink集群状态

### 7.1 通过Web UI验证

在浏览器中访问Flink Web UI：`http://hadoop01:8081`

### 7.2 通过命令行验证

```bash
# 切换到flink用户
sudo su - flink

# 查看集群状态
/opt/flink/current/bin/flink list
```

## 8. 测试Flink与Kafka的连接

### 8.1 创建测试作业

创建一个简单的Flink作业，用于从Kafka读取数据并写入Kafka：

```bash
# 创建测试目录
mkdir -p ~/flink-kafka-test
cd ~/flink-kafka-test

# 创建Maven项目结构
mkdir -p src/main/java/com/example/flink
```

创建pom.xml文件：

```bash
nano pom.xml
```

添加以下内容：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>flink-kafka-test</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <flink.version>1.17.0</flink.version>
        <java.version>11</java.version>
        <scala.binary.version>2.12</scala.binary.version>
        <kafka.version>3.6.0</kafka.version>
    </properties>

    <dependencies>
        <!-- Flink Core -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-streaming-java</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-clients</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Flink Kafka Connector -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-kafka</artifactId>
            <version>${flink.version}</version>
        </dependency>

        <!-- Flink JSON -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-json</artifactId>
            <version>${flink.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.4</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.example.flink.KafkaStreamingJob</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

创建Flink作业类：

```bash
nano src/main/java/com/example/flink/KafkaStreamingJob.java
```

添加以下内容：

```java
package com.example.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class KafkaStreamingJob {
    public static void main(String[] args) throws Exception {
        // 设置执行环境
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 配置Kafka源
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("hadoop01:9092,hadoop02:9092,hadoop03:9092")
                .setTopics("user-behavior")
                .setGroupId("flink-test-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 配置Kafka接收器
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("hadoop01:9092,hadoop02:9092,hadoop03:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("recommendation-results")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build())
                .build();

        // 创建数据流
        DataStream<String> stream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 简单处理：添加时间戳和标记
        DataStream<String> processedStream = stream.map(value -> {
            // 简单处理：添加处理时间戳
            long timestamp = System.currentTimeMillis();
            return "{\"processed\": true, \"timestamp\": " + timestamp + ", \"original\": " + value + "}";
        });

        // 将处理后的数据发送到Kafka
        processedStream.sinkTo(sink);

        // 执行作业
        env.execute("Flink Kafka Test Job");
    }
}
```

### 8.2 编译和运行测试作业

安装Maven并编译项目：

```bash
# 安装Maven
sudo apt update
sudo apt install -y maven

# 编译项目
cd ~/flink-kafka-test
mvn clean package
```

提交作业到Flink集群：

```bash
/opt/flink/current/bin/flink run -c com.example.flink.KafkaStreamingJob target/flink-kafka-test-1.0-SNAPSHOT.jar
```

### 8.3 验证测试作业

在Hadoop01节点上发送测试消息到Kafka：

```bash
# 切换到kafka用户
sudo su - kafka

# 启动生产者控制台
/opt/kafka/kafka/bin/kafka-console-producer.sh --topic user-behavior --bootstrap-server hadoop01:9092

# 输入测试消息（JSON格式）
{"userId": "user1", "itemId": "item1", "action": "click", "timestamp": 1621234567890}
```

在另一个终端中查看处理后的消息：

```bash
# 启动消费者控制台
/opt/kafka/kafka/bin/kafka-console-consumer.sh --topic recommendation-results --from-beginning --bootstrap-server hadoop01:9092
```

## 9. 故障排除

### 9.1 检查日志

```bash
# 查看JobManager日志
sudo tail -f /var/log/flink/flink-flink-jobmanager-*.log

# 查看TaskManager日志
sudo tail -f /var/log/flink/flink-flink-taskmanager-*.log
```

### 9.2 常见问题解决

1. 连接被拒绝：检查防火墙设置，确保端口开放
   ```bash
   sudo ufw status
   sudo ufw allow 6123/tcp  # JobManager RPC端口
   sudo ufw allow 8081/tcp  # JobManager Web UI端口
   ```

2. 任务提交失败：检查JobManager状态和配置
   ```bash
   # 检查JobManager状态
   sudo systemctl status flink-jobmanager
   ```

3. TaskManager未注册：检查网络连接和配置
   ```bash
   # 检查TaskManager状态
   sudo systemctl status flink-taskmanager
   ```

## 10. 性能调优

根据实际负载情况，可以调整以下参数：

```yaml
# 增加TaskManager内存
taskmanager.memory.process.size: 4096m

# 增加任务槽数量
taskmanager.numberOfTaskSlots: 8

# 调整网络缓冲区
taskmanager.network.memory.fraction: 0.1
taskmanager.network.memory.min: 64mb
taskmanager.network.memory.max: 1gb

# 调整检查点设置
execution.checkpointing.interval: 10000
execution.checkpointing.timeout: 60000
```

## 11. 安全配置（可选）

如需启用安全认证，可以配置以下内容：

```yaml
# SSL配置
security.ssl.enabled: true
security.ssl.keystore: /path/to/keystore.jks
security.ssl.keystore-password: keystore-password
security.ssl.key-password: key-password
security.ssl.truststore: /path/to/truststore.jks
security.ssl.truststore-password: truststore-password
```

## 12. 监控配置（可选）

可以配置Prometheus和Grafana监控Flink集群：

```yaml
# 启用Prometheus监控
metrics.reporter.prom.class: org.apache.flink.metrics.prometheus.PrometheusReporter
metrics.reporter.prom.port: 9250
```

## 13. 总结

完成上述步骤后，我们已经成功配置了一个具有1个JobManager和3个TaskManager的Flink分布式环境，包括：
- 在所有节点上安装Flink
- 配置JobManager和TaskManager
- 配置Flink与Kafka的连接器
- 创建并测试一个简单的Flink作业，用于从Kafka读取数据并写入Kafka

这个Flink分布式环境将作为我们实时电商推荐系统的数据处理引擎，用于处理用户行为数据并生成推荐结果。
