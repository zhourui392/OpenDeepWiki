# Linux 部署指南

## 快速开始

### 1. 完整部署（推荐）
```bash
# 一键部署：编译 + 打包 + 停止旧服务 + 启动新服务
./deploy.sh deploy
```

### 2. 分步部署
```bash
# 步骤1：编译打包
./deploy.sh build

# 步骤2：启动应用
./deploy.sh start

# 步骤3：查看状态
./deploy.sh status

# 步骤4：查看日志
./deploy.sh tail
```

## 命令说明

### build - 编译打包
```bash
./deploy.sh build
```
- 清理旧的编译产物
- 执行 Maven 打包（跳过测试）
- 生成可执行 JAR 文件

### start - 启动应用
```bash
./deploy.sh start
```
- 检查应用是否已运行
- 后台启动 Spring Boot 应用
- 配置 JVM 参数（默认 Xms512m Xmx1024m）
- 将 PID 写入 `/var/run/koalawiki.pid`

### stop - 停止应用
```bash
./deploy.sh stop
```
- 优雅停止（发送 TERM 信号）
- 等待最多 30 秒
- 超时则强制终止（KILL 信号）
- 清理 PID 文件

### restart - 重启应用
```bash
./deploy.sh restart
```
- 等效于 `stop` + `start`
- 适用于配置变更后重启

### status - 查看状态
```bash
./deploy.sh status
```
显示信息：
- 进程 PID
- 内存使用量
- 运行时间
- 日志文件路径

### logs - 查看日志
```bash
./deploy.sh logs
```
显示最近 100 行日志

### tail - 实时查看日志
```bash
./deploy.sh tail
```
实时跟踪日志输出（Ctrl+C 退出）

### deploy - 完整部署
```bash
./deploy.sh deploy
```
执行完整部署流程：
1. 检查环境（Java、Maven）
2. 创建必要目录
3. 编译打包
4. 停止旧服务（如果运行中）
5. 启动新服务
6. 显示运行状态

## 环境变量

### SPRING_PROFILES_ACTIVE
指定 Spring 配置文件

```bash
# 使用开发环境配置
SPRING_PROFILES_ACTIVE=dev ./deploy.sh start

# 使用生产环境配置（默认）
SPRING_PROFILES_ACTIVE=prod ./deploy.sh start
```

### GIT_STORAGE_PATH
指定 Git 仓库存储路径

```bash
# 自定义 Git 存储路径
GIT_STORAGE_PATH=/data/koalawiki/git ./deploy.sh start
```

### AI_ENABLED
是否启用 AI 功能

```bash
# 禁用 AI 功能
AI_ENABLED=false ./deploy.sh start
```

## 系统要求

### 必需组件
- **JDK 1.8+**
  ```bash
  # Ubuntu/Debian
  sudo apt install openjdk-8-jdk

  # CentOS/RHEL
  sudo yum install java-1.8.0-openjdk
  ```

- **Maven 3.6+**
  ```bash
  # Ubuntu/Debian
  sudo apt install maven

  # CentOS/RHEL
  sudo yum install maven
  ```

- **MySQL 5.7+** (生产环境)
  ```bash
  # Ubuntu/Debian
  sudo apt install mysql-server

  # CentOS/RHEL
  sudo yum install mysql-server
  ```

### 可选组件
- **Claude CLI** (AI 文档生成)
  ```bash
  npm install -g @anthropic/claude-cli
  ```

## 初始化部署

### 1. 环境准备
```bash
# 安装依赖
sudo apt update
sudo apt install -y openjdk-8-jdk maven mysql-server git

# 验证安装
java -version
mvn -version
```

### 2. 数据库初始化
```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE koalawiki CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户（可选）
CREATE USER 'koalawiki'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON koalawiki.* TO 'koalawiki'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 配置应用

#### 后端配置
编辑 `koalawiki-web/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/koalawiki?useUnicode=true&characterEncoding=utf-8
    username: koalawiki
    password: your_password
```

#### 前端配置（重要！）
编辑前端 API 地址配置：

```bash
cd koalawiki-web-vue

# 创建生产环境配置
cat > .env.production.local << EOF
# 后端 API 地址
# 场景1: 前后端同机部署（推荐）- 使用相对路径
VITE_API_BASE_URL=/api

# 场景2: 前后端分离部署 - 使用完整地址
# VITE_API_BASE_URL=http://localhost:18081/api

# 场景3: 远程部署 - 使用服务器 IP
# VITE_API_BASE_URL=http://192.168.1.100:18081/api

# 场景4: 域名部署 - 使用域名
# VITE_API_BASE_URL=https://api.yourdomain.com/api
EOF

# 安装依赖并构建前端
npm install
npm run build

# 构建后的文件会自动复制到 koalawiki-web/src/main/resources/static
cd ..
```

**配置说明**：
- 前后端同机部署（推荐）：使用 `/api`，无需配置 CORS
- 前后端分离部署：使用完整的后端 URL，需配置 CORS
- 详细配置说明：参见 `koalawiki-web-vue/API_CONFIG.md`

### 4. 首次部署
```bash
# 克隆项目
git clone https://github.com/your-org/OpenDeepWiki.git
cd OpenDeepWiki

# 设置脚本可执行权限
chmod +x deploy.sh

# 完整部署
./deploy.sh deploy
```

### 5. 验证部署
```bash
# 查看状态
./deploy.sh status

# 健康检查
curl http://localhost:18081/actuator/health

# 查看日志
./deploy.sh tail
```

## 文件路径

| 文件/目录 | 路径 | 说明 |
|----------|------|------|
| JAR 文件 | `koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar` | 应用程序包 |
| PID 文件 | `/var/run/koalawiki.pid` | 进程 ID 文件 |
| 日志文件 | `logs/koalawiki.log` | 应用日志 |
| Git 存储 | `/var/lib/koalawiki/git` | Git 仓库存储 |
| 配置文件 | `koalawiki-web/src/main/resources/application.yml` | 应用配置 |

## 常见问题

### Q: 权限不足，无法创建 PID 文件
**A:** 修改 PID 文件路径或使用 sudo：
```bash
# 方式1：修改脚本中的 PID_FILE 路径
PID_FILE="./koalawiki.pid"

# 方式2：使用 sudo（不推荐）
sudo ./deploy.sh start
```

### Q: 端口被占用
**A:** 修改 application.yml 中的端口：
```yaml
server:
  port: 18082  # 改为其他端口
```

### Q: 内存不足
**A:** 调整脚本中的 JVM 参数：
```bash
# 在 deploy.sh 的 start() 函数中修改
-Xms256m \
-Xmx512m \
```

### Q: 应用启动失败
**A:** 查看详细日志：
```bash
./deploy.sh logs
# 或
tail -f logs/koalawiki.log
```

### Q: 如何更新配置
**A:** 修改配置后重启：
```bash
# 1. 编辑配置文件
vim koalawiki-web/src/main/resources/application.yml

# 2. 重新编译
./deploy.sh build

# 3. 重启应用
./deploy.sh restart
```

## 生产环境建议

### 1. 使用 systemd 管理服务
创建 `/etc/systemd/system/koalawiki.service`：

```ini
[Unit]
Description=OpenDeepWiki Service
After=network.target mysql.service

[Service]
Type=forking
User=koalawiki
WorkingDirectory=/opt/OpenDeepWiki
ExecStart=/opt/OpenDeepWiki/deploy.sh start
ExecStop=/opt/OpenDeepWiki/deploy.sh stop
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

使用 systemd 命令：
```bash
# 启动服务
sudo systemctl start koalawiki

# 停止服务
sudo systemctl stop koalawiki

# 重启服务
sudo systemctl restart koalawiki

# 开机自启
sudo systemctl enable koalawiki

# 查看状态
sudo systemctl status koalawiki
```

### 2. 配置日志轮转
创建 `/etc/logrotate.d/koalawiki`：

```
/opt/OpenDeepWiki/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 koalawiki koalawiki
    sharedscripts
    postrotate
        /opt/OpenDeepWiki/deploy.sh restart > /dev/null 2>&1 || true
    endscript
}
```

### 3. 监控告警
使用脚本定期检查健康状态：

```bash
#!/bin/bash
# /opt/OpenDeepWiki/healthcheck.sh

HEALTH_URL="http://localhost:18081/actuator/health"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" ${HEALTH_URL})

if [ "${RESPONSE}" != "200" ]; then
    echo "Health check failed: ${RESPONSE}"
    # 发送告警（邮件、钉钉、企业微信等）
    # 自动重启
    /opt/OpenDeepWiki/deploy.sh restart
fi
```

配置 crontab：
```bash
# 每 5 分钟检查一次
*/5 * * * * /opt/OpenDeepWiki/healthcheck.sh
```

## 故障排查

### 1. 检查进程
```bash
ps aux | grep koalawiki
```

### 2. 检查端口
```bash
netstat -tlnp | grep 18081
# 或
lsof -i:18081
```

### 3. 检查日志
```bash
# 应用日志
tail -100 logs/koalawiki.log

# 系统日志
journalctl -u koalawiki -n 100
```

### 4. 检查磁盘空间
```bash
df -h
du -sh /var/lib/koalawiki/*
```

### 5. 检查数据库连接
```bash
mysql -h localhost -u koalawiki -p -e "SELECT 1"
```

## 技术支持

- 项目地址: https://github.com/your-org/OpenDeepWiki
- 问题反馈: https://github.com/your-org/OpenDeepWiki/issues

---

**最后更新**: 2025-01-21
