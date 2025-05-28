package org.recommendation.algorithms;

import org.recommendation.models.Product;
import org.recommendation.models.UserBehavior;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实时热门商品推荐算法实现
 * 基于实时用户行为数据，计算商品热度，推荐当前最热门的商品
 */
public class HotItemsRecommendation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 商品热度分数映射
    private Map<String, Double> productHeatScores = new HashMap<>();
    
    // 行为时间窗口 (毫秒)
    private long timeWindow = 3600000; // 默认1小时
    
    // 最大推荐数量
    private int maxRecommendations = 10;
    
    // 热度衰减因子 (随时间衰减)
    private double decayFactor = 0.95;
    
    /**
     * 处理用户行为数据，更新商品热度分数
     * @param behavior 用户行为数据
     */
    public void processBehavior(UserBehavior behavior) {
        String productId = behavior.getProductId();
        double weight = behavior.getBehaviorWeight();
        
        // 获取当前热度分数
        double currentScore = productHeatScores.getOrDefault(productId, 0.0);
        
        // 更新热度分数 (累加行为权重)
        productHeatScores.put(productId, currentScore + weight);
    }
    
    /**
     * 定期衰减所有商品的热度分数
     * 应该定期调用此方法，例如每小时
     */
    public void decayAllScores() {
        for (String productId : productHeatScores.keySet()) {
            double currentScore = productHeatScores.get(productId);
            productHeatScores.put(productId, currentScore * decayFactor);
        }
    }
    
    /**
     * 清理过期的热度数据
     * 移除热度分数低于阈值的商品
     * @param threshold 热度阈值
     */
    public void cleanupLowScores(double threshold) {
        productHeatScores.entrySet().removeIf(entry -> entry.getValue() < threshold);
    }
    
    /**
     * 获取当前热门商品推荐
     * @param allProducts 所有可推荐的商品
     * @return 推荐商品列表及其热度分数
     */
    public List<Map.Entry<String, Double>> getHotItems(List<Product> allProducts) {
        // 创建商品ID到商品对象的映射
        Map<String, Product> productMap = allProducts.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
        
        // 过滤出存在于allProducts中的商品，并按热度排序
        return productHeatScores.entrySet().stream()
                .filter(entry -> productMap.containsKey(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取特定类别的热门商品推荐
     * @param allProducts 所有可推荐的商品
     * @param category 商品类别
     * @return 推荐商品列表及其热度分数
     */
    public List<Map.Entry<String, Double>> getHotItemsByCategory(List<Product> allProducts, String category) {
        // 创建商品ID到商品对象的映射
        Map<String, Product> productMap = allProducts.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toMap(Product::getProductId, p -> p));
        
        // 过滤出指定类别的商品，并按热度排序
        return productHeatScores.entrySet().stream()
                .filter(entry -> productMap.containsKey(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }
    
    // Getters and Setters
    public void setTimeWindow(long timeWindow) {
        this.timeWindow = timeWindow;
    }
    
    public void setMaxRecommendations(int maxRecommendations) {
        this.maxRecommendations = maxRecommendations;
    }
    
    public void setDecayFactor(double decayFactor) {
        this.decayFactor = decayFactor;
    }
    
    public Map<String, Double> getProductHeatScores() {
        return productHeatScores;
    }
}
