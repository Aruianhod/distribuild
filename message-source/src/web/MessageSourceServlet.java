package org.messagesource.web;

import org.messagesource.MessageSourceApp;
import org.messagesource.models.Product;
import org.messagesource.models.UserBehavior;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Web界面Servlet
 * 提供消息源软件的Web界面，用于展示推荐结果和模拟用户行为
 */
public class MessageSourceServlet extends HttpServlet {
    
    private final MessageSourceApp app;
    
    public MessageSourceServlet(MessageSourceApp app) {
        this.app = app;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        
        if (path == null || path.equals("/")) {
            // 主页
            showHomePage(resp);
        } else if (path.equals("/products")) {
            // 商品列表页
            showProductsPage(resp);
        } else if (path.equals("/users")) {
            // 用户列表页
            showUsersPage(resp);
        } else if (path.equals("/recommendations")) {
            // 推荐结果页
            showRecommendationsPage(resp);
        } else if (path.startsWith("/user/")) {
            // 用户详情页
            String userId = path.substring("/user/".length());
            showUserDetailPage(resp, userId);
        } else if (path.startsWith("/product/")) {
            // 商品详情页
            String productId = path.substring("/product/".length());
            showProductDetailPage(resp, productId);
        } else {
            // 404页面
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().println("<h1>404 Not Found</h1>");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        
        if (path.equals("/create-product")) {
            // 创建商品
            String productName = req.getParameter("productName");
            String category = req.getParameter("category");
            String subCategory = req.getParameter("subCategory");
            double price = Double.parseDouble(req.getParameter("price"));
            
            Product product = app.createAndSendProduct(productName, category, subCategory, price);
            
            resp.sendRedirect("/products");
        } else if (path.equals("/generate-behavior")) {
            // 生成用户行为
            String userId = req.getParameter("userId");
            String productId = req.getParameter("productId");
            
            List<UserBehavior> behaviors = app.generateAndSendBehaviorSequence(userId, productId);
            
            resp.sendRedirect("/user/" + userId);
        } else {
            // 404页面
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().println("<h1>404 Not Found</h1>");
        }
    }
    
    /**
     * 显示主页
     * @param resp HTTP响应
     * @throws IOException IO异常
     */
    private void showHomePage(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>分布式实时电商推荐系统 - 消息源软件</title>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }");
        out.println("h1 { color: #333; }");
        out.println("nav { margin-bottom: 20px; }");
        out.println("nav a { margin-right: 10px; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>分布式实时电商推荐系统 - 消息源软件</h1>");
        out.println("<nav>");
        out.println("<a href=\"/\">首页</a>");
        out.println("<a href=\"/products\">商品管理</a>");
        out.println("<a href=\"/users\">用户管理</a>");
        out.println("<a href=\"/recommendations\">推荐结果</a>");
        out.println("</nav>");
        out.println("<div>");
        out.println("<h2>系统概述</h2>");
        out.println("<p>本系统是一个分布式实时电商推荐系统，由以下组件组成：</p>");
        out.println("<ul>");
        out.println("<li>Kafka分布式消息队列：负责消息的发布与订阅</li>");
        out.println("<li>Flink分布式流处理：负责实时数据处理和推荐计算</li>");
        out.println("<li>推荐系统：实现多种推荐算法，生成个性化推荐结果</li>");
        out.println("<li>消息源软件：模拟用户行为，管理商品，展示推荐结果</li>");
        out.println("</ul>");
        out.println("<h2>功能导航</h2>");
        out.println("<ul>");
        out.println("<li><a href=\"/products\">商品管理</a>：查看和创建商品</li>");
        out.println("<li><a href=\"/users\">用户管理</a>：查看用户和模拟用户行为</li>");
        out.println("<li><a href=\"/recommendations\">推荐结果</a>：查看实时推荐结果</li>");
        out.println("</ul>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }
    
    /**
     * 显示商品列表页
     * @param resp HTTP响应
     * @throws IOException IO异常
     */
    private void showProductsPage(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>商品管理 - 分布式实时电商推荐系统</title>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }");
        out.println("h1 { color: #333; }");
        out.println("nav { margin-bottom: 20px; }");
        out.println("nav a { margin-right: 10px; }");
        out.println("table { border-collapse: collapse; width: 100%; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #f2f2f2; }");
        out.println("tr:nth-child(even) { background-color: #f9f9f9; }");
        out.println("form { margin-top: 20px; border: 1px solid #ddd; padding: 20px; }");
        out.println("label { display: block; margin-bottom: 5px; }");
        out.println("input, select { margin-bottom: 10px; padding: 5px; width: 300px; }");
        out.println("button { padding: 8px 16px; background-color: #4CAF50; color: white; border: none; cursor: pointer; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>商品管理</h1>");
        out.println("<nav>");
        out.println("<a href=\"/\">首页</a>");
        out.println("<a href=\"/products\">商品管理</a>");
        out.println("<a href=\"/users\">用户管理</a>");
        out.println("<a href=\"/recommendations\">推荐结果</a>");
        out.println("</nav>");
        
        // 商品列表
        Map<String, Product> products = app.getAllProducts();
        out.println("<h2>商品列表</h2>");
        out.println("<table>");
        out.println("<tr><th>ID</th><th>名称</th><th>类别</th><th>子类别</th><th>价格</th><th>热度</th><th>操作</th></tr>");
        
        for (Product product : products.values()) {
            out.println("<tr>");
            out.println("<td>" + product.getProductId() + "</td>");
            out.println("<td>" + product.getProductName() + "</td>");
            out.println("<td>" + product.getCategory() + "</td>");
            out.println("<td>" + product.getSubCategory() + "</td>");
            out.println("<td>" + product.getPrice() + "</td>");
            out.println("<td>" + product.getPopularity() + "</td>");
            out.println("<td><a href=\"/product/" + product.getProductId() + "\">详情</a></td>");
            out.println("</tr>");
        }
        
        out.println("</table>");
        
        // 创建商品表单
        out.println("<h2>创建新商品</h2>");
        out.println("<form action=\"/create-product\" method=\"post\">");
        out.println("<label for=\"productName\">商品名称:</label>");
        out.println("<input type=\"text\" id=\"productName\" name=\"productName\" required>");
        
        out.println("<label for=\"category\">类别:</label>");
        out.println("<select id=\"category\" name=\"category\" required>");
        out.println("<option value=\"电子产品\">电子产品</option>");
        out.println("<option value=\"服装\">服装</option>");
        out.println("<option value=\"家居\">家居</option>");
        out.println("<option value=\"食品\">食品</option>");
        out.println("<option value=\"图书\">图书</option>");
        out.println("<option value=\"美妆\">美妆</option>");
        out.println("<option value=\"运动\">运动</option>");
        out.println("<option value=\"玩具\">玩具</option>");
        out.println("</select>");
        
        out.println("<label for=\"subCategory\">子类别:</label>");
        out.println("<input type=\"text\" id=\"subCategory\" name=\"subCategory\" required>");
        
        out.println("<label for=\"price\">价格:</label>");
        out.println("<input type=\"number\" id=\"price\" name=\"price\" step=\"0.01\" min=\"0\" required>");
        
        out.println("<button type=\"submit\">创建商品</button>");
        out.println("</form>");
        
        out.println("</body>");
        out.println("</html>");
    }
    
    /**
     * 显示用户列表页
     * @param resp HTTP响应
     * @throws IOException IO异常
     */
    private void showUsersPage(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>用户管理 - 分布式实时电商推荐系统</title>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }");
        out.println("h1 { color: #333; }");
        out.println("nav { margin-bottom: 20px; }");
        out.println("nav a { margin-right: 10px; }");
        out.println("table { border-collapse: collapse; width: 100%; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #f2f2f2; }");
        out.println("tr:nth-child(even) { background-color: #f9f9f9; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>用户管理</h1>");
        out.println("<nav>");
        out.println("<a href=\"/\">首页</a>");
        out.println("<a href=\"/products\">商品管理</a>");
        out.println("<a href=\"/users\">用户管理</a>");
        out.println("<a href=\"/recommendations\">推荐结果</a>");
        out.println("</nav>");
        
        // 用户列表
        List<String> userIds = app.getAllUserIds();
        out.println("<h2>用户列表</h2>");
        out.println("<table>");
        out.println("<tr><th>用户ID</th><th>操作</th></tr>");
        
        for (String userId : userIds) {
            out.println("<tr>");
            out.println("<td>" + userId + "</td>");
            out.println("<td><a href=\"/user/" + userId + "\">详情</a></td>");
            out.println("</tr>");
        }
        
        out.println("</table>");
        
        out.println("</body>");
        out.println("</html>");
    }
    
    /**
     * 显示推荐结果页
     * @param resp HTTP响应
     * @throws IOException IO异常
     */
    private void showRecommendationsPage(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>推荐结果 - 分布式实时电商推荐系统</title>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }");
        out.println("h1 { color: #333; }");
        out.println("nav { margin-bottom: 20px; }");
        out.println("nav a { margin-right: 10px; }");
        out.println("table { border-collapse: collapse; width: 100%; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #f2f2f2; }");
        out.println("tr:nth-child(even) { background-color: #f9f9f9; }");
        out.println(".recommendation-card { border: 1px solid #ddd; padding: 15px; margin-bottom: 20px; }");
        out.println(".recommendation-card h3 { margin-top: 0; }");
        out.println("</style>");
        out.println("<script>");
        out.println("function refreshPage() {");
        out.println("  location.reload();");
        out.println("}");
        out.println("setInterval(refreshPage, 10000);"); // 每10秒刷新一次
        out.println("</script>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>推荐结果</h1>");
        out.println("<nav>");
        out.println("<a href=\"/\">首页</a>");
        out.println("<a href=\"/products\">商品管理</a>");
        out.println("<a href=\"/users\">用户管理</a>");
        out.println("<a href=\"/recommendations\">推荐结果</a>");
        out.println("</nav>");
        
        // 推荐结果
        Map<String, Map<String, Object>> recommendations = app.getAllRecommendations();
        out.println("<h2>实时推荐结果</h2>");
        out.println("<p>当前共有 " + recommendations.size() + " 个用户的推荐结果。页面每10秒自动刷新。</p>");
        
        if (recommendations.isEmpty()) {
            out.println("<p>暂无推荐结果，请先模拟一些用户行为。</p>");
        } else {
            for (Map.Entry<String, Map<String, Object>> entry : recommendations.entrySet()) {
                String userId = entry.getKey();
                Map<String, Object> recommendation = entry.getValue();
                
                out.println("<div class=\"recommendation-card\">");
                out.println("<h3>用户: " + userId + "</h3>");
                out.println("<p>推荐时间: " + new java.util.Date((Long) recommendation.get("timestamp")) + "</p>");
                
                @SuppressWarnings("unchecked")
                List<String> recommendedProducts = (List<String>) recommendation.get("recommendations");
                if (recommendedProducts != null && !recommendedProducts.isEmpty()) {
                    out.println("<h4>推荐商品:</h4>");
                    out.println("<ul>");
                    for (String productId : recommendedProducts) {
                        Product product = app.getAllProducts().get(productId);
                        if (product != null) {
                            out.println("<li>" + product.getProductName() + " (ID: " + productId + ")</li>");
                        } else {
                            out.println("<li>商品ID: " + productId + "</li>");
                        }
                    }
                    out.println("</ul>");
                } else {
                    out.println("<p>暂无推荐商品</p>");
                }
                
                out.println("<p><a href=\"/user/" + userId + "\">查看用户详情</a></p>");
                out.println("</div>");
            }
        }
        
        out.println("</body>");
        out.println("</html>");
    }
    
    /**
     * 显示用户详情页
     * @param resp HTTP响应
     * @param userId 用户ID
     * @throws IOException IO异常
     */
    private void showUserDetailPage(HttpServletResponse resp, String userId) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>用户详情 - 分布式实时电商推荐系统</title>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }");
        out.println("h1 { color: #333; }");
        out.println("nav { margin-bottom: 20px; }");
        out.println("nav a { margin-right: 10px; }");
        out.println("table { border-collapse: collapse; width: 100%; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #f2f2f2; }");
        out.println("tr:nth-child(even) { background-color: #f9f9f9; }");
        out.println("form { margin-top: 20px; border: 1px solid #ddd; padding: 20px; }");
        out.println("label { display: block; margin-bottom: 5px; }");
        out.println("select { margin-bottom: 10px; padding: 5px; width: 300px; }");
        out.println("button { padding: 8px 16px; background-color: #4CAF50; color: white; border: none; cursor: pointer; }");
        out.println(".recommendation-card { border: 1px solid #ddd; padding: 15px; margin-bottom: 20px; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>用户详情: " + userId + "</h1>");
        out.println("<nav>");
        out.println("<a href=\"/\">首页</a>");
        out.println("<a href=\"/products\">商品管理</a>");
        out.println("<a href=\"/users\">用户管理</a>");
        out.println("<a href=\"/recommendations\">推荐结果</a>");
        out.println("</nav>");
        
        // 用户推荐结果
        Map<String, Object> recommendation = app.getRecommendationForUser(userId);
        out.println("<h2>推荐结果</h2>");
        
        if (recommendation != null) {
            out.println("<div class=\"recommendation-card\">");
            out.println("<p>推荐时间: " + new java.util.Date((Long) recommendation.get("timestamp")) + "</p>");
            
            @SuppressWarnings("unchecked")
            List<String> recommendedProducts = (List<String>) recommendation.get("recommendations");
            if (recommendedProducts != null && !recommendedProducts.isEmpty()) {
                out.println("<h4>推荐商品:</h4>");
                out.println("<ul>");
                for (String productId : recommendedProducts) {
                    Product product = app.getAllProducts().get(productId);
                    if (product != null) {
                        out.println("<li>" + product.getProductName() + " (ID: " + productId + ")</li>");
                    } else {
                        out.println("<li>商品ID: " + productId + "</li>");
                    }
                }
                out.println("</ul>");
            } else {
                out.println("<p>暂无推荐商品</p>");
            }
            out.println("</div>");
        } else {
            out.println("<p>暂无推荐结果，请先模拟一些用户行为。</p>");
        }
        
        // 模拟用户行为表单
        out.println("<h2>模拟用户行为</h2>");
        out.println("<form action=\"/generate-behavior\" method=\"post\">");
        out.println("<input type=\"hidden\" name=\"userId\" value=\"" + userId + "\">");
        
        out.println("<label for=\"productId\">选择商品:</label>");
        out.println("<select id=\"productId\" name=\"productId\" required>");
        
        Map<String, Product> products = app.getAllProducts();
        for (Product product : products.values()) {
            out.println("<option value=\"" + product.getProductId() + "\">" + product.getProductName() + " (" + product.getCategory() + ")</option>");
        }
        
        out.println("</select>");
        
        out.println("<p>点击下面的按钮将模拟用户从浏览到购买的完整行为序列。</p>");
        out.println("<button type=\"submit\">模拟用户行为</button>");
        out.println("</form>");
        
        out.println("</body>");
        out.println("</html>");
    }
    
    /**
     * 显示商品详情页
     * @param resp HTTP响应
     * @param productId 商品ID
     * @throws IOException IO异常
     */
    private void showProductDetailPage(HttpServletResponse resp, String productId) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>商品详情 - 分布式实时电商推荐系统</title>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }");
        out.println("h1 { color: #333; }");
        out.println("nav { margin-bottom: 20px; }");
        out.println("nav a { margin-right: 10px; }");
        out.println("table { border-collapse: collapse; width: 100%; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #f2f2f2; }");
        out.println("tr:nth-child(even) { background-color: #f9f9f9; }");
        out.println(".product-card { border: 1px solid #ddd; padding: 15px; margin-bottom: 20px; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        
        Product product = app.getAllProducts().get(productId);
        
        if (product != null) {
            out.println("<h1>商品详情: " + product.getProductName() + "</h1>");
            out.println("<nav>");
            out.println("<a href=\"/\">首页</a>");
            out.println("<a href=\"/products\">商品管理</a>");
            out.println("<a href=\"/users\">用户管理</a>");
            out.println("<a href=\"/recommendations\">推荐结果</a>");
            out.println("</nav>");
            
            out.println("<div class=\"product-card\">");
            out.println("<h2>" + product.getProductName() + "</h2>");
            out.println("<p><strong>ID:</strong> " + product.getProductId() + "</p>");
            out.println("<p><strong>类别:</strong> " + product.getCategory() + "</p>");
            out.println("<p><strong>子类别:</strong> " + product.getSubCategory() + "</p>");
            out.println("<p><strong>价格:</strong> " + product.getPrice() + "</p>");
            out.println("<p><strong>热度:</strong> " + product.getPopularity() + "</p>");
            out.println("<p><strong>创建时间:</strong> " + new java.util.Date(product.getCreateTime()) + "</p>");
            out.println("<p><strong>更新时间:</strong> " + new java.util.Date(product.getUpdateTime()) + "</p>");
            
            out.println("<h3>商品特征</h3>");
            out.println("<table>");
            out.println("<tr><th>特征名称</th><th>特征值</th></tr>");
            
            for (Map.Entry<String, Double> feature : product.getFeatures().entrySet()) {
                out.println("<tr>");
                out.println("<td>" + feature.getKey() + "</td>");
                out.println("<td>" + feature.getValue() + "</td>");
                out.println("</tr>");
            }
            
            out.println("</table>");
            out.println("</div>");
        } else {
            out.println("<h1>商品不存在</h1>");
            out.println("<nav>");
            out.println("<a href=\"/\">首页</a>");
            out.println("<a href=\"/products\">商品管理</a>");
            out.println("<a href=\"/users\">用户管理</a>");
            out.println("<a href=\"/recommendations\">推荐结果</a>");
            out.println("</nav>");
            out.println("<p>找不到ID为 " + productId + " 的商品。</p>");
        }
        
        out.println("</body>");
        out.println("</html>");
    }
}
