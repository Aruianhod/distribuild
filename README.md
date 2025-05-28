# 分布式实时电商推荐系统

## 项目概述

本项目是一个基于Kafka和Flink的分布式实时电商推荐系统，旨在为用户提供个性化、实时的商品推荐。系统通过实时处理用户行为数据，结合多种推荐算法，生成高质量的推荐结果，提升用户体验和商业转化率。

## 系统架构

系统由以下四个主要组件构成：

1. **Kafka分布式消息队列**：负责高吞吐量的消息发布与订阅，确保数据流的可靠传输
2. **Flink分布式流处理**：负责实时数据处理和推荐计算，提供高性能的流式计算能力
3. **推荐系统**：实现多种推荐算法，包括协同过滤、内容推荐和热门商品推荐等
4. **消息源软件**：模拟用户行为，管理商品信息，展示推荐结果

## 目录结构

```
distributed-realtime-recommendation/
├── kafka/                      # Kafka分布式环境配置
│   ├── config/                 # Kafka和ZooKeeper配置文件
│   ├── scripts/                # 启动和管理脚本
│   └── docker/                 # Docker部署配置
├── flink/                      # Flink分布式环境配置
│   ├── config/                 # Flink配置文件
│   ├── scripts/                # 作业提交和管理脚本
│   └── docker/                 # Docker部署配置
├── recommendation-system/      # 推荐系统实现
│   ├── src/                    # 源代码
│   │   ├── models/            # 数据模型
│   │   ├── algorithms/        # 推荐算法
│   │   └── processors/        # 数据处理器
│   └── config/                 # 推荐系统配置
├── message-source/             # 消息源软件
│   ├── src/                    # 源代码
│   │   ├── models/            # 数据模型
│   │   ├── generators/        # 数据生成器
│   │   ├── connectors/        # Kafka连接器
│   │   ├── consumers/         # 推荐结果消费者
│   │   └── web/               # Web界面
│   └── config/                 # 消息源软件配置
├── visualization/              # 可视化系统
│   ├── visualization-backend/  # 可视化后端
│   └── visualization-frontend/ # 可视化前端
├── docs/                       # 项目文档
│   ├── kafka_setup.md         # Kafka环境配置文档
│   ├── flink_setup.md         # Flink环境配置文档
│   ├── recommendation_system.md # 推荐系统文档
│   ├── message_source.md      # 消息源软件文档
│   └── visualization_design.md # 可视化设计文档
└── docker-compose.yml         # 整体系统Docker Compose配置
```

## 功能特性

- **实时数据处理**：毫秒级延迟的用户行为数据处理
- **多种推荐算法**：协同过滤、内容推荐、热门商品推荐等
- **高可用性**：分布式架构确保系统高可用和容错
- **可扩展性**：支持水平扩展以应对流量增长
- **可视化界面**：直观展示推荐结果和系统性能
- **易于部署**：提供Docker容器化部署方案

## 快速开始

### 环境要求

- Docker和Docker Compose
- JDK 11或更高版本
- Maven 3.6或更高版本
- 至少8GB RAM和4核CPU

### 部署步骤

1. 克隆代码仓库
   ```bash
   git clone https://github.com/Aruianhod/distribuild.git
   cd distribuild
   ```

2. 启动整个系统
   ```bash
   docker-compose up -d
   ```

3. 访问系统
   - 消息源软件Web界面: http://localhost:8082
   - Flink Web UI: http://localhost:8081
   - 可视化系统: http://localhost:3000

### 使用指南

1. **创建商品**：通过消息源软件Web界面创建新商品
2. **模拟用户行为**：选择用户和商品，模拟用户行为序列
3. **查看推荐结果**：在推荐结果页面查看实时更新的推荐
4. **监控系统性能**：通过可视化系统监控各组件性能指标

## 详细文档

- [Kafka分布式环境配置](docs/kafka_setup.md)
- [Flink分布式环境配置](docs/flink_setup.md)
- [推荐系统实现](docs/recommendation_system.md)
- [消息源软件实现](docs/message_source.md)
- [可视化系统设计](docs/visualization_design.md)
- [系统集成与测试](docs/system_integration_testing.md)
- [部署指南](docs/deployment_guide.md)

## 系统架构图

```
+------------------+    +------------------+    +------------------+
|   消息源软件      |    |   Kafka集群      |    |   Flink集群      |
|                  |    |                  |    |                  |
|  +-----------+   |    |  +-----------+   |    |  +-----------+   |
|  | Web界面   |   |    |  | Broker 1  |   |    |  |JobManager |   |
|  +-----------+   |    |  +-----------+   |    |  +-----------+   |
|                  |    |                  |    |                  |
|  +-----------+   |    |  +-----------+   |    |  +-----------+   |
|  |行为生成器 |   |--->|  | Broker 2  |   |--->|  |TaskManager|   |
|  +-----------+   |    |  +-----------+   |    |  +-----------+   |
|                  |    |                  |    |                  |
|  +-----------+   |    |  +-----------+   |    |  +-----------+   |
|  |结果消费者 |<--|----|-| Broker 3  |<--|----|-|推荐系统作业|   |
|  +-----------+   |    |  +-----------+   |    |  +-----------+   |
|                  |    |                  |    |                  |
+------------------+    +------------------+    +------------------+
        ^                                               |
        |                                               |
        +-----------------------------------------------+
                          推荐结果
```

## 贡献指南

1. Fork本仓库
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开Pull Request

## 许可证

本项目采用MIT许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

如有任何问题或建议，请通过GitHub Issues联系我们。
