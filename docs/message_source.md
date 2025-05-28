# 消息源软件工程设计与实现

## 1. 消息源软件概述

本文档详细描述了分布式实时电商推荐系统中消息源软件的设计与实现。消息源软件是整个系统的起点和终点，负责模拟用户行为、管理商品信息，并将生成的事件发送到Kafka，同时接收并展示Flink生成的推荐结果。

### 1.1 系统目标

- 模拟电商平台的用户行为，如浏览、点击、购买等
- 提供接口用于创建和管理商品信息
- 将模拟的用户行为和商品信息发送到Kafka主题
- 消费Kafka中的推荐结果主题，并展示给用户
- 提供一个简单的Web界面进行交互操作

### 1.2 技术选型

- **编程语言**: Java 11
- **Web框架**: Java Servlet API (使用嵌入式Jetty)
- **消息队列**: Apache Kafka 2.8.1
- **序列化格式**: JSON (使用Gson库)
- **构建工具**: Maven 3.6.3

## 2. 系统架构

### 2.1 整体架构

消息源软件采用基于Java Servlet的Web应用架构，主要包括以下组件：

1. **Web界面层**: 提供HTML页面和Servlet接口，用于用户交互
2. **数据模型层**: 定义商品和用户行为数据模型
3. **数据生成器层**: 负责生成模拟的用户行为和商品数据
4. **Kafka连接器层**: 负责与Kafka集群进行通信，发送和接收消息
5. **推荐结果消费层**: 负责消费Kafka中的推荐结果并进行处理

### 2.2 数据流向

```
Web界面 --> Servlet --> 数据生成器 --> Kafka连接器 --> Kafka (user-behaviors, products)
Kafka (recommendations) --> Kafka连接器 --> 推荐结果消费者 --> Web界面
```

## 3. 数据模型设计

消息源软件使用与推荐系统相同的数据模型：

- **商品模型 (Product.java)**: 包含productId, productName, category, subCategory, price, features等。
- **用户行为模型 (UserBehavior.java)**: 包含userId, productId, behaviorType, timestamp, properties等。

## 4. 核心组件实现

### 4.1 Kafka连接器 (KafkaConnector.java)

```java
package org.messagesource.connectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Kafka连接器
 * 负责与Kafka集群进行通信，发送和接收消息
 */
public class KafkaConnector {
    
    private final KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;
    private final String bootstrapServers;
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;
    
    public KafkaConnector(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        
        // 配置生产者
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "1"); // 只需要Leader确认
        
        this.producer = new KafkaProducer<>(producerProps);
    }
    
    /**
     * 发送消息到指定主题
     * @param topic 主题名称
     * @param message 消息内容
     */
    public void sendMessage(String topic, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, message);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error sending message to topic " + topic + ": " + exception.getMessage());
            }
        });
    }
    
    /**
     * 启动消费者线程，订阅指定主题
     * @param topic 主题名称
     * @param groupId 消费者组ID
     * @param messageHandler 消息处理回调函数
     */
    public void startConsumer(String topic, String groupId, Consumer<String> messageHandler) {
        if (running) {
            System.out.println("Consumer is already running.");
            return;
        }
        
        // 配置消费者
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        
        this.consumer = new KafkaConsumer<>(consumerProps);
        this.consumer.subscribe(Collections.singletonList(topic));
        
        running = true;
        consumerExecutor.submit(() -> {
            try {
                while (running) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    records.forEach(record -> {
                        try {
                            messageHandler.accept(record.value());
                        } catch (Exception e) {
                            System.err.println("Error processing message from topic " + topic + ": " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Kafka consumer error: " + e.getMessage());
            } finally {
                consumer.close();
                System.out.println("Kafka consumer stopped.");
            }
        });
        System.out.println("Kafka consumer started for topic: " + topic);
    }
    
    /**
     * 停止消费者线程
     */
    public void stopConsumer() {
        running = false;
        consumerExecutor.shutdown();
    }
    
    /**
     * 关闭生产者
     */
    public void closeProducer() {
        producer.flush();
        producer.close();
        System.out.println("Kafka producer closed.");
    }
    
    /**
     * 关闭连接器（停止消费者并关闭生产者）
     */
    public void close() {
        stopConsumer();
        closeProducer();
    }
}
```

### 4.2 数据生成器

#### 4.2.1 用户行为生成器 (UserBehaviorGenerator.java)

```java
package org.messagesource.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.messagesource.connectors.KafkaConnector;
import org.messagesource.models.UserBehavior;

import java.util.Random;

/**
 * 用户行为生成器
 * 模拟生成用户行为数据并发送到Kafka
 */
public class UserBehaviorGenerator {
    
    private final KafkaConnector kafkaConnector;
    private final Gson gson;
    private final Random random = new Random();
    private final String[] userIds = {"user1", "user2", "user3", "user4", "user5"};
    private final String[] productIds = {"prod101", "prod102", "prod103", "prod104", "prod105", "prod201", "prod202"};
    private final UserBehavior.BehaviorType[] behaviorTypes = UserBehavior.BehaviorType.values();
    
    public UserBehaviorGenerator(KafkaConnector kafkaConnector) {
        this.kafkaConnector = kafkaConnector;
        this.gson = new GsonBuilder().create();
    }
    
    /**
     * 生成单个用户行为
     * @param userId 用户ID
     * @param productId 商品ID
     * @param behaviorType 行为类型
     */
    public void generateBehavior(String userId, String productId, UserBehavior.BehaviorType behaviorType) {
        UserBehavior behavior = new UserBehavior(userId, productId, behaviorType);
        
        // 添加随机属性
        if (behaviorType == UserBehavior.BehaviorType.RATE) {
            behavior.addProperty("rating", random.nextInt(5) + 1);
        }
        if (behaviorType == UserBehavior.BehaviorType.COMMENT) {
            behavior.addProperty("commentLength", random.nextInt(100) + 10);
        }
        
        String jsonBehavior = gson.toJson(behavior);
        kafkaConnector.sendMessage("user-behaviors", jsonBehavior);
        System.out.println("Generated behavior: " + jsonBehavior);
    }
    
    /**
     * 随机生成一个用户行为
     */
    public void generateRandomBehavior() {
        String userId = userIds[random.nextInt(userIds.length)];
        String productId = productIds[random.nextInt(productIds.length)];
        UserBehavior.BehaviorType behaviorType = behaviorTypes[random.nextInt(behaviorTypes.length)];
        
        generateBehavior(userId, productId, behaviorType);
    }
    
    /**
     * 模拟购买操作
     * @param userId 用户ID
     * @param productId 商品ID
     */
    public void simulatePurchase(String userId, String productId) {
        generateBehavior(userId, productId, UserBehavior.BehaviorType.CLICK);
        // 模拟延迟
        try { Thread.sleep(random.nextInt(500)); } catch (InterruptedException ignored) {}
        generateBehavior(userId, productId, UserBehavior.BehaviorType.CART);
        try { Thread.sleep(random.nextInt(1000)); } catch (InterruptedException ignored) {}
        generateBehavior(userId, productId, UserBehavior.BehaviorType.PURCHASE);
    }
}
```

#### 4.2.2 商品生成器 (ProductGenerator.java)

```java
package org.messagesource.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.messagesource.connectors.KafkaConnector;
import org.messagesource.models.Product;

import java.util.Random;

/**
 * 商品生成器
 * 创建商品信息并发送到Kafka
 */
public class ProductGenerator {
    
    private final KafkaConnector kafkaConnector;
    private final Gson gson;
    private final Random random = new Random();
    
    public ProductGenerator(KafkaConnector kafkaConnector) {
        this.kafkaConnector = kafkaConnector;
        this.gson = new GsonBuilder().create();
    }
    
    /**
     * 创建新商品
     * @param productId 商品ID
     * @param productName 商品名称
     * @param category 商品类别
     */
    public void createProduct(String productId, String productName, String category) {
        Product product = new Product(productId, productName, category);
        product.setPrice(random.nextDouble() * 100 + 10); // 随机价格
        product.setSubCategory(category + "-Sub" + (random.nextInt(3) + 1));
        
        // 添加随机特征
        product.updateFeature("featureA", random.nextDouble());
        product.updateFeature("featureB", random.nextDouble());
        
        String jsonProduct = gson.toJson(product);
        kafkaConnector.sendMessage("products", jsonProduct);
        System.out.println("Created product: " + jsonProduct);
    }
}
```

### 4.3 推荐结果消费者 (RecommendationConsumer.java)

```java
package org.messagesource.consumers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.messagesource.connectors.KafkaConnector;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推荐结果消费者
 * 消费Kafka中的推荐结果并存储
 */
public class RecommendationConsumer {
    
    private final KafkaConnector kafkaConnector;
    private final Gson gson;
    private final Map<String, List<String>> userRecommendations;
    
    public RecommendationConsumer(KafkaConnector kafkaConnector) {
        this.kafkaConnector = kafkaConnector;
        this.gson = new GsonBuilder().create();
        this.userRecommendations = new ConcurrentHashMap<>();
    }
    
    /**
     * 开始消费推荐结果
     */
    public void startConsuming() {
        kafkaConnector.startConsumer("recommendations", "message-source-group", this::handleMessage);
    }
    
    /**
     * 处理接收到的推荐结果消息
     * @param message 推荐结果JSON字符串
     */
    private void handleMessage(String message) {
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> recommendationData = gson.fromJson(message, type);
            
            String userId = (String) recommendationData.get("userId");
            List<String> recommendations = (List<String>) recommendationData.get("recommendations");
            
            if (userId != null && recommendations != null) {
                userRecommendations.put(userId, recommendations);
                System.out.println("Received recommendations for user " + userId + ": " + recommendations);
            }
        } catch (Exception e) {
            System.err.println("Error parsing recommendation message: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定用户的最新推荐结果
     * @param userId 用户ID
     * @return 推荐商品ID列表，如果无推荐则返回空列表
     */
    public List<String> getRecommendations(String userId) {
        return userRecommendations.getOrDefault(userId, List.of());
    }
    
    /**
     * 停止消费
     */
    public void stopConsuming() {
        kafkaConnector.stopConsumer();
    }
}
```

### 4.4 Web界面与Servlet (MessageSourceServlet.java)

```java
package org.messagesource.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.messagesource.consumers.RecommendationConsumer;
import org.messagesource.generators.ProductGenerator;
import org.messagesource.generators.UserBehaviorGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * 消息源软件Web界面Servlet
 * 提供创建商品、模拟行为和查看推荐的接口
 */
public class MessageSourceServlet extends HttpServlet {
    
    private final ProductGenerator productGenerator;
    private final UserBehaviorGenerator behaviorGenerator;
    private final RecommendationConsumer recommendationConsumer;
    
    public MessageSourceServlet(ProductGenerator productGenerator, UserBehaviorGenerator behaviorGenerator, RecommendationConsumer recommendationConsumer) {
        this.productGenerator = productGenerator;
        this.behaviorGenerator = behaviorGenerator;
        this.recommendationConsumer = recommendationConsumer;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter writer = resp.getWriter();
        
        String action = req.getParameter("action");
        String userId = req.getParameter("userId");
        
        writer.println("<html><head><title>消息源软件</title></head><body>");
        writer.println("<h1>分布式实时电商推荐系统 - 消息源软件</h1>");
        
        // 创建商品表单
        writer.println("<h2>创建商品</h2>");
        writer.println("<form method='post'>");
        writer.println("商品ID: <input type='text' name='productId' required><br>");
        writer.println("商品名称: <input type='text' name='productName' required><br>");
        writer.println("商品类别: <input type='text' name='category' required><br>");
        writer.println("<input type='hidden' name='action' value='createProduct'>");
        writer.println("<input type='submit' value='创建商品'>");
        writer.println("</form>");
        
        // 模拟购买表单
        writer.println("<h2>模拟购买</h2>");
        writer.println("<form method='post'>");
        writer.println("用户ID: <input type='text' name='userId' required><br>");
        writer.println("商品ID: <input type='text' name='productId' required><br>");
        writer.println("<input type='hidden' name='action' value='simulatePurchase'>");
        writer.println("<input type='submit' value='模拟购买'>");
        writer.println("</form>");
        
        // 随机生成行为按钮
        writer.println("<h2>随机生成行为</h2>");
        writer.println("<form method='post'>");
        writer.println("<input type='hidden' name='action' value='randomBehavior'>");
        writer.println("<input type='submit' value='生成随机行为'>");
        writer.println("</form>");
        
        // 查看推荐表单
        writer.println("<h2>查看推荐结果</h2>");
        writer.println("<form method='get'>");
        writer.println("用户ID: <input type='text' name='userId' value='" + (userId != null ? userId : "") + "' required><br>");
        writer.println("<input type='hidden' name='action' value='getRecommendations'>");
        writer.println("<input type='submit' value='查看推荐'>");
        writer.println("</form>");
        
        // 显示推荐结果
        if ("getRecommendations".equals(action) && userId != null && !userId.isEmpty()) {
            List<String> recommendations = recommendationConsumer.getRecommendations(userId);
            writer.println("<h3>用户 " + userId + " 的推荐结果:</h3>");
            if (recommendations.isEmpty()) {
                writer.println("<p>暂无推荐</p>");
            } else {
                writer.println("<ul>");
                for (String productId : recommendations) {
                    writer.println("<li>" + productId + "</li>");
                }
                writer.println("</ul>");
            }
        }
        
        writer.println("</body></html>");
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        
        if ("createProduct".equals(action)) {
            String productId = req.getParameter("productId");
            String productName = req.getParameter("productName");
            String category = req.getParameter("category");
            if (productId != null && productName != null && category != null) {
                productGenerator.createProduct(productId, productName, category);
            }
        } else if ("simulatePurchase".equals(action)) {
            String userId = req.getParameter("userId");
            String productId = req.getParameter("productId");
            if (userId != null && productId != null) {
                behaviorGenerator.simulatePurchase(userId, productId);
            }
        } else if ("randomBehavior".equals(action)) {
            behaviorGenerator.generateRandomBehavior();
        }
        
        // 重定向回GET页面
        resp.sendRedirect(req.getContextPath() + req.getServletPath());
    }
    
    // 启动嵌入式Jetty服务器
    public static void startServer(int port, ProductGenerator pg, UserBehaviorGenerator bg, RecommendationConsumer rc) throws Exception {
        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        context.addServlet(new ServletHolder(new MessageSourceServlet(pg, bg, rc)), "/*");
        
        server.start();
        System.out.println("Message Source Web Server started on port " + port);
        server.join();
    }
}
```

### 4.5 主应用程序 (MessageSourceApp.java)

```java
package org.messagesource;

import org.messagesource.connectors.KafkaConnector;
import org.messagesource.consumers.RecommendationConsumer;
import org.messagesource.generators.ProductGenerator;
import org.messagesource.generators.UserBehaviorGenerator;
import org.messagesource.web.MessageSourceServlet;

/**
 * 消息源软件主应用程序
 * 初始化组件并启动Web服务器
 */
public class MessageSourceApp {
    
    public static void main(String[] args) {
        // Kafka服务器地址
        String kafkaBootstrapServers = "kafka1:9092,kafka2:9092,kafka3:9092";
        // Web服务器端口
        int webServerPort = 8082;
        
        // 初始化Kafka连接器
        KafkaConnector kafkaConnector = new KafkaConnector(kafkaBootstrapServers);
        
        // 初始化生成器
        ProductGenerator productGenerator = new ProductGenerator(kafkaConnector);
        UserBehaviorGenerator behaviorGenerator = new UserBehaviorGenerator(kafkaConnector);
        
        // 初始化推荐结果消费者
        RecommendationConsumer recommendationConsumer = new RecommendationConsumer(kafkaConnector);
        recommendationConsumer.startConsuming();
        
        // 添加JVM关闭钩子，确保资源被释放
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Message Source App...");
            recommendationConsumer.stopConsuming();
            kafkaConnector.close();
            System.out.println("Message Source App shut down gracefully.");
        }));
        
        // 启动Web服务器
        try {
            MessageSourceServlet.startServer(webServerPort, productGenerator, behaviorGenerator, recommendationConsumer);
        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            // 确保即使服务器启动失败也关闭连接器
            recommendationConsumer.stopConsuming();
            kafkaConnector.close();
        }
    }
}
```

## 5. 构建与部署

### 5.1 项目结构

```
message-source/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── messagesource/
│   │   │           ├── connectors/
│   │   │           │   └── KafkaConnector.java
│   │   │           ├── consumers/
│   │   │           │   └── RecommendationConsumer.java
│   │   │           ├── generators/
│   │   │           │   ├── ProductGenerator.java
│   │   │           │   └── UserBehaviorGenerator.java
│   │   │           ├── models/
│   │   │           │   ├── Product.java
│   │   │           │   └── UserBehavior.java
│   │   │           ├── web/
│   │   │           │   └── MessageSourceServlet.java
│   │   │           └── MessageSourceApp.java
│   │   └── resources/
│   │       └── log4j.properties
│   └── test/
│       └── java/
│           └── org/
│               └── messagesource/
│                   └── generators/
│                       └── UserBehaviorGeneratorTest.java
├── pom.xml
├── Dockerfile
└── README.md
```

### 5.2 Maven配置 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.messagesource</groupId>
    <artifactId>message-source</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <kafka.version>2.8.1</kafka.version>
        <jetty.version>9.4.44.v20210927</jetty.version>
    </properties>

    <dependencies>
        <!-- Kafka Clients -->
        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <version>${kafka.version}</version>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.8.9</version>
        </dependency>

        <!-- Jetty Server -->
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-server</artifactId>
            <version>${jetty.version}</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlet</artifactId>
            <version>${jetty.version}</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.32</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>1.7.32</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
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
                                    <mainClass>org.messagesource.MessageSourceApp</mainClass>
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

### 5.3 Dockerfile

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# 复制JAR包
COPY target/message-source-1.0-SNAPSHOT.jar /app/message-source.jar

# 暴露Web端口
EXPOSE 8082

# 设置启动命令
CMD ["java", "-jar", "/app/message-source.jar"]
```

### 5.4 构建与运行

```bash
# 构建项目
mvn clean package

# 运行应用程序
java -jar target/message-source-1.0-SNAPSHOT.jar

# 使用Docker构建镜像
docker build -t message-source:1.0 .

# 运行Docker容器
docker run -p 8082:8082 --network recommendation-network message-source:1.0
```

## 6. 使用指南

1. **启动应用**: 通过`java -jar`或Docker运行。
2. **访问Web界面**: 在浏览器中打开`http://localhost:8082`。
3. **创建商品**: 在"创建商品"表单中输入信息并提交。
4. **模拟购买**: 在"模拟购买"表单中输入用户ID和商品ID并提交。
5. **生成随机行为**: 点击"生成随机行为"按钮。
6. **查看推荐**: 在"查看推荐结果"表单中输入用户ID并提交，页面将显示该用户的最新推荐列表。

## 7. 总结

本文档详细介绍了分布式实时电商推荐系统中消息源软件的设计与实现。该软件通过模拟用户行为和管理商品信息，为推荐系统提供了数据源，并通过消费推荐结果展示了系统的最终输出。其简单的Web界面方便了用户与系统的交互和演示。通过本文档的指导，可以成功构建和部署消息源软件，完成整个推荐系统的闭环。
