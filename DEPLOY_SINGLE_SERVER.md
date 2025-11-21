# 前后端同机部署快速指南

## 架构说明

### 部署架构图
```
┌─────────────────────────────────────────────┐
│              Linux 服务器                    │
│                                             │
│  ┌───────────────────────────────────────┐ │
│  │    Spring Boot 应用 (端口 18081)      │ │
│  │                                       │ │
│  │  ┌─────────────────────────────────┐ │ │
│  │  │  静态资源 (Vue 构建产物)        │ │ │
│  │  │  /src/main/resources/static/    │ │ │
│  │  │  - index.html                   │ │ │
│  │  │  - static/js/                   │ │ │
│  │  │  - static/css/                  │ │ │
│  │  └─────────────────────────────────┘ │ │
│  │                                       │ │
│  │  ┌─────────────────────────────────┐ │ │
│  │  │  REST API                       │ │ │
│  │  │  /api/*                         │ │ │
│  │  └─────────────────────────────────┘ │ │
│  └───────────────────────────────────────┘ │
└─────────────────────────────────────────────┘

用户访问: http://server-ip:18081
  ├─ /            → 返回 Vue 前端页面
  ├─ /warehouses  → 返回 Vue 前端页面
  └─ /api/*       → 后端 REST API
```

### 工作原理

1. **前端资源**: Vue 构建后的静态文件打包到 Spring Boot 的 `static` 目录
2. **路由处理**:
   - Spring Boot 优先处理 `/api/*` 路径（后端 API）
   - 其他路径返回静态资源（前端页面）
3. **API 调用**: 前端使用相对路径 `/api` 调用后端，无跨域问题

---

## 完整部署步骤

### 前提条件

确保已安装：
- JDK 1.8+
- Maven 3.6+
- Node.js 20+
- MySQL 5.7+ (或使用 H2 内存数据库)
- Git

---

### 步骤 1: 克隆项目

```bash
# 克隆代码
git clone https://github.com/zhourui392/OpenDeepWiki.git
cd OpenDeepWiki

# 查看项目结构
ls -la
```

---

### 步骤 2: 配置数据库（可选）

#### 选项 A: 使用 H2 内存数据库（开发测试）

无需配置，直接跳到步骤 3。

#### 选项 B: 使用 MySQL（生产推荐）

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 创建数据库
CREATE DATABASE koalawiki CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 3. 创建用户（可选）
CREATE USER 'koalawiki'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON koalawiki.* TO 'koalawiki'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 4. 修改配置文件
vim koalawiki-web/src/main/resources/application.yml
```

修改数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/koalawiki?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: koalawiki
    password: your_password
```

---

### 步骤 3: 配置前端 API 地址

```bash
cd koalawiki-web-vue

# 创建生产环境配置
cat > .env.production.local << 'EOF'
# 前后端同机部署 - 使用相对路径
VITE_API_BASE_URL=/api
EOF

# 查看配置
cat .env.production.local
```

**重要**: 必须设置为 `/api`（相对路径），这样前端请求会自动发送到同一个端口。

---

### 步骤 4: 安装前端依赖

```bash
# 确保在 koalawiki-web-vue 目录下
npm install
```

如果遇到网络问题，可以使用国内镜像：
```bash
npm install --registry=https://registry.npmmirror.com
```

---

### 步骤 5: 构建前端

```bash
# 构建生产版本
npm run build
```

**构建过程**：
1. 编译 Vue 项目
2. 优化资源（压缩、代码分割）
3. 自动复制到 `../koalawiki-web/src/main/resources/static/` 目录

**验证构建结果**：
```bash
# 检查静态文件是否生成
ls -la ../koalawiki-web/src/main/resources/static/
# 应该看到: index.html, static/ 等文件

cd ..  # 返回项目根目录
```

---

### 步骤 6: 配置 Claude CLI（可选）

如果需要使用 AI 文档生成功能：

```bash
# 安装 Claude CLI（根据官方文档）
npm install -g @anthropic/claude-cli

# 或者临时禁用 AI 功能
export AI_ENABLED=false
```

---

### 步骤 7: 部署应用

#### 方式 A: 使用部署脚本（推荐）

```bash
# 一键部署
./deploy.sh deploy
```

这个命令会：
1. ✅ 检查环境（Java、Maven）
2. ✅ 清理旧的编译产物
3. ✅ 编译打包后端（包含前端静态文件）
4. ✅ 停止旧服务（如果在运行）
5. ✅ 启动新服务
6. ✅ 显示运行状态

#### 方式 B: 手动部署

```bash
# 1. 编译打包
mvn clean package -DskipTests

# 2. 启动应用
java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar

# 或后台运行
nohup java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar > logs/app.log 2>&1 &
```

---

### 步骤 8: 验证部署

#### 1. 检查应用状态
```bash
# 使用部署脚本
./deploy.sh status

# 或手动检查进程
ps aux | grep koalawiki
```

#### 2. 健康检查
```bash
# 检查后端健康状态
curl http://localhost:18081/actuator/health

# 预期输出：
# {"status":"UP"}
```

#### 3. 访问应用

**浏览器访问**：
- 应用首页: http://localhost:18081
- 或远程访问: http://your-server-ip:18081

#### 4. 测试前端是否正常加载
打开浏览器开发者工具 (F12)：
- **Console 标签**: 检查是否有错误
- **Network 标签**: 检查资源是否正常加载
- **Network 标签**: 查看 API 请求是否正确（应该是 `/api/xxx`）

#### 5. 测试 API 调用
```bash
# 测试仓库 API
curl http://localhost:18081/api/warehouse/ListWarehouses

# 测试健康检查 API
curl http://localhost:18081/api/health
```

---

### 步骤 9: 查看日志

```bash
# 实时查看日志
./deploy.sh tail

# 或查看最近日志
./deploy.sh logs

# 或直接查看日志文件
tail -f logs/koalawiki.log
```

---

## 常见操作

### 重启应用
```bash
./deploy.sh restart
```

### 停止应用
```bash
./deploy.sh stop
```

### 更新代码后重新部署
```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建前端（如果前端有更新）
cd koalawiki-web-vue
npm run build
cd ..

# 3. 重新部署
./deploy.sh deploy
```

### 仅更新前端
```bash
cd koalawiki-web-vue

# 修改代码...

# 重新构建
npm run build

cd ..

# 重启应用
./deploy.sh restart
```

### 仅更新后端
```bash
# 修改后端代码...

# 重新编译并重启
./deploy.sh build
./deploy.sh restart
```

---

## 防火墙配置（如果需要外网访问）

### Ubuntu/Debian (ufw)
```bash
# 开放端口
sudo ufw allow 18081/tcp

# 查看状态
sudo ufw status
```

### CentOS/RHEL (firewalld)
```bash
# 开放端口
sudo firewall-cmd --permanent --add-port=18081/tcp
sudo firewall-cmd --reload

# 查看开放端口
sudo firewall-cmd --list-ports
```

### 云服务器安全组
如果使用云服务器（阿里云、腾讯云等），还需要在控制台的安全组中开放 18081 端口。

---

## 目录结构说明

部署后的关键目录：

```
OpenDeepWiki/
├── deploy.sh                          # 部署脚本
├── logs/
│   └── koalawiki.log                  # 应用日志
├── koalawiki-web/
│   ├── src/main/resources/
│   │   ├── static/                    # 前端静态文件（自动生成）
│   │   │   ├── index.html
│   │   │   └── static/
│   │   │       ├── js/
│   │   │       ├── css/
│   │   │       └── fonts/
│   │   └── application.yml            # 后端配置
│   └── target/
│       └── koalawiki-web-*.jar        # 可执行 JAR
├── koalawiki-web-vue/                 # 前端源码
│   ├── .env.production.local          # 前端生产配置（不提交）
│   ├── src/
│   └── dist/                          # 前端构建产物（临时）
└── data/
    └── git/                           # Git 仓库存储目录
```

---

## 访问说明

### 本地访问
```
http://localhost:18081
```

### 局域网访问
```
http://192.168.1.100:18081
```
*替换为实际服务器 IP*

### 外网访问
```
http://your-public-ip:18081
```
*需要配置防火墙和路由器端口转发*

### 域名访问（推荐）

使用 Nginx 反向代理：

```nginx
# /etc/nginx/sites-available/koalawiki
server {
    listen 80;
    server_name koalawiki.yourdomain.com;

    location / {
        proxy_pass http://localhost:18081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/koalawiki /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

访问: http://koalawiki.yourdomain.com

---

## 故障排查

### 问题 1: 前端页面无法访问

**检查**:
```bash
# 1. 检查静态文件是否存在
ls koalawiki-web/src/main/resources/static/

# 2. 检查应用是否运行
./deploy.sh status

# 3. 查看日志
./deploy.sh tail
```

**解决**:
```bash
# 重新构建前端
cd koalawiki-web-vue
npm run build
cd ..

# 重新部署
./deploy.sh restart
```

---

### 问题 2: API 请求 404

**检查**:
```bash
# 测试 API 是否可访问
curl http://localhost:18081/api/health
```

**原因**:
- 后端未启动
- API 路径错误
- 端口被占用

**解决**:
```bash
# 查看详细日志
tail -100 logs/koalawiki.log

# 检查端口占用
netstat -tlnp | grep 18081

# 重启应用
./deploy.sh restart
```

---

### 问题 3: 前端请求后端跨域错误

**原因**: 前端配置错误，使用了完整 URL 而不是相对路径

**检查配置**:
```bash
cat koalawiki-web-vue/.env.production.local
# 应该是: VITE_API_BASE_URL=/api
```

**解决**:
```bash
cd koalawiki-web-vue
echo "VITE_API_BASE_URL=/api" > .env.production.local
npm run build
cd ..
./deploy.sh restart
```

---

### 问题 4: 数据库连接失败

**检查日志**:
```bash
grep "database" logs/koalawiki.log
```

**解决**:
```bash
# 1. 检查 MySQL 是否运行
sudo systemctl status mysql

# 2. 测试数据库连接
mysql -h localhost -u koalawiki -p

# 3. 检查配置文件
vim koalawiki-web/src/main/resources/application.yml

# 4. 重启应用
./deploy.sh restart
```

---

### 问题 5: 内存不足

**调整 JVM 参数**:
```bash
# 编辑部署脚本
vim deploy.sh

# 找到 start() 函数中的 java -jar 命令，修改内存参数:
-Xms256m \
-Xmx512m \
```

或直接运行：
```bash
java -jar -Xms256m -Xmx512m koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar
```

---

## 性能优化

### 1. 启用 Gzip 压缩
前端构建已包含 Gzip 压缩，Spring Boot 会自动支持。

### 2. 调整 JVM 参数
```bash
# 根据服务器配置调整
-Xms512m    # 初始堆内存
-Xmx1024m   # 最大堆内存
-XX:+UseG1GC  # 使用 G1 垃圾回收器
```

### 3. 使用 systemd 管理服务
```bash
# 创建服务文件
sudo vim /etc/systemd/system/koalawiki.service
```

内容参考 `DEPLOY_LINUX.md` 中的 systemd 配置。

---

## 安全建议

### 1. 修改默认端口
```yaml
# application.yml
server:
  port: 8080  # 或其他端口
```

### 2. 配置 HTTPS

使用 Nginx + Let's Encrypt:
```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d koalawiki.yourdomain.com

# 自动续期
sudo certbot renew --dry-run
```

### 3. 限制访问 IP
```yaml
# application.yml
server:
  address: 127.0.0.1  # 仅本地访问，通过 Nginx 代理
```

---

## 总结

**前后端同机部署的优势**:
- ✅ **零配置**: 无需配置 CORS
- ✅ **简单**: 一个端口，一个进程
- ✅ **高效**: 无跨域开销
- ✅ **安全**: 所有请求在同一域下

**适用场景**:
- 个人项目、小团队项目
- 内部管理系统
- 单机部署
- 开发测试环境

**关键要点**:
1. 前端配置必须是 `VITE_API_BASE_URL=/api`
2. 前端构建后文件在 `koalawiki-web/src/main/resources/static/`
3. 所有访问通过同一端口 18081
4. Spring Boot 自动处理静态资源和 API 路由

---

**完成部署后，访问**: http://localhost:18081 或 http://your-server-ip:18081

**需要帮助?** 查看详细文档：
- [DEPLOY_LINUX.md](../DEPLOY_LINUX.md)
- [API_CONFIG.md](../koalawiki-web-vue/API_CONFIG.md)

---

**最后更新**: 2025-01-21
