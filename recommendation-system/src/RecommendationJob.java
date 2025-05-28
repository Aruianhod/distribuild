package org.recommendation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.recommendation.models.Product;
import org.recommendation.models.User;
import org.recommendation.models.UserBehavior;
import org.recommendation.processors.RecommendationProcessor;
import org.recommendation.processors.UserBehaviorProcessor;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 推荐系统主作业类
 * 实现Flink流处理作业，从Kafka读取用户行为数据，生成推荐结果，并写回Kafka
 */
public class RecommendationJob {

    // Kafka配置
    private static final String BOOTSTRAP_SERVERS = "kafka1:9092,kafka2:9092,kafka3:9092";
    private static final String USER_BEHAVIOR_TOPIC = "user-behaviors";
    private static final String PRODUCT_TOPIC = "products";
    private static final String RECOMMENDATION_TOPIC = "recommendations";
    private static final String CONSUMER_GROUP_ID = "recommendation-system";

    // Gson实例，用于JSON序列化和反序列化
    private static final Gson GSON = new GsonBuilder().create();

    public static void main(String[] args) throws Exception {
        // 创建Flink流执行环境
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 设置并行度
        env.setParallelism(4);

        // 配置检查点
        env.enableCheckpointing(60000); // 每60秒做一次检查点

        // 从Kafka读取商品数据
        Properties productProperties = createKafkaConsumerProperties(BOOTSTRAP_SERVERS, CONSUMER_GROUP_ID + "-products");
        FlinkKafkaConsumer<String> productConsumer = new FlinkKafkaConsumer<>(
                PRODUCT_TOPIC,
                new SimpleStringSchema(),
                productProperties);
        productConsumer.setStartFromLatest();

        // 解析商品数据
        DataStream<Product> productStream = env.addSource(productConsumer)
                .map(new MapFunction<String, Product>() {
                    @Override
                    public Product map(String value) throws Exception {
                        return GSON.fromJson(value, Product.class);
                    }
                });

        // 收集所有商品到列表
        List<Product> allProducts = new ArrayList<>();
        productStream.executeAndCollect().forEachRemaining(allProducts::add);

        // 从Kafka读取用户行为数据
        Properties behaviorProperties = createKafkaConsumerProperties(BOOTSTRAP_SERVERS, CONSUMER_GROUP_ID + "-behaviors");
        FlinkKafkaConsumer<String> behaviorConsumer = new FlinkKafkaConsumer<>(
                USER_BEHAVIOR_TOPIC,
                new SimpleStringSchema(),
                behaviorProperties);
        behaviorConsumer.setStartFromLatest();

        // 解析用户行为数据
        DataStream<UserBehavior> behaviorStream = env.addSource(behaviorConsumer)
                .map(new MapFunction<String, UserBehavior>() {
                    @Override
                    public UserBehavior map(String value) throws Exception {
                        return GSON.fromJson(value, UserBehavior.class);
                    }
                });

        // 创建商品映射，用于快速查找商品信息
        Map<String, Product> productMap = new HashMap<>();
        for (Product product : allProducts) {
            productMap.put(product.getProductId(), product);
        }

        // 处理用户行为，更新用户模型
        DataStream<User> userStream = behaviorStream
                .keyBy(UserBehavior::getUserId)
                .process(new UserBehaviorProcessor(productMap));

        // 生成推荐结果
        DataStream<Map<String, Object>> recommendationStream = userStream
                .keyBy(User::getUserId)
                .process(new RecommendationProcessor(allProducts));

        // 将推荐结果序列化为JSON
        DataStream<String> recommendationJsonStream = recommendationStream
                .map(new MapFunction<Map<String, Object>, String>() {
                    @Override
                    public String map(Map<String, Object> value) throws Exception {
                        return GSON.toJson(value);
                    }
                });

        // 将推荐结果写回Kafka
        FlinkKafkaProducer<String> recommendationProducer = new FlinkKafkaProducer<>(
                BOOTSTRAP_SERVERS,
                RECOMMENDATION_TOPIC,
                new SimpleStringSchema());

        recommendationJsonStream.addSink(recommendationProducer);

        // 执行作业
        env.execute("E-commerce Recommendation System");
    }

    /**
     * 创建Kafka消费者配置
     * @param bootstrapServers Kafka服务器地址
     * @param groupId 消费者组ID
     * @return Kafka消费者属性
     */
    private static Properties createKafkaConsumerProperties(String bootstrapServers, String groupId) {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("group.id", groupId);
        properties.setProperty("auto.offset.reset", "latest");
        properties.setProperty("enable.auto.commit", "false");
        properties.setProperty("isolation.level", "read_committed");
        return properties;
    }
}
