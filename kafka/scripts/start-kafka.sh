#!/bin/bash
# Kafka启动脚本
# 用于在分布式环境中启动Kafka服务

# 配置文件路径
KAFKA_HOME="/opt/kafka"
CONFIG_DIR="$KAFKA_HOME/config"
LOG_DIR="/var/log/kafka"
KAFKA_CONFIG="$CONFIG_DIR/server.properties"

# 确保日志目录存在
mkdir -p $LOG_DIR

# 根据主机名确定broker.id
HOSTNAME=$(hostname)
BROKER_ID=1

case $HOSTNAME in
  "hadoop01")
    BROKER_ID=1
    ;;
  "hadoop02")
    BROKER_ID=2
    ;;
  "hadoop03")
    BROKER_ID=3
    ;;
  *)
    echo "未知主机名: $HOSTNAME，使用默认broker.id=1"
    ;;
esac

# 替换配置文件中的broker.id
sed -i "s/^broker.id=.*/broker.id=$BROKER_ID/" $KAFKA_CONFIG

# 启动Kafka
echo "正在启动Kafka broker (ID: $BROKER_ID)..."
$KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_CONFIG

# 检查Kafka是否成功启动
sleep 5
if pgrep -f "kafka.Kafka" > /dev/null; then
  echo "Kafka broker已成功启动，broker.id=$BROKER_ID"
else
  echo "Kafka broker启动失败，请检查日志"
  exit 1
fi
