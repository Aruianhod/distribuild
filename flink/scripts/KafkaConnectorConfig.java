package org.apache.flink.streaming.connectors.kafka;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;

/**
 * Flink与Kafka连接器配置示例
 * 用于实时电商推荐系统中的数据流处理
 */
public class KafkaConnectorConfig {

    /**
     * 创建Kafka消费者配置
     * @param bootstrapServers Kafka服务器地址
     * @param groupId 消费者组ID
     * @param topic 主题名称
     * @return Kafka消费者属性
     */
    public static Properties createConsumerProperties(String bootstrapServers, String groupId, String topic) {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("group.id", groupId);
        properties.setProperty("auto.offset.reset", "latest");
        properties.setProperty("enable.auto.commit", "false");
        properties.setProperty("isolation.level", "read_committed");
        properties.setProperty("max.poll.records", "500");
        properties.setProperty("fetch.max.bytes", "52428800"); // 50MB
        properties.setProperty("max.partition.fetch.bytes", "1048576"); // 1MB
        return properties;
    }

    /**
     * 创建Kafka生产者配置
     * @param bootstrapServers Kafka服务器地址
     * @return Kafka生产者属性
     */
    public static Properties createProducerProperties(String bootstrapServers) {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("acks", "all");
        properties.setProperty("retries", "3");
        properties.setProperty("batch.size", "16384");
        properties.setProperty("linger.ms", "10");
        properties.setProperty("buffer.memory", "33554432"); // 32MB
        properties.setProperty("max.in.flight.requests.per.connection", "1");
        properties.setProperty("enable.idempotence", "true");
        properties.setProperty("compression.type", "snappy");
        return properties;
    }

    /**
     * 创建Kafka消费者
     * @param env Flink流执行环境
     * @param bootstrapServers Kafka服务器地址
     * @param groupId 消费者组ID
     * @param topic 主题名称
     * @return 字符串数据流
     */
    public static DataStream<String> createKafkaConsumer(
            StreamExecutionEnvironment env,
            String bootstrapServers,
            String groupId,
            String topic) {
        
        Properties properties = createConsumerProperties(bootstrapServers, groupId, topic);
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(
                topic,
                new SimpleStringSchema(),
                properties);
        
        // 设置从最早的偏移量开始消费
        consumer.setStartFromEarliest();
        
        return env.addSource(consumer);
    }

    /**
     * 创建Kafka生产者
     * @param bootstrapServers Kafka服务器地址
     * @param topic 主题名称
     * @return Kafka生产者
     */
    public static FlinkKafkaProducer<String> createKafkaProducer(
            String bootstrapServers,
            String topic) {
        
        Properties properties = createProducerProperties(bootstrapServers);
        return new FlinkKafkaProducer<>(
                topic,
                new SimpleStringSchema(),
                properties,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE);
    }
}
