package org.messagesource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.messagesource.connectors.KafkaConnector;
import org.messagesource.consumers.RecommendationConsumer;
import org.messagesource.generators.ProductGenerator;
import org.messagesource.generators.UserBehaviorGenerator;
import org.messagesource.models.Product;
import org.messagesource.models.UserBehavior;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 消息源软件主类
 * 用于模拟用户行为、管理商品、生成Kafka消息和展示推荐结果
 */
public class MessageSourceApp {
    
    // Kafka配置
    private static final String BOOTSTRAP_SERVERS = "kafka1:9092,kafka2:9092,kafka3:9092";
    private static final String USER_BEHAVIOR_TOPIC = "user-behaviors";
    private static final String PRODUCT_TOPIC = "products";
    private static final String RECOMMENDATION_TOPIC = "recommendations";
    private static final String CONSUMER_GROUP_ID = "message-source";
    
    // 生成器和连接器
    private final ProductGenerator productGenerator;
    private final Map<String, Product> productMap;
    private final List<String> userIds;
    private UserBehaviorGenerator behaviorGenerator;
    private final KafkaConnector kafkaConnector;
    private final RecommendationConsumer recommendationConsumer;
    
    // 执行器
    private ScheduledExecutorService behaviorExecutor;
    private boolean running;
    
    // JSON序列化
    private final Gson gson;
    
    public MessageSourceApp() {
        this.productGenerator = new ProductGenerator();
        this.productMap = new HashMap<>();
        this.userIds = new ArrayList<>();
        this.kafkaConnector = new KafkaConnector(BOOTSTRAP_SERVERS);
        this.recommendationConsumer = new RecommendationConsumer(kafkaConnector, RECOMMENDATION_TOPIC, CONSUMER_GROUP_ID);
        this.gson = new GsonBuilder().create();
        this.running = false;
        
        // 初始化用户ID列表
        initializeUserIds();
        
        // 初始化行为生成器
        this.behaviorGenerator = new UserBehaviorGenerator(userIds, productMap);
    }
    
    /**
     * 初始化用户ID列表
     */
    private void initializeUserIds() {
        // 生成100个用户ID
        for (int i = 1; i <= 100; i++) {
            userIds.add("U" + String.format("%06d", i));
        }
    }
    
    /**
     * 启动消息源软件
     */
    public void start() {
        if (running) {
            return;
        }
        
        // 启动推荐结果消费者
        recommendationConsumer.start(1000);
        
        // 添加推荐结果监听器
        recommendationConsumer.addListener(this::handleRecommendation);
        
        // 启动行为生成线程
        behaviorExecutor = Executors.newScheduledThreadPool(2);
        behaviorExecutor.scheduleAtFixedRate(this::generateAndSendRandomBehavior, 0, 1000, TimeUnit.MILLISECONDS);
        
        running = true;
        System.out.println("消息源软件已启动");
    }
    
    /**
     * 停止消息源软件
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        // 停止行为生成线程
        if (behaviorExecutor != null) {
            behaviorExecutor.shutdown();
            try {
                if (!behaviorExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                    behaviorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                behaviorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 停止推荐结果消费者
        recommendationConsumer.stop();
        
        // 关闭Kafka连接
        kafkaConnector.close();
        
        running = false;
        System.out.println("消息源软件已停止");
    }
    
    /**
     * 生成并发送随机用户行为
     */
    private void generateAndSendRandomBehavior() {
        try {
            // 生成随机用户行为
            UserBehavior behavior = behaviorGenerator.generateRandomBehavior();
            
            // 发送到Kafka
            String key = behavior.getUserId();
            boolean success = kafkaConnector.sendObject(USER_BEHAVIOR_TOPIC, key, behavior);
            
            if (success) {
                System.out.println("已发送用户行为: " + behavior);
            } else {
                System.err.println("发送用户行为失败: " + behavior);
            }
        } catch (Exception e) {
            System.err.println("生成或发送用户行为时出错: " + e.getMessage());
        }
    }
    
    /**
     * 创建并发送新商品
     * @param productName 商品名称
     * @param category 类别
     * @param subCategory 子类别
     * @param price 价格
     * @return 创建的商品
     */
    public Product createAndSendProduct(String productName, String category, String subCategory, double price) {
        // 生成商品ID
        String productId = "P" + UUID.randomUUID().toString().substring(0, 8);
        
        // 创建商品
        Product product = productGenerator.createSpecificProduct(productId, productName, category, subCategory, price);
        
        // 添加到商品映射
        productMap.put(productId, product);
        
        // 发送到Kafka
        String key = product.getProductId();
        boolean success = kafkaConnector.sendObject(PRODUCT_TOPIC, key, product);
        
        if (success) {
            System.out.println("已发送商品: " + product);
        } else {
            System.err.println("发送商品失败: " + product);
        }
        
        return product;
    }
    
    /**
     * 生成并发送用户行为序列
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 生成的行为序列
     */
    public List<UserBehavior> generateAndSendBehaviorSequence(String userId, String productId) {
        // 检查用户ID和商品ID是否有效
        if (!userIds.contains(userId) || !productMap.containsKey(productId)) {
            throw new IllegalArgumentException("无效的用户ID或商品ID");
        }
        
        // 生成行为序列
        List<UserBehavior> sequence = behaviorGenerator.generateBehaviorSequence(userId, productId);
        
        // 发送到Kafka
        for (UserBehavior behavior : sequence) {
            String key = behavior.getUserId();
            boolean success = kafkaConnector.sendObject(USER_BEHAVIOR_TOPIC, key, behavior);
            
            if (success) {
                System.out.println("已发送用户行为: " + behavior);
            } else {
                System.err.println("发送用户行为失败: " + behavior);
            }
        }
        
        return sequence;
    }
    
    /**
     * 处理接收到的推荐结果
     * @param userId 用户ID
     * @param recommendation 推荐结果
     */
    private void handleRecommendation(String userId, Map<String, Object> recommendation) {
        System.out.println("接收到用户 " + userId + " 的推荐结果: " + recommendation);
        
        // 这里可以添加推荐结果的处理逻辑
        // 例如更新UI、记录日志等
    }
    
    /**
     * 获取指定用户的最新推荐结果
     * @param userId 用户ID
     * @return 推荐结果，如果不存在则返回null
     */
    public Map<String, Object> getRecommendationForUser(String userId) {
        return recommendationConsumer.getLatestRecommendation(userId);
    }
    
    /**
     * 获取所有用户的最新推荐结果
     * @return 用户ID到推荐结果的映射
     */
    public Map<String, Map<String, Object>> getAllRecommendations() {
        return recommendationConsumer.getAllLatestRecommendations();
    }
    
    /**
     * 获取所有商品
     * @return 商品映射
     */
    public Map<String, Product> getAllProducts() {
        return new HashMap<>(productMap);
    }
    
    /**
     * 获取所有用户ID
     * @return 用户ID列表
     */
    public List<String> getAllUserIds() {
        return new ArrayList<>(userIds);
    }
    
    /**
     * 初始化示例商品
     * @param count 商品数量
     */
    public void initializeExampleProducts(int count) {
        // 生成随机商品
        List<Product> products = productGenerator.generateRandomProducts(count);
        
        // 添加到商品映射并发送到Kafka
        for (Product product : products) {
            productMap.put(product.getProductId(), product);
            
            String key = product.getProductId();
            kafkaConnector.sendObject(PRODUCT_TOPIC, key, product);
        }
        
        System.out.println("已初始化 " + count + " 个示例商品");
    }
    
    /**
     * 主方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        MessageSourceApp app = new MessageSourceApp();
        
        // 初始化示例商品
        app.initializeExampleProducts(50);
        
        // 启动应用
        app.start();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        
        System.out.println("消息源软件已启动，按Ctrl+C停止");
    }
}
