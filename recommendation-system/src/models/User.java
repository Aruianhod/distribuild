package org.recommendation.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户模型类
 * 存储用户的基本信息和行为特征
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String userName;
    private int age;
    private String gender;
    private Map<String, Double> preferences; // 类别偏好
    private Map<String, Integer> behaviorCounts; // 行为计数
    private long lastActiveTime; // 最后活跃时间
    
    public User(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.preferences = new HashMap<>();
        this.behaviorCounts = new HashMap<>();
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    // 更新用户对某个类别的偏好
    public void updatePreference(String category, double score) {
        double currentScore = preferences.getOrDefault(category, 0.0);
        // 使用衰减因子，新的偏好有更高的权重
        double newScore = currentScore * 0.7 + score * 0.3;
        preferences.put(category, newScore);
    }
    
    // 记录用户行为
    public void recordBehavior(String behaviorType) {
        int count = behaviorCounts.getOrDefault(behaviorType, 0);
        behaviorCounts.put(behaviorType, count + 1);
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    // 获取用户对某个类别的偏好分数
    public double getPreferenceScore(String category) {
        return preferences.getOrDefault(category, 0.0);
    }
    
    // 获取用户的所有偏好
    public Map<String, Double> getAllPreferences() {
        return new HashMap<>(preferences);
    }
    
    // 获取用户的行为统计
    public Map<String, Integer> getBehaviorCounts() {
        return new HashMap<>(behaviorCounts);
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", preferences=" + preferences +
                ", behaviorCounts=" + behaviorCounts +
                ", lastActiveTime=" + lastActiveTime +
                '}';
    }
}
