package org.recommendation.algorithms;

import org.recommendation.models.Product;
import org.recommendation.models.User;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于内容的推荐算法实现
 * 根据商品特征和用户偏好，推荐与用户历史喜好相似的商品
 */
public class ContentBasedRecommendation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 最大推荐数量
    private int maxRecommendations = 10;
    
    /**
     * 为指定用户生成基于内容的推荐
     * @param user 用户对象
     * @param allProducts 所有可推荐的商品
     * @return 推荐商品列表及其推荐分数
     */
    public List<Map.Entry<String, Double>> recommendForUser(User user, List<Product> allProducts) {
        // 获取用户的类别偏好
        Map<String, Double> userPreferences = user.getAllPreferences();
        
        if (userPreferences.isEmpty()) {
            // 如果用户没有明确的偏好，返回空列表
            return Collections.emptyList();
        }
        
        // 计算每个商品与用户偏好的匹配度
        Map<String, Double> productScores = new HashMap<>();
        
        for (Product product : allProducts) {
            // 计算类别匹配度
            double categoryScore = userPreferences.getOrDefault(product.getCategory(), 0.0);
            double subCategoryScore = userPreferences.getOrDefault(product.getSubCategory(), 0.0);
            
            // 计算特征匹配度
            double featureScore = calculateFeatureMatchScore(userPreferences, product.getAllFeatures());
            
            // 综合评分 (可以调整权重)
            double finalScore = categoryScore * 0.4 + subCategoryScore * 0.3 + featureScore * 0.3;
            
            if (finalScore > 0) {
                productScores.put(product.getProductId(), finalScore);
            }
        }
        
        // 排序并返回推荐结果
        return productScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }
    
    /**
     * 计算用户偏好与商品特征的匹配度
     * @param userPreferences 用户偏好
     * @param productFeatures 商品特征
     * @return 匹配度分数
     */
    private double calculateFeatureMatchScore(Map<String, Double> userPreferences, Map<String, Double> productFeatures) {
        double score = 0.0;
        int matchCount = 0;
        
        // 遍历商品特征
        for (Map.Entry<String, Double> feature : productFeatures.entrySet()) {
            String featureName = feature.getKey();
            double featureValue = feature.getValue();
            
            // 如果用户对该特征有偏好
            if (userPreferences.containsKey(featureName)) {
                double preferenceValue = userPreferences.get(featureName);
                
                // 计算特征匹配度 (值越接近，匹配度越高)
                double similarity = 1.0 - Math.abs(preferenceValue - featureValue) / Math.max(1.0, Math.max(preferenceValue, featureValue));
                score += similarity;
                matchCount++;
            }
        }
        
        // 返回平均匹配度
        return matchCount > 0 ? score / matchCount : 0.0;
    }
    
    /**
     * 设置最大推荐数量
     * @param maxRecommendations 最大推荐数量
     */
    public void setMaxRecommendations(int maxRecommendations) {
        this.maxRecommendations = maxRecommendations;
    }
}
