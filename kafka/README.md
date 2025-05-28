# Kafka分布式环境配置

本目录包含Kafka分布式环境的配置文件和部署脚本。

## 目录结构

- `config/` - Kafka和ZooKeeper配置文件
- `scripts/` - 部署和管理脚本
- `docker/` - Docker部署相关文件

## 环境要求

- 节点规模：3个节点
- 操作系统：Ubuntu 20.04或CentOS 7
- 硬件要求：
  - CPU：至少2核，推荐4核以上
  - 内存：最少8GB，推荐16GB以上
  - 存储：至少50GB可用空间
  - 网络：稳定的网络连接

## 部署架构

- Hadoop01: ZooKeeper, Kafka Broker
- Hadoop02: Kafka Broker
- Hadoop03: Kafka Broker

## 主要功能

- 消息订阅
- 消息发布
- Topic创建和管理
- 高可用性和容错性保障

## 使用说明

请参考[Kafka分布式环境配置文档](../kafka_setup.md)获取详细的安装、配置和使用说明。
