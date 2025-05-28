# 消息源软件工程设计与实现

## 1. 消息源软件概述

消息源软件是分布式实时电商推荐系统的重要组成部分，主要负责模拟用户行为数据的生成和推荐结果的展示。本软件具有以下功能：

- **商品创建**：创建新的商品，并将商品信息发送到Kafka
- **用户行为模拟**：模拟用户的浏览、点击、加入购物车、购买等行为，并将行为数据发送到Kafka
- **推荐结果接收**：从Kafka接收推荐结果，并将其打印到终端
- **完整业务流程**：支持完整的业务流程，包括商品创建、用户行为模拟和推荐结果展示

消息源软件采用Java语言实现，使用Spring Boot框架构建，提供命令行界面供用户操作。

## 2. 系统架构

消息源软件的整体架构如下：

```
+----------------------------------+
|           消息源软件              |
|                                  |
|  +-------------+  +------------+ |
|  | 商品管理模块 |  | 用户管理模块 | |
|  +-------------+  +------------+ |
|                                  |
|  +-------------+  +------------+ |
|  | 行为模拟模块 |  | 推荐展示模块 | |
|  +-------------+  +------------+ |
|                                  |
|  +-------------+  +------------+ |
|  | Kafka生产者 |  | Kafka消费者 | |
|  +-------------+  +------------+ |
+----------------------------------+
          |                 ^
          v                 |
+----------------------------------+
|             Kafka                |
|  (user-behavior, recommendation-results) |
+----------------------------------+
```

各模块功能说明：
- **商品管理模块**：负责商品的创建、查询和管理
- **用户管理模块**：负责用户的创建、查询和管理
- **行为模拟模块**：负责模拟用户的各种行为
- **推荐展示模块**：负责接收和展示推荐结果
- **Kafka生产者**：负责将商品信息和用户行为数据发送到Kafka
- **Kafka消费者**：负责从Kafka接收推荐结果

## 3. 数据模型设计

### 3.1 商品数据模型

```java
public class Item {
    private String itemId;
    private String name;
    private String category;
    private double price;
    private List<String> features;
    private long createTime;
    
    // 构造函数、Getter和Setter方法
}
```

### 3.2 用户行为数据模型

```java
public class UserBehavior {
    private String userId;
    private String itemId;
    private String action;  // view, click, cart, purchase
    private long timestamp;
    
    // 构造函数、Getter和Setter方法
}
```

### 3.3 推荐结果数据模型

```java
public class RecommendationResult {
    private String userId;
    private List<RecommendedItem> recommendedItems;
    private long timestamp;
    
    // 构造函数、Getter和Setter方法
    
    public static class RecommendedItem {
        private String itemId;
        private String name;
        private double score;
        private String reason;
        
        // 构造函数、Getter和Setter方法
    }
}
```

## 4. 项目结构

```
message-source/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── messagesource/
│       │               ├── MessageSourceApplication.java
│       │               ├── command/
│       │               │   ├── CommandLineRunner.java
│       │               │   └── ShellCommands.java
│       │               ├── config/
│       │               │   └── KafkaConfig.java
│       │               ├── model/
│       │               │   ├── Item.java
│       │               │   ├── User.java
│       │               │   ├── UserBehavior.java
│       │               │   └── RecommendationResult.java
│       │               ├── service/
│       │               │   ├── ItemService.java
│       │               │   ├── UserService.java
│       │               │   ├── BehaviorService.java
│       │               │   └── RecommendationService.java
│       │               └── kafka/
│       │                   ├── KafkaProducer.java
│       │                   └── KafkaConsumer.java
│       └── resources/
│           └── application.properties
```

## 5. 核心代码实现

### 5.1 主应用类

```java
package com.example.messagesource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
public class MessageSourceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageSourceApplication.class, args);
    }
}
```

### 5.2 配置类

#### 5.2.1 KafkaConfig.java

```java
package com.example.messagesource.config;

import com.example.messagesource.model.RecommendationResult;
import com.example.messagesource.model.UserBehavior;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${kafka.consumer.group-id}")
    private String groupId;
    
    // 生产者配置
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    // 消费者配置
    @Bean
    public ConsumerFactory<String, RecommendationResult> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.messagesource.model");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(),
                new JsonDeserializer<>(RecommendationResult.class, false));
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RecommendationResult> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RecommendationResult> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

### 5.3 数据模型类

#### 5.3.1 Item.java

```java
package com.example.messagesource.model;

import java.util.List;

public class Item {
    private String itemId;
    private String name;
    private String category;
    private double price;
    private List<String> features;
    private long createTime;

    // 构造函数
    public Item() {}

    public Item(String itemId, String name, String category, double price, List<String> features) {
        this.itemId = itemId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.features = features;
        this.createTime = System.currentTimeMillis();
    }

    // Getter和Setter方法
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "Item{" +
                "itemId='" + itemId + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", features=" + features +
                ", createTime=" + createTime +
                '}';
    }
}
```

#### 5.3.2 User.java

```java
package com.example.messagesource.model;

public class User {
    private String userId;
    private String username;
    private long createTime;

    // 构造函数
    public User() {}

    public User(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.createTime = System.currentTimeMillis();
    }

    // Getter和Setter方法
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
```

#### 5.3.3 UserBehavior.java

```java
package com.example.messagesource.model;

public class UserBehavior {
    private String userId;
    private String itemId;
    private String action;
    private long timestamp;

    // 构造函数
    public UserBehavior() {}

    public UserBehavior(String userId, String itemId, String action) {
        this.userId = userId;
        this.itemId = itemId;
        this.action = action;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter和Setter方法
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "UserBehavior{" +
                "userId='" + userId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
```

#### 5.3.4 RecommendationResult.java

```java
package com.example.messagesource.model;

import java.util.List;

public class RecommendationResult {
    private String userId;
    private List<RecommendedItem> recommendedItems;
    private long timestamp;

    // 构造函数
    public RecommendationResult() {}

    public RecommendationResult(String userId, List<RecommendedItem> recommendedItems, long timestamp) {
        this.userId = userId;
        this.recommendedItems = recommendedItems;
        this.timestamp = timestamp;
    }

    // Getter和Setter方法
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<RecommendedItem> getRecommendedItems() { return recommendedItems; }
    public void setRecommendedItems(List<RecommendedItem> recommendedItems) { this.recommendedItems = recommendedItems; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("推荐结果 - 用户ID: ").append(userId).append("\n");
        sb.append("推荐时间: ").append(new java.util.Date(timestamp)).append("\n");
        sb.append("推荐商品列表:\n");
        
        if (recommendedItems != null && !recommendedItems.isEmpty()) {
            for (int i = 0; i < recommendedItems.size(); i++) {
                RecommendedItem item = recommendedItems.get(i);
                sb.append(i + 1).append(". ");
                sb.append("商品ID: ").append(item.getItemId());
                sb.append(", 商品名称: ").append(item.getName());
                sb.append(", 推荐分数: ").append(String.format("%.2f", item.getScore()));
                sb.append(", 推荐理由: ").append(item.getReason());
                sb.append("\n");
            }
        } else {
            sb.append("暂无推荐商品\n");
        }
        
        return sb.toString();
    }

    // 内部类：推荐商品
    public static class RecommendedItem {
        private String itemId;
        private String name;
        private double score;
        private String reason;

        // 构造函数
        public RecommendedItem() {}

        public RecommendedItem(String itemId, String name, double score, String reason) {
            this.itemId = itemId;
            this.name = name;
            this.score = score;
            this.reason = reason;
        }

        // Getter和Setter方法
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
```

### 5.4 服务类

#### 5.4.1 ItemService.java

```java
package com.example.messagesource.service;

import com.example.messagesource.kafka.KafkaProducer;
import com.example.messagesource.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    
    private final KafkaProducer kafkaProducer;
    private final Map<String, Item> itemMap = new ConcurrentHashMap<>();
    private final AtomicInteger itemIdCounter = new AtomicInteger(1);
    
    @Autowired
    public ItemService(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
        // 初始化一些示例商品
        initSampleItems();
    }
    
    private void initSampleItems() {
        // 电子产品类
        createItem("iPhone 13", "电子产品", 5999.00, List.of("智能手机", "苹果", "高端"));
        createItem("华为 Mate 40 Pro", "电子产品", 6999.00, List.of("智能手机", "华为", "高端"));
        createItem("小米 12", "电子产品", 3999.00, List.of("智能手机", "小米", "中端"));
        createItem("MacBook Pro", "电子产品", 12999.00, List.of("笔记本电脑", "苹果", "高端"));
        createItem("联想 ThinkPad", "电子产品", 8999.00, List.of("笔记本电脑", "联想", "商务"));
        
        // 服装类
        createItem("Nike 运动鞋", "服装", 899.00, List.of("鞋子", "运动", "耐克"));
        createItem("Adidas 运动裤", "服装", 399.00, List.of("裤子", "运动", "阿迪达斯"));
        createItem("优衣库 T恤", "服装", 99.00, List.of("上衣", "休闲", "优衣库"));
        createItem("H&M 连衣裙", "服装", 299.00, List.of("裙子", "时尚", "H&M"));
        createItem("Zara 外套", "服装", 599.00, List.of("外套", "时尚", "Zara"));
        
        // 食品类
        createItem("三只松鼠坚果", "食品", 59.90, List.of("零食", "坚果", "三只松鼠"));
        createItem("良品铺子肉干", "食品", 39.90, List.of("零食", "肉干", "良品铺子"));
        createItem("百草味果干", "食品", 29.90, List.of("零食", "果干", "百草味"));
        createItem("康师傅方便面", "食品", 4.50, List.of("方便食品", "面食", "康师傅"));
        createItem("统一老坛酸菜面", "食品", 4.50, List.of("方便食品", "面食", "统一"));
    }
    
    public Item createItem(String name, String category, double price, List<String> features) {
        String itemId = "item" + itemIdCounter.getAndIncrement();
        Item item = new Item(itemId, name, category, price, features);
        itemMap.put(itemId, item);
        
        // 发送商品信息到Kafka
        kafkaProducer.sendItemMessage(item);
        
        return item;
    }
    
    public Item getItem(String itemId) {
        return itemMap.get(itemId);
    }
    
    public List<Item> getAllItems() {
        return new ArrayList<>(itemMap.values());
    }
    
    public List<Item> getItemsByCategory(String category) {
        List<Item> result = new ArrayList<>();
        for (Item item : itemMap.values()) {
            if (item.getCategory().equals(category)) {
                result.add(item);
            }
        }
        return result;
    }
    
    public Map<String, List<Item>> getItemsByCategories() {
        Map<String, List<Item>> result = new HashMap<>();
        for (Item item : itemMap.values()) {
            String category = item.getCategory();
            if (!result.containsKey(category)) {
                result.put(category, new ArrayList<>());
            }
            result.get(category).add(item);
        }
        return result;
    }
}
```

#### 5.4.2 UserService.java

```java
package com.example.messagesource.service;

import com.example.messagesource.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserService {
    
    private final Map<String, User> userMap = new ConcurrentHashMap<>();
    private final AtomicInteger userIdCounter = new AtomicInteger(1);
    
    public UserService() {
        // 初始化一些示例用户
        initSampleUsers();
    }
    
    private void initSampleUsers() {
        createUser("张三");
        createUser("李四");
        createUser("王五");
        createUser("赵六");
        createUser("钱七");
    }
    
    public User createUser(String username) {
        String userId = "user" + userIdCounter.getAndIncrement();
        User user = new User(userId, username);
        userMap.put(userId, user);
        return user;
    }
    
    public User getUser(String userId) {
        return userMap.get(userId);
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(userMap.values());
    }
}
```

#### 5.4.3 BehaviorService.java

```java
package com.example.messagesource.service;

import com.example.messagesource.kafka.KafkaProducer;
import com.example.messagesource.model.Item;
import com.example.messagesource.model.User;
import com.example.messagesource.model.UserBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class BehaviorService {
    
    private final KafkaProducer kafkaProducer;
    private final UserService userService;
    private final ItemService itemService;
    private final List<UserBehavior> behaviorHistory = new ArrayList<>();
    private final Random random = new Random();
    
    @Autowired
    public BehaviorService(KafkaProducer kafkaProducer, UserService userService, ItemService itemService) {
        this.kafkaProducer = kafkaProducer;
        this.userService = userService;
        this.itemService = itemService;
    }
    
    public UserBehavior createBehavior(String userId, String itemId, String action) {
        User user = userService.getUser(userId);
        Item item = itemService.getItem(itemId);
        
        if (user == null || item == null) {
            throw new IllegalArgumentException("用户或商品不存在");
        }
        
        UserBehavior behavior = new UserBehavior(userId, itemId, action);
        behaviorHistory.add(behavior);
        
        // 发送用户行为数据到Kafka
        kafkaProducer.sendBehaviorMessage(behavior);
        
        return behavior;
    }
    
    public List<UserBehavior> getBehaviorHistory() {
        return new ArrayList<>(behaviorHistory);
    }
    
    public List<UserBehavior> getBehaviorHistoryByUser(String userId) {
        List<UserBehavior> result = new ArrayList<>();
        for (UserBehavior behavior : behaviorHistory) {
            if (behavior.getUserId().equals(userId)) {
                result.add(behavior);
            }
        }
        return result;
    }
    
    public List<UserBehavior> getBehaviorHistoryByItem(String itemId) {
        List<UserBehavior> result = new ArrayList<>();
        for (UserBehavior behavior : behaviorHistory) {
            if (behavior.getItemId().equals(itemId)) {
                result.add(behavior);
            }
        }
        return result;
    }
    
    public void simulateRandomBehavior() {
        List<User> users = userService.getAllUsers();
        List<Item> items = itemService.getAllItems();
        
        if (users.isEmpty() || items.isEmpty()) {
            return;
        }
        
        // 随机选择用户和商品
        User user = users.get(random.nextInt(users.size()));
        Item item = items.get(random.nextInt(items.size()));
        
        // 随机选择行为类型
        String[] actions = {"view", "click", "cart", "purchase"};
        String action = actions[random.nextInt(actions.length)];
        
        // 创建行为
        createBehavior(user.getUserId(), item.getItemId(), action);
    }
    
    public void simulateBehaviorSequence(String userId, String itemId) {
        // 模拟用户对某个商品的完整行为序列：浏览->点击->加入购物车->购买
        createBehavior(userId, itemId, "view");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        createBehavior(userId, itemId, "click");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        createBehavior(userId, itemId, "cart");
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        createBehavior(userId, itemId, "purchase");
    }
}
```

#### 5.4.4 RecommendationService.java

```java
package com.example.messagesource.service;

import com.example.messagesource.model.RecommendationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {
    
    private final Map<String, List<RecommendationResult>> userRecommendations = new HashMap<>();
    
    public void addRecommendation(RecommendationResult result) {
        String userId = result.getUserId();
        if (!userRecommendations.containsKey(userId)) {
            userRecommendations.put(userId, new ArrayList<>());
        }
        userRecommendations.get(userId).add(result);
        
        // 打印推荐结果到终端
        System.out.println("\n===== 收到新的推荐结果 =====");
        System.out.println(result);
        System.out.println("===========================\n");
    }
    
    public List<RecommendationResult> getRecommendationsForUser(String userId) {
        return userRecommendations.getOrDefault(userId, new ArrayList<>());
    }
    
    public RecommendationResult getLatestRecommendationForUser(String userId) {
        List<RecommendationResult> recommendations = userRecommendations.getOrDefault(userId, new ArrayList<>());
        if (recommendations.isEmpty()) {
            return null;
        }
        return recommendations.get(recommendations.size() - 1);
    }
    
    public Map<String, List<RecommendationResult>> getAllRecommendations() {
        return new HashMap<>(userRecommendations);
    }
}
```

### 5.5 Kafka生产者和消费者

#### 5.5.1 KafkaProducer.java

```java
package com.example.messagesource.kafka;

import com.example.messagesource.model.Item;
import com.example.messagesource.model.UserBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.user-behavior}")
    private String userBehaviorTopic;
    
    @Value("${kafka.topic.item-info}")
    private String itemInfoTopic;
    
    @Autowired
    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendBehaviorMessage(UserBehavior behavior) {
        kafkaTemplate.send(userBehaviorTopic, behavior.getUserId(), behavior);
        System.out.println("发送用户行为数据到Kafka: " + behavior);
    }
    
    public void sendItemMessage(Item item) {
        kafkaTemplate.send(itemInfoTopic, item.getItemId(), item);
        System.out.println("发送商品信息到Kafka: " + item);
    }
}
```

#### 5.5.2 KafkaConsumer.java

```java
package com.example.messagesource.kafka;

import com.example.messagesource.model.RecommendationResult;
import com.example.messagesource.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {
    
    private final RecommendationService recommendationService;
    
    @Autowired
    public KafkaConsumer(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }
    
    @KafkaListener(topics = "${kafka.topic.recommendation-results}", groupId = "${kafka.consumer.group-id}")
    public void consumeRecommendationResults(RecommendationResult result) {
        recommendationService.addRecommendation(result);
    }
}
```

### 5.6 命令行界面

#### 5.6.1 CommandLineRunner.java

```java
package com.example.messagesource.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class CommandLineRunner implements CommandLineRunner {
    
    private final ShellCommands shellCommands;
    
    @Autowired
    public CommandLineRunner(ShellCommands shellCommands) {
        this.shellCommands = shellCommands;
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== 电商推荐系统消息源软件 ===");
        System.out.println("输入 'help' 查看可用命令");
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            String[] parts = input.split("\\s+", 2);
            String command = parts[0].toLowerCase();
            String arguments = parts.length > 1 ? parts[1] : "";
            
            switch (command) {
                case "help":
                    shellCommands.help();
                    break;
                case "exit":
                    running = false;
                    break;
                case "list-users":
                    shellCommands.listUsers();
                    break;
                case "list-items":
                    shellCommands.listItems();
                    break;
                case "create-user":
                    shellCommands.createUser(arguments);
                    break;
                case "create-item":
                    shellCommands.createItem(arguments);
                    break;
                case "simulate-behavior":
                    shellCommands.simulateBehavior(arguments);
                    break;
                case "simulate-random":
                    shellCommands.simulateRandomBehavior();
                    break;
                case "simulate-sequence":
                    shellCommands.simulateBehaviorSequence(arguments);
                    break;
                case "show-recommendations":
                    shellCommands.showRecommendations(arguments);
                    break;
                default:
                    System.out.println("未知命令: " + command);
                    System.out.println("输入 'help' 查看可用命令");
                    break;
            }
        }
        
        System.out.println("程序已退出");
    }
}
```

#### 5.6.2 ShellCommands.java

```java
package com.example.messagesource.command;

import com.example.messagesource.model.Item;
import com.example.messagesource.model.RecommendationResult;
import com.example.messagesource.model.User;
import com.example.messagesource.model.UserBehavior;
import com.example.messagesource.service.BehaviorService;
import com.example.messagesource.service.ItemService;
import com.example.messagesource.service.RecommendationService;
import com.example.messagesource.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class ShellCommands {
    
    private final UserService userService;
    private final ItemService itemService;
    private final BehaviorService behaviorService;
    private final RecommendationService recommendationService;
    
    @Autowired
    public ShellCommands(UserService userService, ItemService itemService, BehaviorService behaviorService, RecommendationService recommendationService) {
        this.userService = userService;
        this.itemService = itemService;
        this.behaviorService = behaviorService;
        this.recommendationService = recommendationService;
    }
    
    public void help() {
        System.out.println("可用命令:");
        System.out.println("  help                      - 显示帮助信息");
        System.out.println("  exit                      - 退出程序");
        System.out.println("  list-users                - 列出所有用户");
        System.out.println("  list-items                - 列出所有商品");
        System.out.println("  create-user <username>    - 创建新用户");
        System.out.println("  create-item <name> <category> <price> <feature1,feature2,...> - 创建新商品");
        System.out.println("  simulate-behavior <userId> <itemId> <action> - 模拟用户行为");
        System.out.println("  simulate-random           - 模拟随机用户行为");
        System.out.println("  simulate-sequence <userId> <itemId> - 模拟用户完整行为序列");
        System.out.println("  show-recommendations [userId] - 显示推荐结果");
    }
    
    public void listUsers() {
        List<User> users = userService.getAllUsers();
        System.out.println("用户列表:");
        for (User user : users) {
            System.out.println("  " + user.getUserId() + " - " + user.getUsername());
        }
    }
    
    public void listItems() {
        Map<String, List<Item>> itemsByCategory = itemService.getItemsByCategories();
        System.out.println("商品列表:");
        for (Map.Entry<String, List<Item>> entry : itemsByCategory.entrySet()) {
            System.out.println("类别: " + entry.getKey());
            for (Item item : entry.getValue()) {
                System.out.println("  " + item.getItemId() + " - " + item.getName() + " - ¥" + item.getPrice());
            }
        }
    }
    
    public void createUser(String arguments) {
        if (arguments.isEmpty()) {
            System.out.println("错误: 缺少用户名参数");
            return;
        }
        
        User user = userService.createUser(arguments);
        System.out.println("创建用户成功: " + user.getUserId() + " - " + user.getUsername());
    }
    
    public void createItem(String arguments) {
        String[] args = arguments.split("\\s+", 4);
        if (args.length < 4) {
            System.out.println("错误: 参数不足");
            System.out.println("用法: create-item <name> <category> <price> <feature1,feature2,...>");
            return;
        }
        
        String name = args[0];
        String category = args[1];
        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("错误: 价格必须是数字");
            return;
        }
        
        List<String> features = Arrays.asList(args[3].split(","));
        
        Item item = itemService.createItem(name, category, price, features);
        System.out.println("创建商品成功: " + item.getItemId() + " - " + item.getName());
    }
    
    public void simulateBehavior(String arguments) {
        String[] args = arguments.split("\\s+", 3);
        if (args.length < 3) {
            System.out.println("错误: 参数不足");
            System.out.println("用法: simulate-behavior <userId> <itemId> <action>");
            return;
        }
        
        String userId = args[0];
        String itemId = args[1];
        String action = args[2];
        
        if (!action.equals("view") && !action.equals("click") && !action.equals("cart") && !action.equals("purchase")) {
            System.out.println("错误: 行为类型必须是 view, click, cart 或 purchase");
            return;
        }
        
        try {
            UserBehavior behavior = behaviorService.createBehavior(userId, itemId, action);
            System.out.println("模拟用户行为成功: " + behavior);
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
    
    public void simulateRandomBehavior() {
        behaviorService.simulateRandomBehavior();
        System.out.println("模拟随机用户行为成功");
    }
    
    public void simulateBehaviorSequence(String arguments) {
        String[] args = arguments.split("\\s+", 2);
        if (args.length < 2) {
            System.out.println("错误: 参数不足");
            System.out.println("用法: simulate-sequence <userId> <itemId>");
            return;
        }
        
        String userId = args[0];
        String itemId = args[1];
        
        try {
            behaviorService.simulateBehaviorSequence(userId, itemId);
            System.out.println("模拟用户完整行为序列成功");
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
    
    public void showRecommendations(String arguments) {
        if (arguments.isEmpty()) {
            // 显示所有用户的推荐结果
            Map<String, List<RecommendationResult>> allRecommendations = recommendationService.getAllRecommendations();
            if (allRecommendations.isEmpty()) {
                System.out.println("暂无推荐结果");
                return;
            }
            
            for (Map.Entry<String, List<RecommendationResult>> entry : allRecommendations.entrySet()) {
                String userId = entry.getKey();
                List<RecommendationResult> recommendations = entry.getValue();
                if (!recommendations.isEmpty()) {
                    RecommendationResult latest = recommendations.get(recommendations.size() - 1);
                    System.out.println(latest);
                }
            }
        } else {
            // 显示指定用户的推荐结果
            String userId = arguments.trim();
            RecommendationResult result = recommendationService.getLatestRecommendationForUser(userId);
            if (result == null) {
                System.out.println("用户 " + userId + " 暂无推荐结果");
            } else {
                System.out.println(result);
            }
        }
    }
}
```

### 5.7 配置文件

#### 5.7.1 application.properties

```properties
# 应用配置
spring.application.name=message-source
server.port=8080

# Kafka配置
kafka.bootstrap-servers=hadoop01:9092,hadoop02:9092,hadoop03:9092
kafka.consumer.group-id=message-source-group

# Kafka主题配置
kafka.topic.user-behavior=user-behavior
kafka.topic.item-info=item-info
kafka.topic.recommendation-results=recommendation-results

# 日志配置
logging.level.root=INFO
logging.level.com.example.messagesource=DEBUG
```

## 6. Maven依赖配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>message-source</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <java.version>11</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- Jackson for JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 7. 编译和运行

### 7.1 编译项目

```bash
# 进入项目目录
cd message-source

# 编译项目
mvn clean package
```

### 7.2 运行项目

```bash
# 运行项目
java -jar target/message-source-1.0-SNAPSHOT.jar
```

## 8. 使用示例

### 8.1 创建用户

```
> create-user 张三
创建用户成功: user6 - 张三
```

### 8.2 创建商品

```
> create-item 小米手环 电子产品 199.00 智能手环,小米,运动
创建商品成功: item16 - 小米手环
```

### 8.3 模拟用户行为

```
> simulate-behavior user1 item1 view
模拟用户行为成功: UserBehavior{userId='user1', itemId='item1', action='view', timestamp=1621234567890}
```

### 8.4 模拟用户完整行为序列

```
> simulate-sequence user1 item2
模拟用户完整行为序列成功
```

### 8.5 查看推荐结果

```
> show-recommendations user1
推荐结果 - 用户ID: user1
推荐时间: Wed May 28 10:52:30 CST 2025
推荐商品列表:
1. 商品ID: item3, 商品名称: 小米 12, 推荐分数: 0.85, 推荐理由: 因为您喜欢智能手机
2. 商品ID: item5, 商品名称: 联想 ThinkPad, 推荐分数: 0.75, 推荐理由: 热门商品推荐
3. 商品ID: item4, 商品名称: MacBook Pro, 推荐分数: 0.70, 推荐理由: 购买了相关商品
```

## 9. 系统集成

### 9.1 与Kafka集成

消息源软件通过Kafka与推荐系统进行数据交换：
- 将用户行为数据发送到`user-behavior`主题
- 将商品信息发送到`item-info`主题
- 从`recommendation-results`主题接收推荐结果

### 9.2 与Flink集成

消息源软件不直接与Flink交互，而是通过Kafka间接交互：
- Flink从Kafka消费用户行为数据
- Flink处理数据并生成推荐结果
- Flink将推荐结果发送到Kafka

## 10. 性能优化

### 10.1 批量发送

对于高频率的用户行为数据，可以采用批量发送的方式，减少Kafka的网络开销。

```java
public void sendBehaviorBatch(List<UserBehavior> behaviors) {
    for (UserBehavior behavior : behaviors) {
        kafkaTemplate.send(userBehaviorTopic, behavior.getUserId(), behavior);
    }
    System.out.println("批量发送用户行为数据到Kafka: " + behaviors.size() + "条");
}
```

### 10.2 异步处理

使用异步方式处理Kafka消息的发送和接收，避免阻塞主线程。

```java
public CompletableFuture<SendResult<String, Object>> sendBehaviorAsync(UserBehavior behavior) {
    return kafkaTemplate.send(userBehaviorTopic, behavior.getUserId(), behavior)
            .completable()
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("异步发送用户行为数据到Kafka成功: " + behavior);
                } else {
                    System.err.println("异步发送用户行为数据到Kafka失败: " + ex.getMessage());
                }
            });
}
```

### 10.3 消息压缩

配置Kafka生产者使用消息压缩，减少网络传输量。

```java
configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
```

## 11. 总结

本消息源软件工程实现了一个完整的电商推荐系统消息源，具有以下特点：

1. **功能完整**：支持商品创建、用户行为模拟和推荐结果展示等功能。
2. **易于使用**：提供简单的命令行界面，方便用户操作。
3. **与Kafka集成**：通过Kafka与推荐系统进行数据交换，实现解耦。
4. **可扩展性**：采用模块化设计，易于扩展新功能。
5. **实时性**：实时发送用户行为数据，实时接收推荐结果。

通过本消息源软件，可以模拟完整的电商推荐系统业务流程，验证推荐系统的功能和性能。
