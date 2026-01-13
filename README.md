# KoalaWiki - AI代码文档自动生成系统

> 基于AI的Git仓库文档自动生成系统

---

## 系统简介

KoalaWiki是一个AI代码文档自动生成系统,通过集成Claude等AI工具,自动为代码仓库生成高质量的技术文档。

### 核心特性

- Git仓库集成 - 支持GitHub、GitLab、Gitee等主流平台
- AI文档生成 - 基于Claude自动生成代码文档
- 批量处理 - 支持整个仓库的批量文档生成
- 自动同步 - 定时同步仓库,跟踪代码变更
- 统计分析 - 文档生成统计、成功率分析

---

## 技术栈

- Spring Boot 2.7.18 (JDK 1.8)
- Spring Data JPA + SQLite
- Spring Security
- JGit (Git操作)
- Flyway (数据库迁移)
- Vue 3 + Element Plus (CDN加载)

---

## 项目结构

```
OpenDeepWiki/
├── src/                           # Java源码
│   └── main/
│       ├── java/                  # Java代码
│       │   └── ai/opendw/koalawiki/
│       │       ├── app/           # 应用服务层
│       │       ├── core/          # 核心服务层
│       │       ├── domain/        # 领域模型层
│       │       ├── infra/         # 基础设施层
│       │       └── web/           # 接口层
│       └── resources/
│           ├── static/            # 前端静态资源
│           ├── db/migration/      # 数据库迁移脚本
│           └── application.yml    # 配置文件
├── pom.xml                        # Maven配置
├── deploy.sh                      # 部署脚本
├── quick-deploy.sh                # 快速部署脚本
├── README.md                      # 本文件
└── QUICKSTART.md                  # 快速开始指南
```

---

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+

### 1. 克隆项目

```bash
git clone https://github.com/zhourui392/OpenDeepWiki.git
cd OpenDeepWiki
```

### 2. 编译运行

```bash
# 编译项目
mvn clean package -DskipTests

# 运行应用
java -jar target/koalawiki-0.1.0-SNAPSHOT.jar

# 或使用部署脚本
./deploy.sh deploy
```

### 3. 访问应用

- 应用首页: http://localhost:18081
- 健康检查: http://localhost:18081/actuator/health

---

## 部署脚本

```bash
# 完整部署（编译+启动）
./deploy.sh deploy

# 仅编译
./deploy.sh build

# 启动/停止/重启
./deploy.sh start
./deploy.sh stop
./deploy.sh restart

# 查看状态和日志
./deploy.sh status
./deploy.sh tail
```

---

## 配置说明

配置文件位于 `src/main/resources/application.yml`

```yaml
server:
  port: 18081

spring:
  datasource:
    url: jdbc:sqlite:./data/koalawiki.db

koalawiki:
  git:
    storage-path: ./data/git
```

---

## API示例

### 添加Git仓库

```bash
curl -X POST http://localhost:18081/api/warehouse/SubmitWarehouse \
  -H "Content-Type: application/json" \
  -d '{
    "address": "https://github.com/username/repo.git",
    "branch": "main"
  }'
```

### 生成文档

```bash
curl -X POST http://localhost:18081/api/v1/warehouses/{warehouseId}/generate-docs \
  -H "Content-Type: application/json" \
  -d '{
    "agentType": "claude",
    "language": "chinese"
  }'
```

---

## 许可证

MIT License

---

**最后更新**: 2025-01-13
