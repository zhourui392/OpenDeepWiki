# OpenDeepWiki - 快速开始指南

## 环境要求

- JDK 1.8+
- Maven 3.6+
- Node.js 20+ (前端开发)
- MySQL 5.7+ / PostgreSQL 12+ (生产环境)
- Claude CLI / Codex CLI (AI文档生成,可选)
- IDE（推荐 IntelliJ IDEA，需安装 Lombok 插件）

## 项目结构

```
OpenDeepWiki/
├── pom.xml                      # 父 POM 文件
├── koalawiki-domain/           # 领域模型层
├── koalawiki-core/             # 核心服务层
├── koalawiki-infra/            # 基础设施层
├── koalawiki-app/              # 应用服务层
├── koalawiki-web/              # 接口层（Web入口）
├── koalawiki-web-vue/          # Vue 3 前端
├── README.md                   # 项目说明
└── QUICKSTART.md               # 本文件
```

## 快速启动

### 1. 克隆项目

```bash
git clone https://github.com/your-org/OpenDeepWiki.git
cd OpenDeepWiki
```

### 2. 编译项目

```bash
# 清理并编译
mvn clean compile

# 打包（跳过测试）
mvn clean package -DskipTests
```

### 3. 运行应用

```bash
# 方式1：使用 Maven 插件运行
mvn spring-boot:run -pl koalawiki-web

# 方式2：运行打包后的 JAR
java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar
```

### 4. 访问应用

- 应用首页: http://localhost:18091
- H2控制台: http://localhost:18091/h2-console
- API健康检查: http://localhost:18091/api/health

## 配置说明

### 默认配置（H2内存数据库）

应用默认使用 H2 内存数据库，配置文件位于 `koalawiki-web/src/main/resources/application.yml`

```yaml
server:
  port: 18091

spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:koalawiki
    username: sa
    password:
  h2:
    console:
      enabled: true
```

### MySQL 配置（生产环境）

1. 创建数据库:
```sql
CREATE DATABASE koalawiki CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置环境变量:
```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_PASSWORD=your_password
```

3. 运行应用:
```bash
mvn spring-boot:run -pl koalawiki-web
```

### AI配置（可选）

如果需要使用AI文档生成功能,需要配置AI Agent:

```bash
# Claude CLI
export CLAUDE_API_KEY=your_key

# Codex CLI
export CODEX_API_KEY=your_key
```

配置文件 (`application.yml`):
```yaml
koalawiki:
  ai:
    claude:
      command: claude
    codex:
      command: codex
    default-agent: claude
```

## 前端开发

### 安装依赖

```bash
cd koalawiki-web-vue
npm install
```

### 开发模式

```bash
npm run dev
# 访问 http://localhost:5173
```

### 生产构建

```bash
npm run build
# 构建输出到 dist/ 目录
```

## 访问应用

### 健康检查

```bash
curl http://localhost:18091/api/health
```

响应示例：
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "status": "UP",
    "application": "OpenDeepWiki",
    "version": "0.1.0-SNAPSHOT"
  }
}
```

### H2 控制台

访问：http://localhost:18091/h2-console

连接信息：
- JDBC URL: `jdbc:h2:mem:koalawiki`
- Username: `sa`
- Password: (留空)

### 默认用户

- 邮箱: `admin@koalawiki.com`
- 密码: `123456`

## 使用示例

### 1. 添加仓库

```bash
curl -X POST http://localhost:18091/api/warehouse/SubmitWarehouse \
  -H "Content-Type: application/json" \
  -d '{
    "address": "https://github.com/username/repo.git",
    "branch": "main"
  }'
```

### 2. 生成文档

```bash
curl -X POST http://localhost:18091/api/v1/warehouses/{warehouseId}/generate-docs \
  -H "Content-Type: application/json" \
  -d '{
    "agentType": "claude",
    "language": "chinese"
  }'
```

### 3. 查看文档

```bash
# 文档列表
curl "http://localhost:18091/api/v1/warehouses/{warehouseId}/documents?page=0&size=20"

# 文档详情
curl "http://localhost:18091/api/v1/documents/{documentId}"

# 文档统计
curl "http://localhost:18091/api/v1/warehouses/{warehouseId}/doc-stats"
```

## IDE 配置

### IntelliJ IDEA

1. **安装 Lombok 插件**
   - File → Settings → Plugins
   - 搜索 "Lombok" 并安装
   - 重启 IDE

2. **启用注解处理**
   - File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - 勾选 "Enable annotation processing"

3. **导入项目**
   - File → Open
   - 选择 `pom.xml`
   - 作为 Maven 项目导入

### VSCode

1. **安装扩展**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Vue - Official

2. **打开项目**
   - File → Open Folder
   - 选择项目根目录

## 常见问题

### Q: 编译失败，提示找不到 lombok
A: 确保在对应模块的 pom.xml 中添加了 lombok 依赖，并在IDE中安装了Lombok插件。

### Q: 端口被占用
A: 修改 `application.yml` 中的端口号：
```yaml
server:
  port: 18092  # 改为其他端口
```

### Q: H2 控制台无法访问
A: 检查 `application.yml` 中是否启用了 H2 控制台：
```yaml
spring:
  h2:
    console:
      enabled: true
```

### Q: AI文档生成失败
A:
1. 检查是否安装了Claude CLI或Codex CLI
2. 检查环境变量配置
3. 查看日志获取详细错误信息

## 开发建议

### 代码规范
- 遵循阿里巴巴 Java 开发规范（P3C）
- 使用Lombok减少样板代码
- 编写单元测试

### 日志配置
日志级别配置在 `application.yml`：
```yaml
logging:
  level:
    root: INFO
    ai.opendw.koalawiki: DEBUG
```

### 测试
```bash
# 运行所有测试
mvn test

# 运行单个模块的测试
mvn test -pl koalawiki-core

# 跳过测试
mvn package -DskipTests
```

## 故障排查

### 查看日志
```bash
# 应用日志
tail -f logs/koalawiki.log

# Spring Boot 日志
java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar --debug
```

### 数据库问题
```bash
# 检查数据库连接
curl http://localhost:18091/actuator/health

# 查看H2控制台
访问 http://localhost:18091/h2-console
```

## 下一步

- 查看 [README.md](./README.md) 了解完整功能
- 查看API文档了解接口详情
- 配置AI Agent开始生成文档

## 技术支持

- 项目主页: https://github.com/your-org/OpenDeepWiki
- 问题反馈: https://github.com/your-org/OpenDeepWiki/issues

---

**最后更新**: 2025-11-16
