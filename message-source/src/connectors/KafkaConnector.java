package org.messagesource.connectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Kafka连接器类
 * 提供与Kafka交互的方法，包括消息生产和消费
 */
public class KafkaConnector {
    
    private final String bootstrapServers;
    private final Gson gson;
    private KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;
    
    public KafkaConnector(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        this.gson = new GsonBuilder().create();
        initProducer();
    }
    
    /**
     * 初始化Kafka生产者
     */
    private void initProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        
        this.producer = new KafkaProducer<>(props);
    }
    
    /**
     * 初始化Kafka消费者
     * @param groupId 消费者组ID
     * @param topic 要订阅的主题
     */
    public void initConsumer(String groupId, String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(topic));
    }
    
    /**
     * 发送对象到Kafka主题
     * @param topic 主题名称
     * @param key 消息键
     * @param value 消息值对象
     * @param <T> 对象类型
     * @return 是否发送成功
     */
    public <T> boolean sendObject(String topic, String key, T value) {
        String json = gson.toJson(value);
        return sendMessage(topic, key, json);
    }
    
    /**
     * 发送字符串消息到Kafka主题
     * @param topic 主题名称
     * @param key 消息键
     * @param value 消息值
     * @return 是否发送成功
     */
    public boolean sendMessage(String topic, String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        try {
            Future<?> future = producer.send(record);
            future.get(); // 等待发送完成
            return true;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 从Kafka主题消费消息
     * @param timeout 超时时间（毫秒）
     * @return 消费的记录
     */
    public ConsumerRecords<String, String> consumeMessages(long timeout) {
        if (consumer == null) {
            throw new IllegalStateException("Consumer not initialized. Call initConsumer first.");
        }
        return consumer.poll(Duration.ofMillis(timeout));
    }
    
    /**
     * 将消费的JSON消息转换为对象
     * @param record Kafka消费记录
     * @param clazz 目标类型
     * @param <T> 对象类型
     * @return 转换后的对象
     */
    public <T> T convertToObject(ConsumerRecord<String, String> record, Class<T> clazz) {
        return gson.fromJson(record.value(), clazz);
    }
    
    /**
     * 关闭Kafka连接
     */
    public void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }
}
