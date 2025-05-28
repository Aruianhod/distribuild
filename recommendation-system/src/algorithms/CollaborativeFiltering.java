package org.recommendation.algorithms;

import org.recommendation.models.Product;
import org.recommendation.models.User;
import org.recommendation.models.UserBehavior;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 协同过滤推荐算法实现
 * 基于用户行为数据，计算用户之间的相似度，推荐相似用户喜欢的商品
 */
public class CollaborativeFiltering implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 用户-商品评分矩阵
    private Map<String, Map<String, Double>> userProductMatrix = new HashMap<>();
    
    // 用户相似度矩阵
    private Map<String, Map<String, Double>> userSimilarityMatrix = new HashMap<>();
    
    // 最大推荐数量
    private int maxRecommendations = 10;
    
    /**
     * 处理用户行为数据，更新用户-商品评分矩阵
     * @param behavior 用户行为数据
     */
    public void processBehavior(UserBehavior behavior) {
        String userId = behavior.getUserId();
        String productId = behavior.getProductId();
        double score = behavior.getBehaviorWeight();
        
        // 获取或创建用户的评分映射
        Map<String, Double> userScores = userProductMatrix.getOrDefault(userId, new HashMap<>());
        
        // 更新评分，如果已存在则取较高值
        double currentScore = userScores.getOrDefault(productId, 0.0);
        userScores.put(productId, Math.max(currentScore, score));
        
        // 更新用户-商品矩阵
        userProductMatrix.put(userId, userScores);
    }
    
    /**
     * 计算所有用户之间的相似度
     */
    public void computeUserSimilarities() {
        for (String user1 : userProductMatrix.keySet()) {
            Map<String, Double> similarities = new HashMap<>();
            
            for (String user2 : userProductMatrix.keySet()) {
                if (user1.equals(user2)) continue;
                
                double similarity = computeSimilarity(
                        userProductMatrix.get(user1),
                        userProductMatrix.get(user2)
                );
                
                if (similarity > 0) {
                    similarities.put(user2, similarity);
                }
            }
            
            userSimilarityMatrix.put(user1, similarities);
        }
    }
    
    /**
     * 计算两个用户之间的余弦相似度
     * @param scores1 用户1的评分映射
     * @param scores2 用户2的评分映射
     * @return 相似度分数
     */
    private double computeSimilarity(Map<String, Double> scores1, Map<String, Double> scores2) {
        // 找出两个用户共同评分的商品
        Set<String> commonProducts = new HashSet<>(scores1.keySet());
        commonProducts.retainAll(scores2.keySet());
        
        if (commonProducts.isEmpty()) {
            return 0.0;
        }
        
        // 计算点积
        double dotProduct = 0.0;
        for (String product : commonProducts) {
            dotProduct += scores1.get(product) * scores2.get(product);
        }
        
        // 计算向量模长
        double norm1 = Math.sqrt(scores1.values().stream().mapToDouble(v -> v * v).sum());
        double norm2 = Math.sqrt(scores2.values().stream().mapToDouble(v -> v * v).sum());
        
        // 计算余弦相似度
        return dotProduct / (norm1 * norm2);
    }
    
    /**
     * 为指定用户生成推荐
     * @param userId 用户ID
     * @param allProducts 所有可推荐的商品
     * @return 推荐商品列表及其推荐分数
     */
    public List<Map.Entry<String, Double>> recommendForUser(String userId, List<Product> allProducts) {
        // 如果用户不存在，返回空列表
        if (!userProductMatrix.containsKey(userId)) {
            return Collections.emptyList();
        }
        
        // 获取用户已评分的商品
        Set<String> ratedProducts = userProductMatrix.get(userId).keySet();
        
        // 获取与用户相似的其他用户
        Map<String, Double> similarUsers = userSimilarityMatrix.getOrDefault(userId, Collections.emptyMap());
        
        // 计算每个未评分商品的预测分数
        Map<String, Double> productScores = new HashMap<>();
        
        for (Product product : allProducts) {
            String productId = product.getProductId();
            
            // 跳过用户已评分的商品
            if (ratedProducts.contains(productId)) {
                continue;
            }
            
            double weightedSum = 0.0;
            double similaritySum = 0.0;
            
            // 遍历相似用户
            for (Map.Entry<String, Double> entry : similarUsers.entrySet()) {
                String similarUserId = entry.getKey();
                double similarity = entry.getValue();
                
                // 获取相似用户对该商品的评分
                Map<String, Double> similarUserScores = userProductMatrix.get(similarUserId);
                if (similarUserScores.containsKey(productId)) {
                    double score = similarUserScores.get(productId);
                    weightedSum += similarity * score;
                    similaritySum += similarity;
                }
            }
            
            // 计算预测分数
            if (similaritySum > 0) {
                double predictedScore = weightedSum / similaritySum;
                productScores.put(productId, predictedScore);
            }
        }
        
        // 排序并返回推荐结果
        return productScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }
    
    /**
     * 设置最大推荐数量
     * @param maxRecommendations 最大推荐数量
     */
    public void setMaxRecommendations(int maxRecommendations) {
        this.maxRecommendations = maxRecommendations;
    }
    
    /**
     * 获取用户-商品评分矩阵
     * @return 用户-商品评分矩阵
     */
    public Map<String, Map<String, Double>> getUserProductMatrix() {
        return userProductMatrix;
    }
    
    /**
     * 获取用户相似度矩阵
     * @return 用户相似度矩阵
     */
    public Map<String, Map<String, Double>> getUserSimilarityMatrix() {
        return userSimilarityMatrix;
    }
}
