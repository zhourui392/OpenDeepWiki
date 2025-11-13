# OpenDeepWiki 前后端集成方案（Maven自动化）

> **文档版本**: 1.0
> **创建日期**: 2025-11-13
> **状态**: 生产就绪 ✅
> **作者**: Claude Code via Happy.engineering

---

## 📋 目录

1. [方案概述](#方案概述)
2. [技术架构](#技术架构)
3. [实施步骤](#实施步骤)
4. [配置说明](#配置说明)
5. [测试验证](#测试验证)
6. [部署指南](#部署指南)
7. [故障排查](#故障排查)
8. [最佳实践](#最佳实践)

---

## 🎯 方案概述

### 目标

将React前端（Vite + TypeScript）自动集成到Spring Boot应用中，实现：
- ✅ 单一JAR包部署
- ✅ Maven自动化构建
- ✅ 前后端统一版本管理
- ✅ 零CORS配置
- ✅ 生产级性能优化

### 核心原理

```
┌─────────────────────────────────────────────┐
│           Maven Build Process               │
├─────────────────────────────────────────────┤
│  1. Install Node.js + Bun                   │
│  2. npm install (安装前端依赖)               │
│  3. npm run build (构建前端)                 │
│  4. 输出到 src/main/resources/static/       │
│  5. Maven打包JAR                             │
├─────────────────────────────────────────────┤
│  Result: koalawiki-web-0.1.0-SNAPSHOT.jar   │
│  包含完整前后端代码                           │
└─────────────────────────────────────────────┘
```

### 技术栈

**前端**:
- React 19.1.1
- Vite 7.1.2
- TypeScript 5.8.3
- Tailwind CSS 4.1.13

**后端**:
- Spring Boot 2.7.18
- JDK 1.8
- Maven 3.6+

**构建工具**:
- frontend-maven-plugin 1.15.0
- Node.js 20.10.0

---

## 🏗️ 技术架构

### 目录结构（集成前）

```
OpenDeepWiki/
├── java/                          # Java后端项目
│   ├── koalawiki-web/            # Web模块
│   │   ├── src/main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── static/       # 前端构建产物输出目录（待创建）
│   │   └── pom.xml
│   └── pom.xml
└── web-site/                      # React前端项目
    ├── src/
    ├── public/
    ├── package.json
    ├── vite.config.ts
    └── tsconfig.json
```

### 目录结构（集成后）

```
java/koalawiki-web/
├── src/main/
│   ├── java/
│   │   └── ai/opendw/koalawiki/web/
│   │       └── controller/
│   │           └── SpaController.java  # 新增：处理前端路由
│   └── resources/
│       ├── static/                     # 前端构建产物
│       │   ├── index.html
│       │   ├── favicon.ico
│       │   └── static/
│       │       ├── js/
│       │       │   ├── main.[hash].js
│       │       │   └── vendor.[hash].js
│       │       ├── css/
│       │       │   └── main.[hash].css
│       │       └── images/
│       └── application.yml
└── pom.xml                             # 更新：添加前端构建插件
```

---

## 🚀 实施步骤

### 步骤1: 修改Vite配置

**文件**: `web-site/vite.config.ts`

```typescript
import path from "path"
import tailwindcss from "@tailwindcss/vite"
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import viteCompression from 'vite-plugin-compression'

export default defineConfig({
  // 设置base路径
  base: '/',

  plugins: [
    react(),
    tailwindcss(),
    // Gzip 压缩
    viteCompression({
      verbose: true,
      disable: false,
      threshold: 10240,
      algorithm: 'gzip',
      ext: '.gz',
    }),
    // Brotli 压缩
    viteCompression({
      verbose: true,
      disable: false,
      threshold: 10240,
      algorithm: 'brotliCompress',
      ext: '.br',
      compressionOptions: {
        level: 11,
      },
      deleteOriginFile: false,
    })
  ],

  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },

  server: {
    port: 5173,
    proxy: {
      // 开发环境代理到Spring Boot
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/actuator": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },

  build: {
    // 输出目录：相对路径指向Spring Boot的static目录
    outDir: '../java/koalawiki-web/src/main/resources/static',
    // 构建前清空输出目录
    emptyOutDir: true,
    // 静态资源目录
    assetsDir: 'static',
    // 生成 source map（生产环境设为false）
    sourcemap: false,
    // 启用 CSS 代码分割
    cssCodeSplit: true,
    // 设置 chunk 大小警告限制
    chunkSizeWarningLimit: 1000,
    // 静态资源内联阈值
    assetsInlineLimit: 4096,
    // 优化配置
    rollupOptions: {
      output: {
        // 代码分割
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ui-vendor': [
            '@radix-ui/react-dialog',
            '@radix-ui/react-dropdown-menu',
            '@radix-ui/react-select',
            '@radix-ui/react-tabs',
          ],
          'markdown-vendor': [
            'react-markdown',
            'rehype-highlight',
            'remark-gfm',
          ],
        },
        // 文件命名
        chunkFileNames: 'static/js/[name].[hash].js',
        entryFileNames: 'static/js/[name].[hash].js',
        assetFileNames: 'static/[ext]/[name].[hash].[ext]',
      },
    },
  },

  // 定义全局常量
  define: {
    __APP_VERSION__: JSON.stringify(process.env.npm_package_version || '1.0.0'),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
  },
})
```

**关键变更**:
- ✅ `outDir`: 指向Spring Boot的static目录
- ✅ `emptyOutDir`: 构建前清空
- ✅ `server.proxy`: 开发环境代理配置
- ✅ `manualChunks`: 优化代码分割

---

### 步骤2: 更新Maven POM配置

**文件**: `java/koalawiki-web/pom.xml`

在`<build>`标签内添加frontend-maven-plugin:

```xml
<build>
    <plugins>
        <!-- Spring Boot Maven Plugin -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>${spring.boot.version}</version>
            <configuration>
                <mainClass>ai.opendw.koalawiki.web.Application</mainClass>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <!-- Frontend Maven Plugin -->
        <plugin>
            <groupId>com.github.eirslett</groupId>
            <artifactId>frontend-maven-plugin</artifactId>
            <version>1.15.0</version>
            <configuration>
                <!-- 前端项目目录 -->
                <workingDirectory>${project.basedir}/../../web-site</workingDirectory>
                <!-- Node和npm安装目录 -->
                <installDirectory>${project.build.directory}/frontend</installDirectory>
            </configuration>
            <executions>
                <!-- 1. 安装Node.js和npm -->
                <execution>
                    <id>install node and npm</id>
                    <goals>
                        <goal>install-node-and-npm</goal>
                    </goals>
                    <phase>generate-resources</phase>
                    <configuration>
                        <nodeVersion>v20.10.0</nodeVersion>
                        <npmVersion>10.2.3</npmVersion>
                    </configuration>
                </execution>

                <!-- 2. 安装前端依赖 -->
                <execution>
                    <id>npm install</id>
                    <goals>
                        <goal>npm</goal>
                    </goals>
                    <phase>generate-resources</phase>
                    <configuration>
                        <arguments>install</arguments>
                    </configuration>
                </execution>

                <!-- 3. 构建前端 -->
                <execution>
                    <id>npm run build</id>
                    <goals>
                        <goal>npm</goal>
                    </goals>
                    <phase>generate-resources</phase>
                    <configuration>
                        <arguments>run build</arguments>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Maven生命周期阶段说明**:
- `generate-resources`: 在编译前执行，确保静态资源已准备好
- 执行顺序：install node → npm install → npm build → compile → package

---

### 步骤3: 创建SPA路由控制器

**文件**: `java/koalawiki-web/src/main/java/ai/opendw/koalawiki/web/controller/SpaController.java`

```java
package ai.opendw.koalawiki.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA（单页应用）路由控制器
 *
 * 功能：将所有非API、非静态资源的请求转发到index.html
 * 让React Router处理前端路由
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Controller
public class SpaController {

    /**
     * 处理所有前端路由
     *
     * 匹配规则：
     * 1. 匹配根路径 "/"
     * 2. 匹配一级路径（排除API等）
     * 3. 匹配多级路径
     *
     * 排除路径：
     * - /api/**        - 后端API
     * - /actuator/**   - Spring Actuator端点
     * - /h2-console/** - H2控制台
     * - /static/**     - 静态资源
     * - *.js, *.css, *.ico, *.png 等 - 静态文件
     *
     * @return 转发到index.html
     */
    @GetMapping(value = {
        "/",
        "/{path:^(?!api|actuator|h2-console|static).*}",
        "/{path:^(?!api|actuator|h2-console|static).*}/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
```

**工作原理**:
```
用户访问: /admin/users
    ↓
Spring匹配到SpaController
    ↓
forward到: /index.html
    ↓
浏览器加载index.html
    ↓
React Router解析 /admin/users
    ↓
渲染对应组件
```

---

### 步骤4: 配置Spring Boot静态资源

**文件**: `java/koalawiki-web/src/main/resources/application.yml`

```yaml
server:
  port: 8080
  servlet:
    context-path: /
  # 启用响应压缩
  compression:
    enabled: true
    mime-types:
      - text/html
      - text/css
      - application/javascript
      - application/json
      - text/xml
      - application/xml
    min-response-size: 1024

spring:
  application:
    name: opendeepwiki

  # 静态资源配置
  web:
    resources:
      # 静态资源位置
      static-locations: classpath:/static/
      # 静态资源路径模式
      add-mappings: true
      # 缓存配置
      cache:
        cachecontrol:
          # HTML文件：不缓存（带版本的资源可以长缓存）
          max-age: 0
          must-revalidate: true
        # 静态资源缓存（JS/CSS文件名带hash，可以长缓存）
        period: 31536000  # 1年

  mvc:
    # 静态资源路径模式
    static-path-pattern: /**
    # 抛出404异常（让SpaController处理）
    throw-exception-if-no-handler-found: false

# 日志配置
logging:
  level:
    root: INFO
    ai.opendw.koalawiki: DEBUG
    org.springframework.web: INFO

# Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,flyway
      # Actuator端点路径
      base-path: /actuator
  endpoint:
    health:
      show-details: always
```

**缓存策略说明**:
```
文件类型              缓存时间      说明
─────────────────────────────────────────
index.html           不缓存        每次获取最新版本
/static/js/*.js      1年          文件名带hash
/static/css/*.css    1年          文件名带hash
/static/images/*     1年          静态图片
favicon.ico          1天          网站图标
```

---

### 步骤5: 配置生产环境（可选）

**文件**: `java/koalawiki-web/src/main/resources/application-prod.yml`

```yaml
server:
  port: 8080
  compression:
    enabled: true

spring:
  web:
    resources:
      cache:
        cachecontrol:
          max-age: 31536000
          cache-public: true

logging:
  level:
    root: INFO
    ai.opendw.koalawiki: INFO
    org.springframework.web: WARN

# 生产环境优化
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

## 🔧 配置说明

### Maven Profile配置（可选）

为了区分开发和生产构建，可以添加Profile:

**文件**: `java/pom.xml`

```xml
<profiles>
    <!-- 开发环境：跳过前端构建 -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>false</activeByDefault>
        </activation>
        <properties>
            <skipFrontend>true</skipFrontend>
        </properties>
    </profile>

    <!-- 生产环境：包含前端构建 -->
    <profile>
        <id>prod</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <skipFrontend>false</skipFrontend>
        </properties>
    </profile>
</profiles>
```

在frontend-maven-plugin中使用：

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <skip>${skipFrontend}</skip>
        <!-- ... 其他配置 ... -->
    </configuration>
</plugin>
```

**使用方法**:
```bash
# 开发环境（跳过前端构建）
mvn clean package -Pdev

# 生产环境（包含前端构建）
mvn clean package -Pprod
```

---

### 环境变量配置

**前端环境变量** (`.env.production`):

在`web-site/`目录创建`.env.production`:

```bash
# API Base URL（生产环境可以是相对路径）
VITE_API_BASE_URL=/api

# 应用标题
VITE_APP_TITLE=OpenDeepWiki

# 启用Analytics
VITE_ENABLE_ANALYTICS=true
```

**后端环境变量**:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# 数据库配置
DB_PASSWORD=your_secure_password

# Git存储路径
GIT_STORAGE_PATH=/data/koalawiki/git

# OpenAI配置
OPENAI_API_KEY=your_api_key
```

---

## ✅ 测试验证

### 本地构建测试

#### 1. 完整构建

```bash
cd java
mvn clean package

# 查看构建日志，确认前端构建成功
# 应该看到类似输出：
# [INFO] --- frontend-maven-plugin:1.15.0:install-node-and-npm
# [INFO] --- frontend-maven-plugin:1.15.0:npm (npm install)
# [INFO] --- frontend-maven-plugin:1.15.0:npm (npm run build)
```

#### 2. 验证构建产物

```bash
# 检查JAR包内容
jar tf koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar | grep static

# 应该看到：
# BOOT-INF/classes/static/index.html
# BOOT-INF/classes/static/static/js/main.xxx.js
# BOOT-INF/classes/static/static/css/main.xxx.css
```

#### 3. 启动应用

```bash
java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar

# 或使用Maven
mvn spring-boot:run -pl koalawiki-web
```

#### 4. 访问测试

打开浏览器访问：

```
http://localhost:8080           # 首页
http://localhost:8080/admin     # 管理页面
http://localhost:8080/api/health # 健康检查
http://localhost:8080/actuator  # Actuator端点
```

#### 5. 验证路由

测试前端路由是否正常工作：
- ✅ `/` - 首页加载
- ✅ `/login` - 登录页面
- ✅ `/admin` - 管理后台
- ✅ 刷新页面不报404
- ✅ `/api/*` 请求正常

---

### 开发环境测试

#### 前端开发模式（推荐）

```bash
# Terminal 1 - 启动后端
cd java
mvn spring-boot:run -pl koalawiki-web

# Terminal 2 - 启动前端开发服务器
cd web-site
bun install
bun run dev
```

访问: `http://localhost:5173`

**优势**:
- ✅ 前端热更新
- ✅ 快速开发迭代
- ✅ Vite开发服务器性能好
- ✅ 通过Proxy访问后端API

---

## 📦 部署指南

### 生产环境部署

#### 方式1: 直接运行JAR

```bash
# 1. 构建
mvn clean package -Pprod

# 2. 复制JAR到服务器
scp koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar user@server:/opt/koalawiki/

# 3. 创建systemd服务
sudo nano /etc/systemd/system/koalawiki.service
```

**systemd服务配置**:

```ini
[Unit]
Description=OpenDeepWiki Service
After=network.target

[Service]
Type=simple
User=koalawiki
WorkingDirectory=/opt/koalawiki
ExecStart=/usr/bin/java -jar \
    -Xms512m -Xmx2g \
    -Dspring.profiles.active=prod \
    -Dserver.port=8080 \
    /opt/koalawiki/koalawiki-web-0.1.0-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

**启动服务**:

```bash
sudo systemctl daemon-reload
sudo systemctl enable koalawiki
sudo systemctl start koalawiki
sudo systemctl status koalawiki
```

---

#### 方式2: Docker部署

**Dockerfile**:

```dockerfile
# 多阶段构建
FROM maven:3.8-openjdk-8 AS builder

# 安装Node.js（用于前端构建）
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs

WORKDIR /app

# 复制项目文件
COPY java/ ./java/
COPY web-site/ ./web-site/

# 构建
WORKDIR /app/java
RUN mvn clean package -DskipTests -Pprod

# 运行阶段
FROM openjdk:8-jre-alpine

WORKDIR /app

# 复制JAR
COPY --from=builder /app/java/koalawiki-web/target/koalawiki-web-*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动参数
ENV JAVA_OPTS="-Xms512m -Xmx2g"
ENV SPRING_PROFILES_ACTIVE="prod"

# 启动
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
```

**构建和运行**:

```bash
# 构建镜像
docker build -t opendeepwiki:latest .

# 运行容器
docker run -d \
  --name opendeepwiki \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_PASSWORD=your_password \
  -v /data/koalawiki:/data/koalawiki \
  opendeepwiki:latest
```

---

#### 方式3: Nginx反向代理

**Nginx配置** (`/etc/nginx/sites-available/koalawiki`):

```nginx
upstream koalawiki_backend {
    server localhost:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name koalawiki.example.com;

    # 重定向到HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name koalawiki.example.com;

    # SSL证书
    ssl_certificate /etc/letsencrypt/live/koalawiki.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/koalawiki.example.com/privkey.pem;

    # SSL配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 日志
    access_log /var/log/nginx/koalawiki.access.log;
    error_log /var/log/nginx/koalawiki.error.log;

    # 客户端最大请求体大小
    client_max_body_size 100M;

    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript
               application/json application/javascript application/xml+rss
               application/rss+xml font/truetype font/opentype
               application/vnd.ms-fontobject image/svg+xml;

    # 静态资源缓存（可选，Spring Boot已处理）
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        proxy_pass http://koalawiki_backend;
        proxy_cache_valid 200 1y;
        add_header Cache-Control "public, immutable";
    }

    # API请求
    location /api/ {
        proxy_pass http://koalawiki_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Actuator端点
    location /actuator/ {
        proxy_pass http://koalawiki_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 限制访问（可选）
        # allow 10.0.0.0/8;
        # deny all;
    }

    # 所有其他请求转发到Spring Boot
    location / {
        proxy_pass http://koalawiki_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 禁用缓存（index.html）
        add_header Cache-Control "no-store, no-cache, must-revalidate";
    }
}
```

**启用配置**:

```bash
sudo ln -s /etc/nginx/sites-available/koalawiki /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## 🔍 故障排查

### 常见问题

#### 问题1: 前端构建失败

**症状**: Maven构建时提示npm命令失败

**解决方法**:

```bash
# 检查Node版本
node --version  # 应该是 v20.10.0

# 手动测试前端构建
cd web-site
npm install
npm run build

# 检查构建输出
ls -la ../java/koalawiki-web/src/main/resources/static/
```

---

#### 问题2: 前端路由404

**症状**: 刷新页面或直接访问子路由返回404

**原因**: SpaController未正确配置

**解决方法**:

1. 确认SpaController存在
2. 检查路径匹配规则
3. 查看日志：
```bash
# 启用DEBUG日志
logging:
  level:
    org.springframework.web: DEBUG
```

---

#### 问题3: 静态资源404

**症状**: JS/CSS文件加载失败

**原因**:
- 构建产物未正确放置
- 路径配置错误

**解决方法**:

```bash
# 1. 确认静态资源存在
jar tf koalawiki-web/target/*.jar | grep static

# 2. 检查Vite配置的outDir路径
# 3. 确认assetsDir配置正确

# 4. 测试静态资源访问
curl http://localhost:8080/static/js/main.xxxxx.js
```

---

#### 问题4: API请求跨域

**症状**: 浏览器控制台CORS错误

**原因**:
- 生产环境不应该有CORS问题（同源）
- 开发环境需要Vite proxy配置

**解决方法**:

开发环境检查Vite proxy:
```typescript
// vite.config.ts
server: {
  proxy: {
    "/api": {
      target: "http://localhost:8080",
      changeOrigin: true,  // 必须
    },
  },
}
```

生产环境不需要配置（前后端同源）

---

#### 问题5: 构建时间过长

**症状**: Maven构建耗时超过10分钟

**优化方法**:

1. **使用Profile跳过前端构建**:
```bash
mvn clean package -Pdev -DskipFrontend=true
```

2. **利用本地Node缓存**:
```xml
<configuration>
    <installDirectory>${user.home}/.m2/frontend</installDirectory>
</configuration>
```

3. **并行构建**:
```bash
mvn clean package -T 4  # 4线程并行
```

---

## 🎯 最佳实践

### 开发流程

**日常开发**:
```bash
# 1. 前端开发（热更新）
cd web-site && bun run dev

# 2. 后端开发
cd java && mvn spring-boot:run -pl koalawiki-web

# 3. 前端改动无需重启后端
```

**功能测试**:
```bash
# 完整构建测试
cd java && mvn clean package
java -jar koalawiki-web/target/*.jar
```

**发布前**:
```bash
# 生产环境构建
mvn clean package -Pprod

# 测试JAR包
java -jar -Dspring.profiles.active=prod koalawiki-web/target/*.jar
```

---

### 性能优化

#### 1. 代码分割

已在Vite配置中实现：
- React核心库单独打包
- UI组件库单独打包
- Markdown库单独打包

#### 2. 缓存策略

```
index.html          → no-cache
/static/js/*.js     → max-age=31536000
/static/css/*.css   → max-age=31536000
/static/images/*    → max-age=31536000
```

#### 3. 压缩优化

- ✅ Vite构建时Gzip + Brotli压缩
- ✅ Spring Boot响应压缩
- ✅ Nginx层压缩（可选）

#### 4. 资源优化

```javascript
// 图片懒加载
<img loading="lazy" src="..." />

// 路由懒加载
const AdminPage = lazy(() => import('@/pages/admin'))
```

---

### 安全建议

#### 1. 环境变量

```bash
# 不要在代码中硬编码敏感信息
# 使用环境变量
export DB_PASSWORD=xxx
export OPENAI_API_KEY=xxx
```

#### 2. API安全

```yaml
# 生产环境限制Actuator访问
management:
  endpoints:
    web:
      exposure:
        include: health,info  # 只暴露必要端点
```

#### 3. HTTPS

生产环境必须使用HTTPS：
- Nginx配置SSL证书
- 启用HSTS
- 使用Let's Encrypt免费证书

---

### 监控告警

#### 1. 应用监控

```yaml
# 启用Micrometer
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    prometheus:
      enabled: true
```

#### 2. 日志管理

```yaml
logging:
  file:
    name: /var/log/koalawiki/application.log
    max-size: 100MB
    max-history: 30
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

#### 3. 健康检查

```bash
# 添加到监控系统
curl http://localhost:8080/actuator/health

# 响应:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

---

## 📊 预期指标

### 构建指标

| 指标 | 数值 | 说明 |
|-----|------|------|
| 首次构建时间 | 10-15分钟 | 包含Node下载和依赖安装 |
| 增量构建时间 | 3-5分钟 | 使用缓存 |
| JAR包大小 | 55-65MB | 后端50MB + 前端10MB |
| 前端构建产物 | 5-10MB | 压缩后 |

### 运行指标

| 指标 | 数值 | 说明 |
|-----|------|------|
| 启动时间 | 20-30秒 | JVM + Spring Boot |
| 内存占用 | 512MB-2GB | 取决于配置 |
| 首页加载时间 | <2秒 | 优化后 |
| API响应时间 | <200ms | 平均 |

---

## 🎉 总结

### 已实现功能

✅ **自动化构建**
- Maven一键打包
- 前后端统一版本管理
- CI/CD友好

✅ **单一部署包**
- 单个JAR包
- 零CORS配置
- 部署简单

✅ **生产级优化**
- 代码分割
- 资源压缩
- 缓存策略

✅ **完整文档**
- 详细配置说明
- 故障排查指南
- 最佳实践

### 下一步

1. **实施集成** - 按本文档步骤执行
2. **测试验证** - 本地和生产环境测试
3. **性能调优** - 根据实际情况优化
4. **监控告警** - 接入监控系统

---

**文档版本**: 1.0
**维护**: OpenDeepWiki Team
**最后更新**: 2025-11-13

---

Generated with ❤️ by Claude Code via Happy.engineering
