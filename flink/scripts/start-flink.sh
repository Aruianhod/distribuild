#!/bin/bash
# Flink启动脚本
# 用于在分布式环境中启动Flink服务

# 配置文件路径
FLINK_HOME="/opt/flink"
CONFIG_DIR="$FLINK_HOME/conf"
LOG_DIR="/var/log/flink"
FLINK_CONFIG="$CONFIG_DIR/flink-conf.yaml"

# 确保日志目录存在
mkdir -p $LOG_DIR

# 根据主机名确定角色
HOSTNAME=$(hostname)
ROLE="taskmanager"

case $HOSTNAME in
  "hadoop01")
    ROLE="jobmanager"
    ;;
  "hadoop02"|"hadoop03")
    ROLE="taskmanager"
    ;;
  *)
    echo "未知主机名: $HOSTNAME，默认启动为TaskManager"
    ;;
esac

# 启动Flink服务
if [ "$ROLE" == "jobmanager" ]; then
  echo "正在启动Flink JobManager..."
  $FLINK_HOME/bin/jobmanager.sh start
  
  # 等待JobManager启动完成
  sleep 10
  
  # 检查JobManager是否成功启动
  if pgrep -f "org.apache.flink.runtime.jobmanager.JobManager" > /dev/null; then
    echo "Flink JobManager已成功启动"
  else
    echo "Flink JobManager启动失败，请检查日志"
    exit 1
  fi
else
  echo "正在启动Flink TaskManager..."
  $FLINK_HOME/bin/taskmanager.sh start
  
  # 等待TaskManager启动完成
  sleep 10
  
  # 检查TaskManager是否成功启动
  if pgrep -f "org.apache.flink.runtime.taskexecutor.TaskManagerRunner" > /dev/null; then
    echo "Flink TaskManager已成功启动"
  else
    echo "Flink TaskManager启动失败，请检查日志"
    exit 1
  fi
fi
