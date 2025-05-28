package org.recommendation.processors;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.recommendation.models.User;
import org.recommendation.models.UserBehavior;
import org.recommendation.models.Product;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户行为处理器
 * 处理实时用户行为数据，更新用户模型
 */
public class UserBehaviorProcessor extends KeyedProcessFunction<String, UserBehavior, User> {
    private static final long serialVersionUID = 1L;
    
    // 用户状态，存储用户模型
    private transient MapState<String, User> userState;
    
    // 商品映射，用于获取商品信息
    private Map<String, Product> productMap;
    
    public UserBehaviorProcessor(Map<String, Product> productMap) {
        this.productMap = productMap;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        // 初始化用户状态
        MapStateDescriptor<String, User> userStateDescriptor = new MapStateDescriptor<>(
                "user-state",
                TypeInformation.of(String.class),
                TypeInformation.of(User.class)
        );
        userState = getRuntimeContext().getMapState(userStateDescriptor);
    }
    
    @Override
    public void processElement(UserBehavior behavior, Context context, Collector<User> collector) throws Exception {
        String userId = behavior.getUserId();
        String productId = behavior.getProductId();
        
        // 获取或创建用户
        User user;
        if (userState.contains(userId)) {
            user = userState.get(userId);
        } else {
            user = new User(userId, "User-" + userId);
        }
        
        // 记录用户行为
        user.recordBehavior(behavior.getBehaviorType().toString());
        
        // 如果商品存在，更新用户对商品类别的偏好
        if (productMap.containsKey(productId)) {
            Product product = productMap.get(productId);
            String category = product.getCategory();
            
            // 根据行为类型和权重更新用户偏好
            double weight = behavior.getBehaviorWeight();
            user.updatePreference(category, weight);
            
            // 如果有子类别，也更新子类别偏好
            if (product.getSubCategory() != null && !product.getSubCategory().isEmpty()) {
                user.updatePreference(product.getSubCategory(), weight * 0.8); // 子类别权重稍低
            }
            
            // 更新商品特征偏好
            Map<String, Double> productFeatures = product.getAllFeatures();
            for (Map.Entry<String, Double> feature : productFeatures.entrySet()) {
                user.updatePreference("feature:" + feature.getKey(), feature.getValue() * weight * 0.5); // 特征权重更低
            }
        }
        
        // 更新用户状态
        userState.put(userId, user);
        
        // 输出更新后的用户模型
        collector.collect(user);
    }
}
