# 前端 API 配置说明

## 配置方式

前端通过环境变量 `VITE_API_BASE_URL` 配置后端 API 地址。

## 环境配置文件

### 开发环境 `.env.development`
```bash
# 开发环境使用相对路径，通过 Vite proxy 代理到后端
VITE_API_BASE_URL=/api
```

### 生产环境 `.env.production`
```bash
# 生产环境使用完整的后端 API 地址
VITE_API_BASE_URL=http://localhost:18081/api
```

## 部署场景配置

### 场景 1: 前后端同机部署（推荐）

前端构建后的静态文件放在后端的 `static` 目录下，访问同一个端口。

**配置**：
```bash
# .env.production
VITE_API_BASE_URL=/api
```

**优点**：
- 无需配置 CORS
- 无需配置反向代理
- 部署最简单

**部署步骤**：
```bash
cd koalawiki-web-vue

# 编辑 .env.production
echo "VITE_API_BASE_URL=/api" > .env.production

# 构建前端
npm run build

# 构建后端（前端已自动复制到 static 目录）
cd ..
./deploy.sh deploy
```

**访问**：
- 应用地址：http://localhost:18081
- API 地址：http://localhost:18081/api

---

### 场景 2: 前后端分离部署（同服务器）

前端和后端在同一服务器，但分别启动。

**配置**：
```bash
# .env.production
VITE_API_BASE_URL=http://localhost:18081/api
```

**部署步骤**：
```bash
# 1. 启动后端
./deploy.sh start

# 2. 前端构建并使用 nginx 部署
cd koalawiki-web-vue
npm run build
# 将 dist 目录部署到 nginx
```

**Nginx 配置示例**：
```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态文件
    location / {
        root /opt/OpenDeepWiki/koalawiki-web-vue/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理（可选）
    location /api {
        proxy_pass http://localhost:18081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**访问**：
- 前端地址：http://localhost:80
- 后端地址：http://localhost:18081

---

### 场景 3: 前后端分离部署（不同服务器）

前端和后端在不同服务器。

**后端服务器**（例如 192.168.1.100）：
```bash
# 启动后端
./deploy.sh start
```

**前端服务器**（例如 192.168.1.101）：
```bash
# .env.production
VITE_API_BASE_URL=http://192.168.1.100:18081/api

# 构建
npm run build
```

**注意事项**：
1. 后端需要配置 CORS：

```yaml
# application.yml
spring:
  web:
    cors:
      allowed-origins: "http://192.168.1.101"
      allowed-methods: "*"
      allowed-headers: "*"
      allow-credentials: true
```

2. 或者使用 Nginx 反向代理避免 CORS 问题

---

### 场景 4: 使用域名部署

**后端服务器配置**：
```bash
# 后端使用域名
# api.yourdomain.com -> 192.168.1.100:18081
```

**前端配置**：
```bash
# .env.production
VITE_API_BASE_URL=https://api.yourdomain.com/api
```

**Nginx 反向代理配置**：
```nginx
# 后端 API 域名
server {
    listen 443 ssl;
    server_name api.yourdomain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location /api {
        proxy_pass http://localhost:18081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# 前端域名
server {
    listen 443 ssl;
    server_name www.yourdomain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        root /opt/OpenDeepWiki/dist;
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 配置优先级

环境变量加载顺序（优先级从高到低）：

1. `.env.production.local` （生产环境本地配置，**不提交到 Git**）
2. `.env.production` （生产环境配置）
3. `.env.local` （本地配置，**不提交到 Git**）
4. `.env` （默认配置）

## 快速配置

### 步骤 1: 复制配置模板
```bash
cd koalawiki-web-vue
cp .env.production.example .env.production.local
```

### 步骤 2: 编辑配置
```bash
# 根据实际部署情况修改
vim .env.production.local
```

### 步骤 3: 构建前端
```bash
npm run build
```

## 配置验证

构建后，可以在浏览器控制台查看实际使用的 API 地址：

```javascript
// 在浏览器控制台执行
console.log('API Base URL:', import.meta.env.VITE_API_BASE_URL)
```

或检查网络请求：
1. 打开浏览器开发者工具
2. 切换到 Network 标签
3. 刷新页面
4. 查看 API 请求的实际地址

## 常见问题

### Q1: 前端无法访问后端 API
**A:** 检查以下几点：
1. 后端是否正常运行：`curl http://localhost:18081/actuator/health`
2. 前端配置的 API 地址是否正确
3. 是否有 CORS 跨域问题（检查浏览器控制台）
4. 防火墙是否开放端口

### Q2: CORS 跨域错误
**A:** 有三种解决方案：
1. **推荐**：前后端同机部署，使用相对路径
2. 后端配置 CORS（见场景 3）
3. 使用 Nginx 反向代理

### Q3: 如何切换不同环境
**A:**
```bash
# 开发环境（使用 .env.development）
npm run dev

# 生产环境（使用 .env.production）
npm run build

# 自定义环境
npm run build -- --mode staging
```

### Q4: 配置不生效
**A:**
1. 确保环境变量以 `VITE_` 开头
2. 修改配置后重新构建：`npm run build`
3. 清除浏览器缓存

### Q5: 动态配置后端地址
**A:** 可以在运行时从后端获取配置：

```typescript
// src/config.ts
export async function loadConfig() {
  const response = await fetch('/config.json')
  const config = await response.json()
  return config
}

// 在 main.ts 中使用
import { loadConfig } from './config'

loadConfig().then(config => {
  // 使用配置初始化应用
})
```

## 环境变量说明

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `VITE_API_BASE_URL` | 后端 API 基础地址 | `http://localhost:18081/api` |
| `VITE_APP_TITLE` | 应用标题（可选） | `OpenDeepWiki` |
| `VITE_APP_VERSION` | 应用版本（可选） | `0.1.0` |

## 最佳实践

### 1. 本地配置不提交
创建 `.gitignore`：
```
.env.local
.env.*.local
```

### 2. 提供配置模板
提供 `.env.production.example` 作为配置示例

### 3. 文档化配置
在 README 中说明所有配置项

### 4. 验证配置
在构建脚本中添加配置验证：
```bash
#!/bin/bash
if [ -z "$VITE_API_BASE_URL" ]; then
  echo "错误：VITE_API_BASE_URL 未配置"
  exit 1
fi
npm run build
```

---

**最后更新**: 2025-01-21
