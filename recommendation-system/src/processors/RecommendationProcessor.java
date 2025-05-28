package org.recommendation.processors;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.recommendation.algorithms.CollaborativeFiltering;
import org.recommendation.algorithms.ContentBasedRecommendation;
import org.recommendation.algorithms.HotItemsRecommendation;
import org.recommendation.models.Product;
import org.recommendation.models.User;
import org.recommendation.models.UserBehavior;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐生成处理器
 * 集成多种推荐算法，为用户生成个性化推荐结果
 */
public class RecommendationProcessor extends KeyedProcessFunction<String, User, Map<String, Object>> {
    private static final long serialVersionUID = 1L;
    
    // 推荐算法
    private CollaborativeFiltering collaborativeFiltering;
    private ContentBasedRecommendation contentBasedRecommendation;
    private HotItemsRecommendation hotItemsRecommendation;
    
    // 商品数据
    private List<Product> allProducts;
    
    // 用户行为状态
    private transient MapState<String, List<UserBehavior>> userBehaviorState;
    
    // 最大推荐数量
    private int maxRecommendations = 10;
    
    public RecommendationProcessor(List<Product> allProducts) {
        this.allProducts = allProducts;
        this.collaborativeFiltering = new CollaborativeFiltering();
        this.contentBasedRecommendation = new ContentBasedRecommendation();
        this.hotItemsRecommendation = new HotItemsRecommendation();
        
        // 设置各算法的最大推荐数量
        this.collaborativeFiltering.setMaxRecommendations(maxRecommendations);
        this.contentBasedRecommendation.setMaxRecommendations(maxRecommendations);
        this.hotItemsRecommendation.setMaxRecommendations(maxRecommendations);
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        // 初始化用户行为状态
        MapStateDescriptor<String, List<UserBehavior>> behaviorStateDescriptor = new MapStateDescriptor<>(
                "user-behavior-state",
                TypeInformation.of(String.class),
                TypeInformation.of(new TypeInformation.TypeHint<List<UserBehavior>>() {})
        );
        userBehaviorState = getRuntimeContext().getMapState(behaviorStateDescriptor);
    }
    
    @Override
    public void processElement(User user, Context context, Collector<Map<String, Object>> collector) throws Exception {
        String userId = user.getUserId();
        
        // 获取用户行为历史
        List<UserBehavior> userBehaviors = userBehaviorState.contains(userId) 
                ? userBehaviorState.get(userId) 
                : new ArrayList<>();
        
        // 更新协同过滤模型
        for (UserBehavior behavior : userBehaviors) {
            collaborativeFiltering.processBehavior(behavior);
        }
        
        // 更新热门商品模型
        for (UserBehavior behavior : userBehaviors) {
            hotItemsRecommendation.processBehavior(behavior);
        }
        
        // 定期计算用户相似度
        collaborativeFiltering.computeUserSimilarities();
        
        // 生成推荐结果
        Map<String, Object> recommendations = new HashMap<>();
        
        // 1. 协同过滤推荐
        List<Map.Entry<String, Double>> cfRecommendations = 
                collaborativeFiltering.recommendForUser(userId, allProducts);
        
        // 2. 基于内容的推荐
        List<Map.Entry<String, Double>> cbRecommendations = 
                contentBasedRecommendation.recommendForUser(user, allProducts);
        
        // 3. 热门商品推荐
        List<Map.Entry<String, Double>> hotRecommendations = 
                hotItemsRecommendation.getHotItems(allProducts);
        
        // 4. 用户最近浏览的商品类别的热门商品
        String recentCategory = getRecentCategory(userBehaviors);
        List<Map.Entry<String, Double>> categoryHotRecommendations = 
                recentCategory != null ? 
                hotItemsRecommendation.getHotItemsByCategory(allProducts, recentCategory) : 
                Collections.emptyList();
        
        // 合并推荐结果
        List<String> finalRecommendations = mergeRecommendations(
                cfRecommendations, 
                cbRecommendations, 
                hotRecommendations,
                categoryHotRecommendations
        );
        
        // 构建推荐结果
        recommendations.put("userId", userId);
        recommendations.put("timestamp", System.currentTimeMillis());
        recommendations.put("recommendations", finalRecommendations);
        recommendations.put("recentCategory", recentCategory);
        
        // 添加推荐理由
        Map<String, String> recommendationReasons = generateRecommendationReasons(
                finalRecommendations, 
                cfRecommendations, 
                cbRecommendations, 
                hotRecommendations,
                categoryHotRecommendations
        );
        recommendations.put("reasons", recommendationReasons);
        
        // 输出推荐结果
        collector.collect(recommendations);
    }
    
    /**
     * 获取用户最近浏览的商品类别
     * @param behaviors 用户行为列表
     * @return 最近浏览的类别，如果没有则返回null
     */
    private String getRecentCategory(List<UserBehavior> behaviors) {
        if (behaviors.isEmpty()) {
            return null;
        }
        
        // 按时间戳降序排序
        behaviors.sort(Comparator.comparing(UserBehavior::getTimestamp).reversed());
        
        // 获取最近行为对应的商品
        for (UserBehavior behavior : behaviors) {
            String productId = behavior.getProductId();
            for (Product product : allProducts) {
                if (product.getProductId().equals(productId)) {
                    return product.getCategory();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 合并多种推荐结果，去重并按优先级排序
     * @param cfRecommendations 协同过滤推荐
     * @param cbRecommendations 基于内容的推荐
     * @param hotRecommendations 热门商品推荐
     * @param categoryHotRecommendations 类别热门推荐
     * @return 合并后的推荐列表
     */
    private List<String> mergeRecommendations(
            List<Map.Entry<String, Double>> cfRecommendations,
            List<Map.Entry<String, Double>> cbRecommendations,
            List<Map.Entry<String, Double>> hotRecommendations,
            List<Map.Entry<String, Double>> categoryHotRecommendations) {
        
        // 使用LinkedHashSet保持顺序并去重
        Set<String> mergedRecommendations = new LinkedHashSet<>();
        
        // 按优先级添加推荐结果
        // 1. 协同过滤 (最高优先级)
        for (Map.Entry<String, Double> entry : cfRecommendations) {
            mergedRecommendations.add(entry.getKey());
        }
        
        // 2. 基于内容的推荐
        for (Map.Entry<String, Double> entry : cbRecommendations) {
            mergedRecommendations.add(entry.getKey());
        }
        
        // 3. 类别热门推荐
        for (Map.Entry<String, Double> entry : categoryHotRecommendations) {
            mergedRecommendations.add(entry.getKey());
        }
        
        // 4. 全局热门推荐 (最低优先级)
        for (Map.Entry<String, Double> entry : hotRecommendations) {
            mergedRecommendations.add(entry.getKey());
        }
        
        // 限制推荐数量
        return mergedRecommendations.stream()
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }
    
    /**
     * 为每个推荐商品生成推荐理由
     * @param finalRecommendations 最终推荐列表
     * @param cfRecommendations 协同过滤推荐
     * @param cbRecommendations 基于内容的推荐
     * @param hotRecommendations 热门商品推荐
     * @param categoryHotRecommendations 类别热门推荐
     * @return 商品ID到推荐理由的映射
     */
    private Map<String, String> generateRecommendationReasons(
            List<String> finalRecommendations,
            List<Map.Entry<String, Double>> cfRecommendations,
            List<Map.Entry<String, Double>> cbRecommendations,
            List<Map.Entry<String, Double>> hotRecommendations,
            List<Map.Entry<String, Double>> categoryHotRecommendations) {
        
        Map<String, String> reasons = new HashMap<>();
        
        // 创建各推荐来源的商品集合
        Set<String> cfProducts = cfRecommendations.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        
        Set<String> cbProducts = cbRecommendations.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        
        Set<String> hotProducts = hotRecommendations.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        
        Set<String> categoryHotProducts = categoryHotRecommendations.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        
        // 为每个推荐商品生成理由
        for (String productId : finalRecommendations) {
            StringBuilder reason = new StringBuilder();
            
            if (cfProducts.contains(productId)) {
                reason.append("与您喜好相似的用户也喜欢这个商品");
            } else if (cbProducts.contains(productId)) {
                reason.append("根据您的历史偏好推荐");
            } else if (categoryHotProducts.contains(productId)) {
                reason.append("您最近浏览类别的热门商品");
            } else if (hotProducts.contains(productId)) {
                reason.append("当前热门商品");
            } else {
                reason.append("为您精选的商品");
            }
            
            reasons.put(productId, reason.toString());
        }
        
        return reasons;
    }
    
    /**
     * 添加用户行为到状态
     * @param userId 用户ID
     * @param behavior 用户行为
     * @throws Exception 状态操作异常
     */
    public void addUserBehavior(String userId, UserBehavior behavior) throws Exception {
        List<UserBehavior> behaviors = userBehaviorState.contains(userId) 
                ? userBehaviorState.get(userId) 
                : new ArrayList<>();
        
        behaviors.add(behavior);
        
        // 限制行为历史大小，保留最近的行为
        if (behaviors.size() > 100) {
            behaviors.sort(Comparator.comparing(UserBehavior::getTimestamp));
            behaviors = behaviors.subList(behaviors.size() - 100, behaviors.size());
        }
        
        userBehaviorState.put(userId, behaviors);
    }
    
    /**
     * 设置最大推荐数量
     * @param maxRecommendations 最大推荐数量
     */
    public void setMaxRecommendations(int maxRecommendations) {
        this.maxRecommendations = maxRecommendations;
        this.collaborativeFiltering.setMaxRecommendations(maxRecommendations);
        this.contentBasedRecommendation.setMaxRecommendations(maxRecommendations);
        this.hotItemsRecommendation.setMaxRecommendations(maxRecommendations);
    }
}
