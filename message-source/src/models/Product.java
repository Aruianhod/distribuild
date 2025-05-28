package org.messagesource.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品模型类
 * 与推荐系统中的商品模型保持一致，用于消息源软件中的商品管理
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String productId;
    private String productName;
    private String category;
    private String subCategory;
    private double price;
    private Map<String, Double> features; // 商品特征向量
    private int popularity; // 商品热度
    private long createTime; // 创建时间
    private long updateTime; // 更新时间
    
    public Product() {
        this.features = new HashMap<>();
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
    }
    
    public Product(String productId, String productName, String category) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.features = new HashMap<>();
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
    }
    
    // 更新商品特征
    public void updateFeature(String featureName, double value) {
        features.put(featureName, value);
        this.updateTime = System.currentTimeMillis();
    }
    
    // 增加商品热度
    public void increasePopularity(int delta) {
        this.popularity += delta;
        this.updateTime = System.currentTimeMillis();
    }
    
    // 获取商品特征值
    public double getFeatureValue(String featureName) {
        return features.getOrDefault(featureName, 0.0);
    }
    
    // 获取商品的所有特征
    public Map<String, Double> getAllFeatures() {
        return new HashMap<>(features);
    }
    
    // Getters and Setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSubCategory() {
        return subCategory;
    }
    
    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public int getPopularity() {
        return popularity;
    }
    
    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    public Map<String, Double> getFeatures() {
        return features;
    }
    
    public void setFeatures(Map<String, Double> features) {
        this.features = features;
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", category='" + category + '\'' +
                ", subCategory='" + subCategory + '\'' +
                ", price=" + price +
                ", features=" + features +
                ", popularity=" + popularity +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
