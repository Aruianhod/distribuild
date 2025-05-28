package org.messagesource.consumers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.messagesource.connectors.KafkaConnector;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 推荐结果消费者
 * 用于从Kafka消费推荐系统生成的推荐结果
 */
public class RecommendationConsumer {
    
    private final KafkaConnector kafkaConnector;
    private final String topic;
    private final String groupId;
    private final Gson gson;
    
    // 存储最新的推荐结果，按用户ID索引
    private final Map<String, Map<String, Object>> latestRecommendations;
    
    // 推荐结果监听器列表
    private final List<RecommendationListener> listeners;
    
    // 消费线程
    private ScheduledExecutorService executor;
    private boolean running;
    
    public RecommendationConsumer(KafkaConnector kafkaConnector, String topic, String groupId) {
        this.kafkaConnector = kafkaConnector;
        this.topic = topic;
        this.groupId = groupId;
        this.gson = new GsonBuilder().create();
        this.latestRecommendations = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.running = false;
    }
    
    /**
     * 启动推荐结果消费
     * @param pollIntervalMs 轮询间隔（毫秒）
     */
    public void start(long pollIntervalMs) {
        if (running) {
            return;
        }
        
        // 初始化Kafka消费者
        kafkaConnector.initConsumer(groupId, topic);
        
        // 创建并启动消费线程
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::consumeRecommendations, 0, pollIntervalMs, TimeUnit.MILLISECONDS);
        
        running = true;
    }
    
    /**
     * 停止推荐结果消费
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        // 关闭消费线程
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        running = false;
    }
    
    /**
     * 消费推荐结果
     */
    private void consumeRecommendations() {
        try {
            // 从Kafka消费消息
            ConsumerRecords<String, String> records = kafkaConnector.consumeMessages(1000);
            
            // 处理消费的消息
            for (ConsumerRecord<String, String> record : records) {
                try {
                    // 解析推荐结果
                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> recommendation = gson.fromJson(record.value(), type);
                    
                    // 获取用户ID
                    String userId = (String) recommendation.get("userId");
                    if (userId != null) {
                        // 更新最新推荐结果
                        latestRecommendations.put(userId, recommendation);
                        
                        // 通知监听器
                        notifyListeners(userId, recommendation);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing recommendation: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error consuming recommendations: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定用户的最新推荐结果
     * @param userId 用户ID
     * @return 推荐结果，如果不存在则返回null
     */
    public Map<String, Object> getLatestRecommendation(String userId) {
        return latestRecommendations.get(userId);
    }
    
    /**
     * 获取所有用户的最新推荐结果
     * @return 用户ID到推荐结果的映射
     */
    public Map<String, Map<String, Object>> getAllLatestRecommendations() {
        return new ConcurrentHashMap<>(latestRecommendations);
    }
    
    /**
     * 添加推荐结果监听器
     * @param listener 监听器
     */
    public void addListener(RecommendationListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除推荐结果监听器
     * @param listener 监听器
     */
    public void removeListener(RecommendationListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 通知所有监听器
     * @param userId 用户ID
     * @param recommendation 推荐结果
     */
    private void notifyListeners(String userId, Map<String, Object> recommendation) {
        for (RecommendationListener listener : listeners) {
            listener.onRecommendationReceived(userId, recommendation);
        }
    }
    
    /**
     * 推荐结果监听器接口
     */
    public interface RecommendationListener {
        /**
         * 当接收到新的推荐结果时调用
         * @param userId 用户ID
         * @param recommendation 推荐结果
         */
        void onRecommendationReceived(String userId, Map<String, Object> recommendation);
    }
}
