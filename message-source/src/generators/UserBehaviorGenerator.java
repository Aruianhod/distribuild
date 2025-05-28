package org.messagesource.generators;

import org.messagesource.models.Product;
import org.messagesource.models.UserBehavior;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户行为生成器
 * 用于模拟用户的各种行为，如浏览、点击、加购、购买等
 */
public class UserBehaviorGenerator {
    
    private final List<String> userIds;
    private final Map<String, Product> productMap;
    private final Random random;
    
    // 行为类型概率分布 (总和为1.0)
    private final Map<UserBehavior.BehaviorType, Double> behaviorProbabilities;
    
    public UserBehaviorGenerator(List<String> userIds, Map<String, Product> productMap) {
        this.userIds = userIds;
        this.productMap = productMap;
        this.random = new Random();
        
        // 初始化行为类型概率分布
        this.behaviorProbabilities = new HashMap<>();
        behaviorProbabilities.put(UserBehavior.BehaviorType.VIEW, 0.5);     // 50% 浏览
        behaviorProbabilities.put(UserBehavior.BehaviorType.CLICK, 0.25);   // 25% 点击
        behaviorProbabilities.put(UserBehavior.BehaviorType.CART, 0.1);     // 10% 加购
        behaviorProbabilities.put(UserBehavior.BehaviorType.PURCHASE, 0.05); // 5% 购买
        behaviorProbabilities.put(UserBehavior.BehaviorType.FAVORITE, 0.05); // 5% 收藏
        behaviorProbabilities.put(UserBehavior.BehaviorType.RATE, 0.03);     // 3% 评分
        behaviorProbabilities.put(UserBehavior.BehaviorType.COMMENT, 0.02);  // 2% 评论
    }
    
    /**
     * 生成随机用户行为
     * @return 用户行为对象
     */
    public UserBehavior generateRandomBehavior() {
        // 随机选择用户
        String userId = userIds.get(random.nextInt(userIds.size()));
        
        // 随机选择商品
        List<String> productIds = new ArrayList<>(productMap.keySet());
        String productId = productIds.get(random.nextInt(productIds.size()));
        
        // 根据概率分布随机选择行为类型
        UserBehavior.BehaviorType behaviorType = selectRandomBehaviorType();
        
        // 创建用户行为对象
        UserBehavior behavior = new UserBehavior(userId, productId, behaviorType);
        
        // 根据行为类型添加额外属性
        addBehaviorProperties(behavior);
        
        return behavior;
    }
    
    /**
     * 生成特定用户对特定商品的行为
     * @param userId 用户ID
     * @param productId 商品ID
     * @param behaviorType 行为类型
     * @return 用户行为对象
     */
    public UserBehavior generateSpecificBehavior(String userId, String productId, UserBehavior.BehaviorType behaviorType) {
        // 创建用户行为对象
        UserBehavior behavior = new UserBehavior(userId, productId, behaviorType);
        
        // 添加额外属性
        addBehaviorProperties(behavior);
        
        return behavior;
    }
    
    /**
     * 生成用户行为序列
     * 模拟用户从浏览到购买的完整行为链路
     * @param userId 用户ID
     * @param productId 商品ID
     * @return 用户行为序列
     */
    public List<UserBehavior> generateBehaviorSequence(String userId, String productId) {
        List<UserBehavior> sequence = new ArrayList<>();
        
        // 1. 浏览商品
        sequence.add(generateSpecificBehavior(userId, productId, UserBehavior.BehaviorType.VIEW));
        
        // 2. 点击商品详情
        if (random.nextDouble() < 0.8) { // 80%的概率点击
            sequence.add(generateSpecificBehavior(userId, productId, UserBehavior.BehaviorType.CLICK));
            
            // 3. 加入购物车
            if (random.nextDouble() < 0.5) { // 50%的概率加购
                sequence.add(generateSpecificBehavior(userId, productId, UserBehavior.BehaviorType.CART));
                
                // 4. 购买
                if (random.nextDouble() < 0.6) { // 60%的概率购买
                    sequence.add(generateSpecificBehavior(userId, productId, UserBehavior.BehaviorType.PURCHASE));
                    
                    // 5. 评分
                    if (random.nextDouble() < 0.3) { // 30%的概率评分
                        sequence.add(generateSpecificBehavior(userId, productId, UserBehavior.BehaviorType.RATE));
                        
                        // 6. 评论
                        if (random.nextDouble() < 0.5) { // 50%的概率评论
                            sequence.add(generateSpecificBehavior(userId, productId, UserBehavior.BehaviorType.COMMENT));
                        }
                    }
                }
            }
            
            // 收藏 (独立于购买路径)
            if (random.nextDouble() < 0.2) { // 20%的概率收藏
                sequence.add(generateSpecificBehavior(userId, productId, UserBehavior.BehaviorType.FAVORITE));
            }
        }
        
        return sequence;
    }
    
    /**
     * 根据概率分布随机选择行为类型
     * @return 行为类型
     */
    private UserBehavior.BehaviorType selectRandomBehaviorType() {
        double rand = random.nextDouble();
        double cumulativeProbability = 0.0;
        
        for (Map.Entry<UserBehavior.BehaviorType, Double> entry : behaviorProbabilities.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (rand <= cumulativeProbability) {
                return entry.getKey();
            }
        }
        
        // 默认返回浏览行为
        return UserBehavior.BehaviorType.VIEW;
    }
    
    /**
     * 根据行为类型添加额外属性
     * @param behavior 用户行为对象
     */
    private void addBehaviorProperties(UserBehavior behavior) {
        switch (behavior.getBehaviorType()) {
            case VIEW:
                // 添加浏览时长 (5-120秒)
                behavior.addProperty("duration", ThreadLocalRandom.current().nextInt(5, 121));
                break;
                
            case CLICK:
                // 添加点击位置
                behavior.addProperty("position", ThreadLocalRandom.current().nextInt(1, 21));
                break;
                
            case CART:
                // 添加数量
                behavior.addProperty("quantity", ThreadLocalRandom.current().nextInt(1, 6));
                break;
                
            case PURCHASE:
                // 添加数量和支付方式
                behavior.addProperty("quantity", ThreadLocalRandom.current().nextInt(1, 6));
                String[] paymentMethods = {"credit_card", "debit_card", "paypal", "alipay", "wechat_pay"};
                behavior.addProperty("payment_method", paymentMethods[random.nextInt(paymentMethods.length)]);
                break;
                
            case RATE:
                // 添加评分 (1-5星)
                behavior.addProperty("rating", ThreadLocalRandom.current().nextInt(1, 6));
                break;
                
            case COMMENT:
                // 添加评论长度
                behavior.addProperty("comment_length", ThreadLocalRandom.current().nextInt(10, 201));
                break;
                
            case FAVORITE:
                // 添加收藏夹ID
                behavior.addProperty("favorite_list_id", "list_" + ThreadLocalRandom.current().nextInt(1, 6));
                break;
        }
    }
}
