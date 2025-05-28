package org.messagesource.generators;

import org.messagesource.models.Product;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品生成器
 * 用于创建和管理商品数据
 */
public class ProductGenerator {
    
    // 商品类别
    private static final String[] CATEGORIES = {
        "电子产品", "服装", "家居", "食品", "图书", "美妆", "运动", "玩具"
    };
    
    // 子类别映射
    private static final Map<String, String[]> SUB_CATEGORIES = new HashMap<>();
    static {
        SUB_CATEGORIES.put("电子产品", new String[]{"手机", "电脑", "相机", "耳机", "平板"});
        SUB_CATEGORIES.put("服装", new String[]{"上衣", "裤子", "裙子", "外套", "鞋子", "配饰"});
        SUB_CATEGORIES.put("家居", new String[]{"家具", "厨具", "床上用品", "装饰品", "灯具"});
        SUB_CATEGORIES.put("食品", new String[]{"零食", "饮料", "生鲜", "调味品", "速食"});
        SUB_CATEGORIES.put("图书", new String[]{"小说", "教育", "科技", "艺术", "杂志"});
        SUB_CATEGORIES.put("美妆", new String[]{"护肤", "彩妆", "香水", "美发", "美甲"});
        SUB_CATEGORIES.put("运动", new String[]{"健身器材", "运动服饰", "户外装备", "球类", "游泳"});
        SUB_CATEGORIES.put("玩具", new String[]{"积木", "玩偶", "电动玩具", "益智玩具", "模型"});
    }
    
    // 特征名称
    private static final String[] FEATURE_NAMES = {
        "价格", "质量", "外观", "功能", "性价比", "品牌知名度", "新颖度", "实用性"
    };
    
    private final Random random;
    private int nextProductId = 1;
    
    public ProductGenerator() {
        this.random = new Random();
    }
    
    /**
     * 生成随机商品
     * @return 商品对象
     */
    public Product generateRandomProduct() {
        // 生成商品ID
        String productId = "P" + String.format("%06d", nextProductId++);
        
        // 随机选择类别和子类别
        String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        String[] subCategoriesForCategory = SUB_CATEGORIES.get(category);
        String subCategory = subCategoriesForCategory[random.nextInt(subCategoriesForCategory.length)];
        
        // 生成商品名称
        String productName = generateProductName(category, subCategory);
        
        // 创建商品对象
        Product product = new Product(productId, productName, category);
        product.setSubCategory(subCategory);
        
        // 设置价格 (10-10000元)
        double price = 10 + random.nextDouble() * 9990;
        product.setPrice(Math.round(price * 100) / 100.0);
        
        // 设置初始热度
        product.setPopularity(random.nextInt(100));
        
        // 添加商品特征
        for (String featureName : FEATURE_NAMES) {
            // 特征值范围：0.1-5.0
            double featureValue = 0.1 + random.nextDouble() * 4.9;
            product.updateFeature(featureName, Math.round(featureValue * 10) / 10.0);
        }
        
        return product;
    }
    
    /**
     * 生成指定数量的随机商品
     * @param count 商品数量
     * @return 商品列表
     */
    public List<Product> generateRandomProducts(int count) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            products.add(generateRandomProduct());
        }
        return products;
    }
    
    /**
     * 生成商品名称
     * @param category 类别
     * @param subCategory 子类别
     * @return 商品名称
     */
    private String generateProductName(String category, String subCategory) {
        // 品牌名前缀
        String[] brandPrefixes = {"优品", "美佳", "卓越", "星辰", "领航", "创新", "品质", "尚品", "雅致", "智慧"};
        
        // 根据类别和子类别生成商品名称
        String brandPrefix = brandPrefixes[random.nextInt(brandPrefixes.length)];
        String randomSuffix = String.valueOf(100 + random.nextInt(900)); // 100-999的随机数
        
        return brandPrefix + subCategory + randomSuffix;
    }
    
    /**
     * 创建指定ID和名称的商品
     * @param productId 商品ID
     * @param productName 商品名称
     * @param category 类别
     * @param subCategory 子类别
     * @param price 价格
     * @return 商品对象
     */
    public Product createSpecificProduct(String productId, String productName, String category, String subCategory, double price) {
        Product product = new Product(productId, productName, category);
        product.setSubCategory(subCategory);
        product.setPrice(price);
        
        // 设置初始热度
        product.setPopularity(0);
        
        // 添加默认商品特征
        for (String featureName : FEATURE_NAMES) {
            // 默认特征值：3.0
            product.updateFeature(featureName, 3.0);
        }
        
        return product;
    }
    
    /**
     * 更新商品特征
     * @param product 商品对象
     * @param featureName 特征名称
     * @param featureValue 特征值
     */
    public void updateProductFeature(Product product, String featureName, double featureValue) {
        product.updateFeature(featureName, featureValue);
    }
    
    /**
     * 增加商品热度
     * @param product 商品对象
     * @param delta 热度增量
     */
    public void increaseProductPopularity(Product product, int delta) {
        product.increasePopularity(delta);
    }
}
