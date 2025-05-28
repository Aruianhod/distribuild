# 分布式实时电商推荐系统本地部署指南

## 1. 部署环境需求分析

在将分布式实时电商推荐系统部署到本地PC之前，我们需要分析系统对环境的要求和用户PC的实际情况。

### 1.1 系统环境需求

根据系统设计，完整部署需要以下环境：

1. **硬件需求**
   - CPU：至少2核，推荐4核以上
   - 内存：至少8GB，推荐16GB以上
   - 存储：至少50GB可用空间
   - 网络：稳定的网络连接

2. **软件需求**
   - 操作系统：Linux (Ubuntu 20.04或CentOS 7)
   - Java运行环境：JDK 8或JDK 11
   - Python环境：Python 3.6+
   - Node.js环境：Node.js 14+
   - 容器环境（可选）：Docker和Docker Compose

### 1.2 部署方案选择

考虑到在单机PC上部署完整的分布式系统的复杂性，我们提供以下几种部署方案供选择：

1. **完整分布式部署**：在单机上模拟多节点环境，完整部署Kafka集群、Flink集群、推荐系统和可视化系统。
   - 优点：最接近生产环境，可以完整体验分布式系统的特性
   - 缺点：资源消耗大，配置复杂，对PC硬件要求高

2. **伪分布式部署**：在单机上以伪分布式模式部署Kafka和Flink，简化集群配置。
   - 优点：保留分布式特性，资源消耗适中，配置相对简单
   - 缺点：性能和可靠性不如完整分布式部署

3. **单机模拟部署**：使用单机版的Kafka和Flink，重点展示推荐系统功能和可视化效果。
   - 优点：资源消耗小，配置简单，适合大多数PC
   - 缺点：失去了分布式系统的特性和优势

4. **Docker容器化部署**：使用Docker和Docker Compose将整个系统容器化部署。
   - 优点：环境隔离，部署简单，可移植性强
   - 缺点：需要了解Docker基础知识，可能存在性能开销

### 1.3 推荐部署方案

基于大多数用户PC的配置和使用便捷性考虑，我们**推荐采用Docker容器化部署方案**，原因如下：

1. 环境一致性：避免因环境差异导致的部署问题
2. 部署简便：通过Docker Compose可以一键启动整个系统
3. 资源可控：可以根据PC配置调整容器资源分配
4. 易于清理：使用完毕后可以方便地清理环境

如果用户PC配置较高（16GB内存以上），也可以考虑伪分布式部署方案，以更好地体验分布式系统的特性。

## 2. 部署前置条件与环境准备

### 2.1 Docker环境安装

#### Windows系统

1. 下载并安装Docker Desktop for Windows：
   - 访问 https://www.docker.com/products/docker-desktop
   - 下载安装包并运行
   - 按照安装向导完成安装

2. 启用WSL 2（Windows Subsystem for Linux 2）：
   - 打开PowerShell（以管理员身份运行）
   - 执行命令：`dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart`
   - 执行命令：`dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart`
   - 重启计算机
   - 下载并安装WSL 2 Linux内核更新包：https://aka.ms/wsl2kernel
   - 执行命令：`wsl --set-default-version 2`

3. 配置Docker资源：
   - 打开Docker Desktop
   - 点击右上角的设置图标
   - 在"Resources"选项卡中，分配至少8GB内存和4核CPU
   - 点击"Apply & Restart"应用设置

#### macOS系统

1. 下载并安装Docker Desktop for Mac：
   - 访问 https://www.docker.com/products/docker-desktop
   - 下载安装包并运行
   - 将Docker图标拖到Applications文件夹

2. 启动Docker：
   - 在Applications文件夹中找到Docker并启动
   - 等待Docker启动完成（状态栏图标变为稳定状态）

3. 配置Docker资源：
   - 点击状态栏的Docker图标
   - 选择"Preferences"
   - 在"Resources"选项卡中，分配至少8GB内存和4核CPU
   - 点击"Apply & Restart"应用设置

#### Linux系统

1. 安装Docker Engine：

   ```bash
   # 更新包索引
   sudo apt-get update

   # 安装必要的包
   sudo apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release

   # 添加Docker官方GPG密钥
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

   # 设置稳定版仓库
   echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

   # 更新包索引
   sudo apt-get update

   # 安装Docker Engine
   sudo apt-get install -y docker-ce docker-ce-cli containerd.io
   ```

2. 安装Docker Compose：

   ```bash
   # 下载Docker Compose
   sudo curl -L "https://github.com/docker/compose/releases/download/v2.5.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

   # 添加执行权限
   sudo chmod +x /usr/local/bin/docker-compose

   # 验证安装
   docker-compose --version
   ```

3. 配置Docker用户组（可选，避免每次使用sudo）：

   ```bash
   # 创建docker组
   sudo groupadd docker

   # 将当前用户添加到docker组
   sudo usermod -aG docker $USER

   # 激活更改（或者重新登录）
   newgrp docker
   ```

### 2.2 Git安装（用于获取项目代码）

#### Windows系统

1. 下载并安装Git for Windows：
   - 访问 https://git-scm.com/download/win
   - 下载安装包并运行
   - 按照安装向导完成安装，使用默认选项即可

#### macOS系统

1. 使用Homebrew安装Git：
   ```bash
   # 安装Homebrew（如果尚未安装）
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

   # 安装Git
   brew install git
   ```

2. 或者下载安装包：
   - 访问 https://git-scm.com/download/mac
   - 下载安装包并运行

#### Linux系统

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y git

# CentOS/RHEL
sudo yum install -y git
```

### 2.3 验证环境准备

完成上述安装后，请验证环境是否准备就绪：

```bash
# 验证Docker
docker --version
docker-compose --version
docker run hello-world

# 验证Git
git --version
```

如果所有命令都能正常执行，说明环境已经准备就绪，可以进行下一步的部署操作。

## 3. 系统部署步骤

### 3.1 获取项目代码

首先，我们需要从代码仓库获取项目代码：

```bash
# 创建项目目录
mkdir -p ~/distributed-realtime-recommendation
cd ~/distributed-realtime-recommendation

# 克隆项目代码（假设代码已上传到GitHub）
git clone https://github.com/yourusername/distributed-realtime-recommendation.git .

# 如果没有GitHub仓库，可以手动创建目录结构
mkdir -p kafka flink recommendation-system message-source visualization
```

### 3.2 准备Docker Compose配置

创建`docker-compose.yml`文件，定义整个系统的容器配置：

```yaml
version: '3'

services:
  # ZooKeeper服务
  zookeeper:
    image: wurstmeister/zookeeper
    container_name: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - recommendation-network

  # Kafka服务
  kafka1:
    image: wurstmeister/kafka
    container_name: kafka1
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ADVERTISED_HOST_NAME: kafka1
      KAFKA_ADVERTISED_PORT: 9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_CREATE_TOPICS: "user-behavior:3:1,recommendation-result:3:1"
    depends_on:
      - zookeeper
    networks:
      - recommendation-network

  kafka2:
    image: wurstmeister/kafka
    container_name: kafka2
    ports:
      - "9093:9092"
    environment:
      KAFKA_BROKER_ID: 2
      KAFKA_ADVERTISED_HOST_NAME: kafka2
      KAFKA_ADVERTISED_PORT: 9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    depends_on:
      - zookeeper
    networks:
      - recommendation-network

  kafka3:
    image: wurstmeister/kafka
    container_name: kafka3
    ports:
      - "9094:9092"
    environment:
      KAFKA_BROKER_ID: 3
      KAFKA_ADVERTISED_HOST_NAME: kafka3
      KAFKA_ADVERTISED_PORT: 9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    depends_on:
      - zookeeper
    networks:
      - recommendation-network

  # Flink JobManager
  jobmanager:
    image: flink:1.14.4-scala_2.12
    container_name: jobmanager
    ports:
      - "8081:8081"
    command: jobmanager
    environment:
      - JOB_MANAGER_RPC_ADDRESS=jobmanager
    networks:
      - recommendation-network

  # Flink TaskManager 1
  taskmanager1:
    image: flink:1.14.4-scala_2.12
    container_name: taskmanager1
    depends_on:
      - jobmanager
    command: taskmanager
    environment:
      - JOB_MANAGER_RPC_ADDRESS=jobmanager
    networks:
      - recommendation-network

  # Flink TaskManager 2
  taskmanager2:
    image: flink:1.14.4-scala_2.12
    container_name: taskmanager2
    depends_on:
      - jobmanager
    command: taskmanager
    environment:
      - JOB_MANAGER_RPC_ADDRESS=jobmanager
    networks:
      - recommendation-network

  # Flink TaskManager 3
  taskmanager3:
    image: flink:1.14.4-scala_2.12
    container_name: taskmanager3
    depends_on:
      - jobmanager
    command: taskmanager
    environment:
      - JOB_MANAGER_RPC_ADDRESS=jobmanager
    networks:
      - recommendation-network

  # 推荐系统
  recommendation-system:
    build:
      context: ./recommendation-system
      dockerfile: Dockerfile
    container_name: recommendation-system
    depends_on:
      - kafka1
      - kafka2
      - kafka3
      - jobmanager
    networks:
      - recommendation-network

  # 消息源软件
  message-source:
    build:
      context: ./message-source
      dockerfile: Dockerfile
    container_name: message-source
    ports:
      - "8082:8082"
    depends_on:
      - kafka1
      - kafka2
      - kafka3
    networks:
      - recommendation-network

  # 可视化后端
  visualization-backend:
    build:
      context: ./visualization/visualization-backend
      dockerfile: Dockerfile
    container_name: visualization-backend
    ports:
      - "5000:5000"
    depends_on:
      - kafka1
      - kafka2
      - kafka3
      - jobmanager
    networks:
      - recommendation-network

  # 可视化前端
  visualization-frontend:
    build:
      context: ./visualization/visualization-frontend
      dockerfile: Dockerfile
    container_name: visualization-frontend
    ports:
      - "3000:80"
    depends_on:
      - visualization-backend
    networks:
      - recommendation-network

networks:
  recommendation-network:
    driver: bridge
```

### 3.3 准备各组件的Dockerfile

#### 3.3.1 推荐系统Dockerfile

在`recommendation-system`目录下创建`Dockerfile`：

```dockerfile
FROM flink:1.14.4-scala_2.12

WORKDIR /opt/recommendation-system

# 复制推荐系统代码和依赖
COPY . /opt/recommendation-system/

# 安装Python依赖
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    pip3 install -r requirements.txt

# 设置启动脚本
COPY start.sh /opt/recommendation-system/
RUN chmod +x /opt/recommendation-system/start.sh

CMD ["/opt/recommendation-system/start.sh"]
```

创建`start.sh`启动脚本：

```bash
#!/bin/bash

# 等待Kafka和Flink启动
echo "Waiting for Kafka and Flink to start..."
sleep 30

# 提交Flink作业
echo "Submitting Flink job..."
flink run -d /opt/recommendation-system/recommendation-job.jar

# 保持容器运行
tail -f /dev/null
```

#### 3.3.2 消息源软件Dockerfile

在`message-source`目录下创建`Dockerfile`：

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /opt/message-source

# 复制消息源软件代码和依赖
COPY . /opt/message-source/

# 设置启动脚本
COPY start.sh /opt/message-source/
RUN chmod +x /opt/message-source/start.sh

EXPOSE 8082

CMD ["/opt/message-source/start.sh"]
```

创建`start.sh`启动脚本：

```bash
#!/bin/bash

# 等待Kafka启动
echo "Waiting for Kafka to start..."
sleep 30

# 启动消息源软件
echo "Starting message source..."
java -jar /opt/message-source/message-source.jar
```

#### 3.3.3 可视化后端Dockerfile

在`visualization/visualization-backend`目录下创建`Dockerfile`：

```dockerfile
FROM python:3.9-slim

WORKDIR /app

# 复制后端代码和依赖
COPY . /app/

# 安装依赖
RUN pip install -r requirements.txt

EXPOSE 5000

# 启动后端服务
CMD ["python", "src/main.py"]
```

#### 3.3.4 可视化前端Dockerfile

在`visualization/visualization-frontend`目录下创建`Dockerfile`：

```dockerfile
# 构建阶段
FROM node:16 as build

WORKDIR /app

# 复制前端代码和依赖
COPY . /app/

# 安装依赖并构建
RUN npm install
RUN npm run build

# 部署阶段
FROM nginx:alpine

# 从构建阶段复制构建结果到Nginx
COPY --from=build /app/build /usr/share/nginx/html

# 配置Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

创建`nginx.conf`配置文件：

```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://visualization-backend:5000/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /socket.io {
        proxy_pass http://visualization-backend:5000/socket.io;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

### 3.4 启动系统

完成上述配置后，可以使用Docker Compose启动整个系统：

```bash
# 进入项目根目录
cd ~/distributed-realtime-recommendation

# 构建并启动所有容器
docker-compose up -d

# 查看容器状态
docker-compose ps
```

启动过程可能需要几分钟时间，请耐心等待。

### 3.5 访问系统

系统启动成功后，可以通过以下URL访问各组件：

1. **Flink Web UI**：http://localhost:8081
   - 用于监控Flink作业的执行情况

2. **消息源软件**：http://localhost:8082
   - 用于模拟用户行为和查看推荐结果

3. **可视化系统**：http://localhost:3000
   - 用于查看系统运行状态、用户行为分析和推荐效果分析

## 4. 常见问题排查与解决

### 4.1 容器启动失败

**问题**：某些容器启动失败或反复重启。

**解决方案**：
1. 查看容器日志：
   ```bash
   docker-compose logs <container_name>
   ```

2. 检查端口冲突：
   ```bash
   # 查看端口占用情况
   netstat -tuln | grep <port_number>
   
   # 如果有冲突，修改docker-compose.yml中的端口映射
   ```

3. 检查资源不足：
   - 增加Docker分配的内存和CPU资源
   - 关闭不必要的应用程序释放资源

### 4.2 Kafka连接问题

**问题**：Kafka服务无法正常连接或消息无法正常发送/接收。

**解决方案**：
1. 检查Kafka容器状态：
   ```bash
   docker-compose ps kafka1 kafka2 kafka3
   ```

2. 检查ZooKeeper连接：
   ```bash
   docker-compose exec kafka1 bash -c "kafka-topics.sh --list --zookeeper zookeeper:2181"
   ```

3. 手动创建主题：
   ```bash
   docker-compose exec kafka1 bash -c "kafka-topics.sh --create --topic user-behavior --partitions 3 --replication-factor 1 --zookeeper zookeeper:2181"
   docker-compose exec kafka1 bash -c "kafka-topics.sh --create --topic recommendation-result --partitions 3 --replication-factor 1 --zookeeper zookeeper:2181"
   ```

### 4.3 Flink作业提交失败

**问题**：Flink作业无法成功提交或执行。

**解决方案**：
1. 检查JobManager和TaskManager状态：
   ```bash
   docker-compose logs jobmanager
   docker-compose logs taskmanager1
   ```

2. 通过Flink Web UI手动提交作业：
   - 访问http://localhost:8081
   - 点击"Submit New Job"
   - 上传JAR文件并提交

3. 检查JAR文件是否正确：
   - 确保JAR文件包含所有必要的依赖
   - 确保主类和入口点正确配置

### 4.4 可视化系统连接问题

**问题**：可视化前端无法连接后端或显示数据。

**解决方案**：
1. 检查后端服务状态：
   ```bash
   docker-compose logs visualization-backend
   ```

2. 检查API连接：
   ```bash
   curl http://localhost:5000/api/dashboard/kpi
   ```

3. 检查WebSocket连接：
   - 打开浏览器开发者工具
   - 查看Network选项卡中的WebSocket连接状态

4. 修改前端配置：
   - 如果使用不同的主机名或IP，需要更新前端代码中的API_URL和SOCKET_URL

### 4.5 系统性能问题

**问题**：系统运行缓慢或不稳定。

**解决方案**：
1. 增加Docker资源分配：
   - 打开Docker Desktop设置
   - 增加内存和CPU分配

2. 减少组件数量：
   - 修改docker-compose.yml，减少Kafka和TaskManager的实例数量
   - 例如，可以只保留一个Kafka和两个TaskManager

3. 优化Flink配置：
   - 修改Flink配置参数，减少内存使用
   - 调整并行度参数适应本地环境

## 5. 系统使用指南

### 5.1 消息源软件使用

1. 访问消息源软件Web界面：http://localhost:8082

2. 创建新商品：
   - 点击"创建商品"按钮
   - 填写商品信息（ID、名称、类别、价格等）
   - 点击"提交"按钮

3. 模拟用户购买行为：
   - 点击"模拟购买"按钮
   - 选择用户和目标商品
   - 点击"提交"按钮

4. 查看推荐结果：
   - 系统会自动显示推荐结果
   - 可以点击"刷新"按钮获取最新推荐

### 5.2 可视化系统使用

1. 访问可视化系统：http://localhost:3000

2. 仪表盘：
   - 查看系统关键指标（活跃用户数、推荐总数、平均延迟、吞吐量）
   - 查看实时用户行为和推荐结果流图表

3. 用户行为分析：
   - 查看用户行为类型分布
   - 查看用户行为时间分布
   - 查看热门商品排行榜
   - 查看用户活跃度分析

4. 推荐效果分析：
   - 查看推荐商品类别分布
   - 查看推荐理由分析
   - 查看推荐转化率趋势
   - 查看个人推荐历史记录

5. 系统性能监控：
   - 查看Kafka消息吞吐量
   - 查看Flink作业性能
   - 查看端到端延迟
   - 查看系统资源使用情况
   - 查看系统日志

### 5.3 Flink管理界面使用

1. 访问Flink Web UI：http://localhost:8081

2. 查看作业状态：
   - 在"Running Jobs"选项卡查看正在运行的作业
   - 在"Completed Jobs"选项卡查看已完成的作业

3. 监控作业性能：
   - 点击作业ID查看详细信息
   - 查看各算子的处理速度和延迟

4. 提交新作业：
   - 点击"Submit New Job"按钮
   - 上传JAR文件并配置参数
   - 点击"Submit"按钮

## 6. 系统关闭与清理

当不再需要运行系统时，可以按照以下步骤关闭和清理：

```bash
# 停止所有容器
docker-compose down

# 如果需要清理数据卷
docker-compose down -v

# 如果需要清理所有相关镜像
docker-compose down --rmi all
```

## 7. 总结

通过本指南，您已经成功将分布式实时电商推荐系统部署到本地PC上。系统包括Kafka消息队列、Flink流处理引擎、推荐系统、消息源软件和可视化系统等组件，可以模拟真实的电商推荐场景。

如果在部署或使用过程中遇到任何问题，请参考"常见问题排查与解决"部分，或者联系系统开发人员获取支持。

祝您使用愉快！
