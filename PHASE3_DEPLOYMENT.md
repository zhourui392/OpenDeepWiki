# Phase 3 部署文档

> **文档版本**: 1.0
> **创建时间**: 2025-11-13
> **适用环境**: Production, Staging, Development

---

## 📋 目录

1. [环境要求](#环境要求)
2. [配置文件](#配置文件)
3. [数据库初始化](#数据库初始化)
4. [部署步骤](#部署步骤)
5. [健康检查](#健康检查)
6. [监控配置](#监控配置)
7. [故障排查](#故障排查)

---

## 🔧 环境要求

### 最低配置

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 1.8+ | 建议使用OpenJDK 8或11 |
| Maven | 3.6+ | 用于构建项目 |
| MySQL | 5.7+/8.0+ | 数据库服务 |
| Redis | 5.0+ | 可选,用于分布式缓存 |
| 内存 | 2GB+ | 最低配置 |
| CPU | 2核+ | 最低配置 |
| 磁盘 | 20GB+ | 包含日志空间 |

### 推荐配置

| 组件 | 版本 | 配置 |
|------|------|------|
| JDK | OpenJDK 11 | - |
| MySQL | 8.0 | 4GB内存, 100GB存储 |
| Redis | 6.2 | 2GB内存 |
| 应用服务器 | - | 4核CPU, 8GB内存 |

---

## ⚙️ 配置文件

### application.yml

生产环境配置模板:

```yaml
server:
  port: 8080
  servlet:
    context-path: /
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain

spring:
  application:
    name: koalawiki

  profiles:
    active: prod

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:koalawiki}?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:koalawiki}
    password: ${DB_PASSWORD}
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      auto-commit: true
      leak-detection-threshold: 60000

  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    show-sql: false
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 100
        order_inserts: true
        order_updates: true

  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m
    cache-names:
      - catalogCache
      - statisticsCache
      - aiResultCache

  task:
    execution:
      pool:
        core-size: 5
        max-size: 10
        queue-capacity: 100
    scheduling:
      pool:
        size: 5

logging:
  level:
    root: INFO
    ai.opendw.koalawiki: INFO
    org.springframework: WARN
    org.hibernate: WARN
  file:
    name: logs/koalawiki.log
    max-size: 100MB
    max-history: 30
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# KoalaWiki配置
koalawiki:
  # OpenAI配置
  openai:
    api-key: ${OPENAI_API_KEY}
    endpoint: ${OPENAI_ENDPOINT:https://api.openai.com/v1}
    chat-model: ${OPENAI_CHAT_MODEL:gpt-4}
    analysis-model: ${OPENAI_ANALYSIS_MODEL:gpt-4-turbo-preview}
    max-tokens: ${OPENAI_MAX_TOKENS:4096}
    temperature: ${OPENAI_TEMPERATURE:0.7}
    timeout: ${OPENAI_TIMEOUT:60000}
    retry:
      max-attempts: 3
      backoff-delay: 1000

  # 访问日志配置
  access-log:
    enabled: true
    batch-size: 100
    flush-interval: 30000
    retention-days: 90
    queue-size: 10000

  # 统计配置
  statistics:
    enabled: true
    daily-cron: "0 0 1 * * ?"
    weekly-cron: "0 0 1 ? * MON"
    monthly-cron: "0 0 1 1 * ?"
    retention-days: 365

  # 同步配置
  sync:
    enabled: true
    cron: "0 */30 * * * ?"
    max-concurrent: 5

# Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

### 环境变量

创建`.env`文件:

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=koalawiki
DB_USERNAME=koalawiki
DB_PASSWORD=your_secure_password

# OpenAI配置
OPENAI_API_KEY=sk-your-openai-api-key
OPENAI_ENDPOINT=https://api.openai.com/v1
OPENAI_CHAT_MODEL=gpt-4
OPENAI_MAX_TOKENS=4096

# Redis配置(可选)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 应用配置
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# 日志配置
LOG_LEVEL=INFO
LOG_PATH=/var/log/koalawiki
```

---

## 🗄️ 数据库初始化

### 1. 创建数据库

```sql
CREATE DATABASE koalawiki
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER 'koalawiki'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON koalawiki.* TO 'koalawiki'@'%';
FLUSH PRIVILEGES;
```

### 2. 执行初始化脚本

```bash
cd java/scripts/sql

# Phase 1-2表结构
mysql -h localhost -u koalawiki -p koalawiki < phase1_schema.sql
mysql -h localhost -u koalawiki -p koalawiki < phase2_schema.sql

# Phase 3表结构
mysql -h localhost -u koalawiki -p koalawiki < phase3_schema.sql
```

### 3. Phase 3表结构

```sql
-- AccessLog表
CREATE TABLE access_log (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50),
    warehouse_id VARCHAR(50),
    document_id VARCHAR(50),
    action VARCHAR(20) NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    request_uri VARCHAR(500),
    request_method VARCHAR(10),
    request_params TEXT,
    response_time INT,
    status_code INT,
    error_message VARCHAR(1000),
    access_time TIMESTAMP NOT NULL,
    session_id VARCHAR(100),
    referer VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_user_id (user_id),
    INDEX idx_access_time (access_time),
    INDEX idx_action (action),
    INDEX idx_warehouse_access_time (warehouse_id, access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- DailyStatistics表
CREATE TABLE daily_statistics (
    id VARCHAR(50) PRIMARY KEY,
    statistics_date DATE NOT NULL,
    warehouse_id VARCHAR(50),
    view_count BIGINT DEFAULT 0,
    unique_user_count BIGINT DEFAULT 0,
    unique_ip_count BIGINT DEFAULT 0,
    document_view_count BIGINT DEFAULT 0,
    search_count BIGINT DEFAULT 0,
    download_count BIGINT DEFAULT 0,
    avg_response_time DOUBLE DEFAULT 0,
    max_response_time INT DEFAULT 0,
    min_response_time INT DEFAULT 0,
    total_requests BIGINT DEFAULT 0,
    success_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    error_rate DOUBLE DEFAULT 0,
    action_counts TEXT,
    top_documents TEXT,
    top_search_keywords TEXT,
    new_user_count BIGINT DEFAULT 0,
    active_user_count BIGINT DEFAULT 0,
    calculated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_statistics_date (statistics_date),
    INDEX idx_warehouse_date (warehouse_id, statistics_date),
    UNIQUE KEY uk_warehouse_date (warehouse_id, statistics_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 🚀 部署步骤

### 方式1: JAR包部署

#### 1. 构建项目

```bash
cd java
mvn clean package -DskipTests

# 生成的JAR文件位于
# koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar
```

#### 2. 运行应用

```bash
# 使用环境变量
export DB_HOST=localhost
export DB_PASSWORD=your_password
export OPENAI_API_KEY=sk-your-key

java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod

# 或使用配置文件
java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar \
  --spring.config.location=file:./application-prod.yml
```

#### 3. 后台运行

```bash
nohup java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  > logs/koalawiki.log 2>&1 &

# 查看进程
ps aux | grep koalawiki
```

### 方式2: Docker部署

#### 1. 构建Docker镜像

```bash
cd java

# 使用项目提供的Dockerfile
docker build -t koalawiki:latest .
```

#### 2. 运行容器

```bash
docker run -d \
  --name koalawiki \
  -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=koalawiki \
  -e DB_USERNAME=koalawiki \
  -e DB_PASSWORD=your_password \
  -e OPENAI_API_KEY=sk-your-key \
  -v /var/log/koalawiki:/app/logs \
  koalawiki:latest
```

#### 3. Docker Compose部署

创建`docker-compose-phase3.yml`:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: koalawiki-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: koalawiki
      MYSQL_USER: koalawiki
      MYSQL_PASSWORD: koalawiki_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./scripts/sql:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:6.2-alpine
    container_name: koalawiki-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  koalawiki:
    image: koalawiki:latest
    container_name: koalawiki-app
    depends_on:
      - mysql
      - redis
    ports:
      - "8080:8080"
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: koalawiki
      DB_USERNAME: koalawiki
      DB_PASSWORD: koalawiki_password
      REDIS_HOST: redis
      REDIS_PORT: 6379
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      SPRING_PROFILES_ACTIVE: prod
    volumes:
      - ./logs:/app/logs
      - ./data:/app/data
    restart: unless-stopped

volumes:
  mysql_data:
  redis_data:
```

启动:

```bash
docker-compose -f docker-compose-phase3.yml up -d
```

### 方式3: Kubernetes部署

#### 1. 创建ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: koalawiki-config
data:
  application.yml: |
    server:
      port: 8080
    spring:
      datasource:
        url: jdbc:mysql://mysql-service:3306/koalawiki
```

#### 2. 创建Secret

```bash
kubectl create secret generic koalawiki-secrets \
  --from-literal=db-password=your_password \
  --from-literal=openai-api-key=sk-your-key
```

#### 3. 创建Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: koalawiki
spec:
  replicas: 3
  selector:
    matchLabels:
      app: koalawiki
  template:
    metadata:
      labels:
        app: koalawiki
    spec:
      containers:
      - name: koalawiki
        image: koalawiki:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: koalawiki-secrets
              key: db-password
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: koalawiki-secrets
              key: openai-api-key
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

#### 4. 创建Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: koalawiki-service
spec:
  type: LoadBalancer
  selector:
    app: koalawiki
  ports:
  - port: 80
    targetPort: 8080
```

---

## 🏥 健康检查

### Actuator端点

应用启动后，通过以下端点检查状态:

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 详细健康信息(需要认证)
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/actuator/health

# 应用信息
curl http://localhost:8080/actuator/info

# 指标
curl http://localhost:8080/actuator/metrics
```

### 健康检查响应

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 214748364800,
        "free": 107374182400,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

## 📊 监控配置

### Prometheus集成

#### 1. 添加Prometheus配置

`prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'koalawiki'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

#### 2. 启动Prometheus

```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v ./prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

### Grafana仪表板

#### 1. 启动Grafana

```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  grafana/grafana
```

#### 2. 添加数据源

- URL: http://prometheus:9090
- Type: Prometheus

#### 3. 导入仪表板

推荐仪表板ID:
- JVM (Micrometer): 4701
- Spring Boot 2.1: 10280
- MySQL: 7362

---

## 🔍 故障排查

### 常见问题

#### 1. 应用无法启动

**症状**: 应用启动失败或退出

**检查步骤**:

```bash
# 查看日志
tail -f logs/koalawiki.log

# 检查端口占用
netstat -tunlp | grep 8080

# 检查数据库连接
mysql -h $DB_HOST -u $DB_USERNAME -p
```

#### 2. 数据库连接失败

**症状**: `CommunicationsException`

**解决方案**:

```bash
# 检查数据库服务
systemctl status mysql

# 检查防火墙
sudo firewall-cmd --list-ports

# 测试连接
telnet $DB_HOST 3306
```

#### 3. OpenAI API调用失败

**症状**: AI功能不可用

**检查步骤**:

```bash
# 验证API Key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"

# 检查网络连接
ping api.openai.com

# 查看应用日志
grep "AIClient" logs/koalawiki.log
```

#### 4. 内存溢出

**症状**: `OutOfMemoryError`

**解决方案**:

```bash
# 增加堆内存
java -Xms2g -Xmx4g -jar koalawiki.jar

# 生成堆转储
java -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -jar koalawiki.jar
```

#### 5. 性能问题

**症状**: 响应慢、超时

**诊断工具**:

```bash
# 查看线程状态
jstack <pid> > thread_dump.txt

# 查看CPU使用
top -H -p <pid>

# 慢查询日志
tail -f /var/log/mysql/slow-query.log
```

### 日志分析

```bash
# 查找错误日志
grep -i error logs/koalawiki.log

# 统计错误类型
grep -i error logs/koalawiki.log | awk '{print $6}' | sort | uniq -c

# 查找慢请求
grep "duration>" logs/koalawiki.log | awk '{print $NF}' | sort -n

# 实时监控
tail -f logs/koalawiki.log | grep -i "error\|warn"
```

---

## 🔐 安全配置

### 1. SSL/TLS配置

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: koalawiki
```

### 2. 防火墙配置

```bash
# 开放8080端口
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

### 3. 数据库安全

```sql
-- 限制远程访问
REVOKE ALL PRIVILEGES ON *.* FROM 'koalawiki'@'%';
GRANT ALL PRIVILEGES ON koalawiki.* TO 'koalawiki'@'localhost';

-- 启用SSL连接
ALTER USER 'koalawiki'@'localhost' REQUIRE SSL;
```

---

## 📝 备份和恢复

### 数据库备份

```bash
# 完整备份
mysqldump -h localhost -u koalawiki -p koalawiki \
  > backup_$(date +%Y%m%d).sql

# 增量备份(使用binlog)
mysqlbinlog mysql-bin.000001 > incremental_backup.sql
```

### 数据恢复

```bash
# 恢复完整备份
mysql -h localhost -u koalawiki -p koalawiki \
  < backup_20251113.sql

# 恢复增量备份
mysql -h localhost -u koalawiki -p koalawiki \
  < incremental_backup.sql
```

---

## 🎯 性能调优

### JVM参数

```bash
java -server \
  -Xms4g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -jar koalawiki.jar
```

### MySQL调优

```ini
[mysqld]
max_connections = 500
innodb_buffer_pool_size = 4G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
query_cache_size = 0
query_cache_type = 0
```

---

**编写者**: OpenDeepWiki Team
**日期**: 2025-11-13
**版本**: 1.0
