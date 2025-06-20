# 消息源软件工程实现

本目录包含消息源软件的实现代码和配置文件，用于模拟用户行为和获取推荐结果。

## 目录结构

- `src/` - 消息源软件源代码
  - `generators/` - 用户行为数据生成器
  - `consumers/` - 推荐结果消费者
  - `models/` - 数据模型定义
  - `connectors/` - Kafka连接器
- `config/` - 配置文件
- `web/` - Web界面相关文件
- `scripts/` - 部署和管理脚本

## 主要功能

1. **用户行为模拟**
   - 浏览行为模拟
   - 点击行为模拟
   - 加购行为模拟
   - 购买行为模拟

2. **商品管理**
   - 创建新商品
   - 查询商品信息
   - 更新商品属性

3. **推荐结果展示**
   - 接收推荐系统生成的推荐结果
   - 展示推荐商品及理由
   - 推荐效果统计

## 数据流

1. 生成用户行为数据
2. 将用户行为数据发送到Kafka
3. 从Kafka接收推荐结果
4. 展示推荐结果

## 使用说明

请参考[消息源软件工程设计与实现文档](../message_source.md)获取详细的设计、实现和使用说明。
