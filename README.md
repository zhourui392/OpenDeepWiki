# OpenDeepWiki - AI代码文档自动生成系统

> 基于AI的Git仓库文档自动生成系统

<img alt="Version" src="https://img.shields.io/badge/version-0.1.0-blue.svg" />
<img alt="License" src="https://img.shields.io/badge/license-MIT-green.svg" />
<img alt="Java" src="https://img.shields.io/badge/java-1.8-orange.svg" />
<img alt="Spring Boot" src="https://img.shields.io/badge/spring--boot-2.7.18-brightgreen.svg" />
<img alt="Vue" src="https://img.shields.io/badge/vue-3.5-42b883.svg" />

---

## 📋 系统简介

OpenDeepWiki是一个AI代码文档自动生成系统,通过集成Claude、Codex等AI工具,自动为代码仓库生成高质量的技术文档。

### 核心特性

- 🚀 **Git仓库集成** - 支持GitHub、GitLab、Gitee等主流平台
- 🤖 **AI文档生成** - 基于Claude/Codex自动生成代码文档
- 📚 **批量处理** - 支持整个仓库的批量文档生成
- 🔄 **自动同步** - 定时同步仓库,跟踪代码变更
- 📊 **统计分析** - 文档生成统计、成功率分析
- 🔐 **权限管理** - 基于角色的访问控制

---

## 🏗️ 系统架构

### 技术栈

**后端 (Java)**
- Spring Boot 2.7.18 (JDK 1.8)
- Spring Data JPA + Hibernate
- Spring Security + JWT
- JGit (Git操作)
- Flyway (数据库迁移)
- H2/MySQL/PostgreSQL

**前端 (Vue)**
- Vue 3.5
- TypeScript
- Vite
- Tailwind CSS v4
- Vue Router

**AI集成**
- Claude CLI
- Codex CLI
- 多Agent支持

### DDD分层架构

```
koalawiki-domain    # 领域模型层 - 实体、值对象、枚举
koalawiki-core      # 核心服务层 - 领域服务、Git集成、AI集成
koalawiki-infra     # 基础设施层 - JPA实体、仓储
koalawiki-app       # 应用服务层 - 业务编排、定时任务
koalawiki-web       # 接口层 - REST API、前端资源
```

---

## 🎯 核心功能

### 1. 仓库管理

- 仓库提交(HTTP/HTTPS/SSH)
- 自动/手动同步
- 同步状态追踪
- 仓库列表查询
- 仓库统计信息

### 2. AI文档生成

#### 2.1 文档生成
- 基于源代码自动生成文档
- 支持多种编程语言
- Markdown格式输出
- 上下文理解

#### 2.2 Agent支持
- Claude Agent (中文文档)
- Codex Agent (英文文档)
- 可扩展的Agent架构

#### 2.3 批量处理
- 整个仓库批量生成
- 异步任务处理
- 进度跟踪
- 失败重试

### 3. 文档管理

- 文档列表查询
- 文档详情查看
- 文档搜索过滤
- 生成统计

### 4. Git集成

- 仓库克隆
- 仓库拉取
- 提交历史查询
- 分支管理

### 5. 用户权限

- 用户注册/登录
- 角色权限管理
- Admin/User/Guest角色

---

## 📦 核心模块

### koalawiki-domain (领域模型层)

**实体**
- `User` - 用户
- `Role` - 角色
- `Warehouse` - 仓库
- `WarehouseSyncRecord` - 同步记录
- `AIDocument` - AI生成的文档
- `GenerationTask` - 文档生成任务

**枚举**
- `WarehouseStatus` - 仓库状态
- `WarehouseSyncStatus` - 同步状态
- `DocumentStatus` - 文档状态
- `AgentType` - AI Agent类型

### koalawiki-core (核心服务层)

**Git服务**
- `GitService` - Git操作服务
  - 仓库克隆、拉取、提交历史
  - HTTP/OAuth/SSH认证支持
  - 进度监控和异常处理

**同步服务**
- `IWarehouseSyncService` - 同步服务接口
- `WarehouseSyncServiceImpl` - 同步服务实现
  - 异步同步(CompletableFuture)
  - 同步记录管理

**AI服务**
- `AIAgent` - Agent接口
- `ClaudeAgent` - Claude CLI封装
- `CodexAgent` - Codex CLI封装
- `AIAgentFactory` - Agent工厂
- `CLIExecutor` - CLI命令执行器
- `DocumentPromptBuilder` - 提示词构建器

### koalawiki-infra (基础设施层)

**JPA实体和仓储**
- `AIDocumentEntity` - AI文档实体
- `GenerationTaskEntity` - 生成任务实体
- `AIDocumentRepository` - 文档仓储
- `GenerationTaskRepository` - 任务仓储
- Flyway数据库迁移脚本

### koalawiki-app (应用服务层)

**应用服务**
- `DocumentGenerationService` - 文档生成服务
  - 单文件生成
  - 批量生成
  - 任务管理

**定时任务**
- `WarehouseAutoSyncTask` - 仓库自动同步

### koalawiki-web (接口层)

**REST API控制器**
- `WarehouseController` - 仓库管理API
- `RepositoryController` - 仓库操作API
- `AIDocumentController` - AI文档API
  - `POST /api/v1/warehouses/{id}/generate-docs` - 生成文档
  - `GET /api/v1/warehouses/{id}/documents` - 文档列表
  - `GET /api/v1/documents/{id}` - 文档详情
  - `GET /api/v1/warehouses/{id}/doc-stats` - 文档统计

---

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- Node.js 20+ (前端开发)
- MySQL 5.7+ / PostgreSQL 12+ (生产环境)
- Claude CLI / Codex CLI (AI文档生成)

### 1. 克隆项目

```bash
git clone https://github.com/your-org/OpenDeepWiki.git
cd OpenDeepWiki
```

### 2. 配置数据库

**使用H2内存数据库（开发）**
```yaml
# 默认配置，无需修改
spring:
  datasource:
    url: jdbc:h2:mem:koalawiki
```

**使用MySQL（生产）**
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE koalawiki CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 配置环境变量
export SPRING_PROFILES_ACTIVE=mysql
export DB_PASSWORD=your_password
```

### 3. 配置AI Agent (可选)

```bash
# Claude CLI配置
export CLAUDE_API_KEY=your_key

# Codex CLI配置
export CODEX_API_KEY=your_key
```

### 4. 编译运行

```bash
# 编译项目
mvn clean package

# 运行应用
java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar

# 或使用Maven运行
mvn spring-boot:run -pl koalawiki-web
```

### 5. 访问应用

- 应用首页: http://localhost:18091
- H2控制台: http://localhost:18091/h2-console
- API健康检查: http://localhost:18091/api/health

---

## 📖 使用指南

### 添加Git仓库

```bash
curl -X POST http://localhost:18091/api/warehouse/SubmitWarehouse \
  -H "Content-Type: application/json" \
  -d '{
    "address": "https://github.com/username/repo.git",
    "branch": "main"
  }'
```

### 生成文档

```bash
curl -X POST http://localhost:18091/api/v1/warehouses/{warehouseId}/generate-docs \
  -H "Content-Type: application/json" \
  -d '{
    "agentType": "claude",
    "language": "chinese"
  }'
```

### 查看文档列表

```bash
curl "http://localhost:18091/api/v1/warehouses/{warehouseId}/documents?page=0&size=20"
```

### 查看文档详情

```bash
curl "http://localhost:18091/api/v1/documents/{documentId}"
```

---

## 📊 数据库结构

### 核心表

**用户相关(3张表)**
- `users` - 用户表
- `roles` - 角色表
- `user_in_roles` - 用户角色关联表

**仓库相关(2张表)**
- `warehouses` - 仓库表
- `warehouse_sync_records` - 同步记录表

**AI文档相关(2张表)**
- `ai_document` - AI生成的文档
- `generation_task` - 文档生成任务

---

## 🔧 配置说明

### 应用配置

**application.yml**
```yaml
server:
  port: 18091

spring:
  datasource:
    url: jdbc:h2:mem:koalawiki

koalawiki:
  git:
    storage-path: /data/koalawiki/git
  sync:
    enabled: true
    cron: "0 0 */6 * * ?"
  ai:
    claude:
      command: claude
    codex:
      command: codex
    default-agent: claude
```

---

## 📁 项目结构

```
OpenDeepWiki/
├── koalawiki-domain/              # 领域模型层
├── koalawiki-core/                # 核心服务层
├── koalawiki-infra/               # 基础设施层
├── koalawiki-app/                 # 应用服务层
├── koalawiki-web/                 # 接口层
├── koalawiki-web-vue/             # Vue前端项目
├── QUICKSTART.md                  # 快速开始指南
└── README.md                      # 本文件
```

---

## 🔒 安全说明

- JWT Token认证
- 基于角色的访问控制(RBAC)
- 环境变量管理敏感配置
- 不在代码中硬编码密钥

---

## 📜 版本历史

### v0.1.0 (2025-11-16)
- ✅ Git集成 (JGit)
- ✅ 仓库同步服务
- ✅ AI文档自动生成 (Claude/Codex)
- ✅ Vue 3前端
- ✅ REST API
- ✅ 数据库迁移

---

## 📄 许可证

MIT License

---

## 🙏 致谢

- Spring Boot团队
- Vue.js社区
- Anthropic (Claude)
- 所有贡献者

---

**最后更新**: 2025-11-16
**维护者**: OpenDeepWiki Team
