package org.messagesource.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户行为事件模型类
 * 与推荐系统中的用户行为模型保持一致，用于消息源软件中的行为模拟
 */
public class UserBehavior implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum BehaviorType {
        VIEW,      // 浏览商品
        CLICK,     // 点击商品
        CART,      // 加入购物车
        PURCHASE,  // 购买商品
        FAVORITE,  // 收藏商品
        RATE,      // 评分
        COMMENT    // 评论
    }
    
    private String userId;
    private String productId;
    private BehaviorType behaviorType;
    private long timestamp;
    private Map<String, Object> properties; // 额外属性，如停留时间、评分等
    
    public UserBehavior() {
        this.timestamp = System.currentTimeMillis();
        this.properties = new HashMap<>();
    }
    
    public UserBehavior(String userId, String productId, BehaviorType behaviorType) {
        this.userId = userId;
        this.productId = productId;
        this.behaviorType = behaviorType;
        this.timestamp = System.currentTimeMillis();
        this.properties = new HashMap<>();
    }
    
    // 添加行为属性
    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    // 获取行为属性
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    // 获取行为权重（不同行为的重要性不同）
    public double getBehaviorWeight() {
        switch (behaviorType) {
            case VIEW:
                return 1.0;
            case CLICK:
                return 2.0;
            case CART:
                return 5.0;
            case PURCHASE:
                return 10.0;
            case FAVORITE:
                return 8.0;
            case RATE:
                return 6.0;
            case COMMENT:
                return 7.0;
            default:
                return 1.0;
        }
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public BehaviorType getBehaviorType() {
        return behaviorType;
    }
    
    public void setBehaviorType(BehaviorType behaviorType) {
        this.behaviorType = behaviorType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    @Override
    public String toString() {
        return "UserBehavior{" +
                "userId='" + userId + '\'' +
                ", productId='" + productId + '\'' +
                ", behaviorType=" + behaviorType +
                ", timestamp=" + timestamp +
                ", properties=" + properties +
                '}';
    }
}
