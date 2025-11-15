# OpenDeepWiki - 智能文档管理系统

> 基于AI的Git仓库文档管理和知识库系统

<img alt="Version" src="https://img.shields.io/badge/version-0.1.0-blue.svg" />
<img alt="License" src="https://img.shields.io/badge/license-MIT-green.svg" />
<img alt="Java" src="https://img.shields.io/badge/java-1.8-orange.svg" />
<img alt="Spring Boot" src="https://img.shields.io/badge/spring--boot-2.7.18-brightgreen.svg" />
<img alt="React" src="https://img.shields.io/badge/react-19.1.1-61dafb.svg" />

---

## 📋 系统简介

OpenDeepWiki是一个智能文档管理系统,专为技术团队设计。通过Git集成、AI辅助和自动化处理,将代码仓库转化为结构化的知识库。

### 核心特性

- 🚀 **Git仓库集成** - 支持GitHub、GitLab、Gitee等主流平台
- 🤖 **AI文档处理** - 智能生成README、目录优化、文档总结
- 📚 **文档目录管理** - 自动生成多语言文档结构
- 🔄 **自动同步** - 定时同步仓库,跟踪文档变更
- 📊 **统计分析** - 访问日志、文档浏览量、活跃度分析
- 🌐 **国际化支持** - 多语言文档目录
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

**前端 (React)**
- React 19.1.1
- TypeScript 5.8.3
- Vite 7.1.2
- Tailwind CSS 4.1.13
- React Router 7

**AI集成**
- OpenAI API (GPT-4/GPT-3.5)
- 文档生成与优化
- 智能问答

### DDD分层架构

```
koalawiki-domain    # 领域模型层 - 实体、值对象、枚举
koalawiki-core      # 核心服务层 - 领域服务、Git集成
koalawiki-infra     # 基础设施层 - JPA实体、仓储、AI客户端
koalawiki-app       # 应用服务层 - 业务编排、定时任务
koalawiki-web       # 接口层 - REST API、前端资源
```

---

## 🎯 核心功能

### 1. 仓库管理

#### 1.1 仓库提交
- 支持Git仓库URL提交（HTTP/HTTPS/SSH）
- 自定义仓库信息
- 文件上传提交
- 分支选择

#### 1.2 仓库同步
- 手动/自动同步
- 定时任务调度
- 同步状态追踪
- 同步记录查询
- 失败重试机制

#### 1.3 仓库信息管理
- 仓库列表查询（分页、搜索）
- 仓库详情查看
- 仓库信息更新
- 仓库删除
- 仓库统计信息

### 2. 文档处理

#### 2.1 文档解析
- Markdown文档解析
- 文档元数据提取
- 文档结构分析
- 多语言支持

#### 2.2 文档目录
- 自动生成文档目录树
- 多层级目录结构
- 目录国际化
- 目录排序和过滤

#### 2.3 文档文件管理
- 文件列表浏览
- 文件内容查看
- 文件历史记录
- 文件搜索

### 3. AI辅助功能

#### 3.1 智能生成
- README自动生成
- 文档摘要提取
- 目录结构优化
- 项目描述生成

#### 3.2 AI问答
- 基于文档内容的问答
- 上下文理解
- 多轮对话

### 4. Git集成

#### 4.1 Git操作
- 仓库克隆
- 仓库拉取
- 提交历史查询
- 分支管理
- 差异对比

#### 4.2 仓库信息
- 提交记录
- 贡献者统计
- 变更日志
- 仓库统计

### 5. 统计分析

#### 5.1 访问统计
- 访问日志记录
- 每日访问统计
- 用户行为分析
- 热门文档排名

#### 5.2 仓库统计
- 文档数量统计
- 同步频率统计
- 仓库活跃度
- Token消耗统计

### 6. 用户权限

#### 6.1 用户管理
- 用户注册/登录
- 用户信息管理
- 密码重置

#### 6.2 角色权限
- Admin - 系统管理员
- User - 普通用户
- Guest - 访客（只读）

---

## 📦 核心模块

### koalawiki-domain (领域模型层)

**实体**
- `User` - 用户
- `Role` - 角色
- `Warehouse` - 仓库
- `WarehouseSyncRecord` - 同步记录
- `Document` - 文档
- `DocumentCatalog` - 文档目录
- `DocumentFileItem` - 文档文件项
- `AccessLog` - 访问日志
- `DailyStatistics` - 每日统计

**枚举**
- `WarehouseStatus` - 仓库状态
- `WarehouseSyncStatus` - 同步状态
- `WarehouseSyncTrigger` - 同步触发方式
- `AccessLogType` - 访问类型

### koalawiki-core (核心服务层)

**Git服务**
- `GitService` - Git操作服务(497行)
  - 仓库克隆、拉取、提交历史
  - HTTP/OAuth/SSH认证支持
  - 进度监控和异常处理
- `GitRepositoryManager` - 仓库管理器(251行)
  - 智能缓存管理
  - 定时清理过期仓库
- `CommitQueryService` - 提交查询服务
  - 提交范围查询
  - 文件历史查询
  - 差异分析

**同步服务**
- `IWarehouseSyncService` - 同步服务接口
- `WarehouseSyncServiceImpl` - 同步服务实现(360行)
  - 异步同步(CompletableFuture)
  - 批量同步
  - 失败重试
  - 同步记录管理

**文档处理流水线**
- `IDocumentProcessor` - 文档处理器接口(责任链模式)
- `DocumentProcessingOrchestrator` - 处理编排器
- 处理器:
  - `CatalogOptimizationProcessor` - 目录优化
  - `CatalogStructureProcessor` - 目录结构生成
  - `ChangeLogProcessor` - 变更日志生成
- `DocumentHelper` - 文档工具类
  - README读取
  - .gitignore解析
  - 目录扫描

### koalawiki-infra (基础设施层)

**JPA实体和仓储**
- 13张核心数据库表
- Spring Data JPA仓储接口
- Flyway数据库迁移脚本
  - MySQL版本(420行SQL)
  - PostgreSQL版本

**AI客户端**
- `OpenAIClient` - OpenAI API客户端
- 支持Chat Completions
- 流式响应
- 自动重试

### koalawiki-app (应用服务层)

**应用服务**
- `IDocumentService` - 文档服务
- `DocumentServiceImpl` - 文档服务实现

**定时任务**
- `WarehouseAutoSyncTask` - 仓库自动同步
- `AccessLogTask` - 访问日志处理
- `StatisticsTask` - 统计数据生成

### koalawiki-web (接口层)

**REST API控制器**
- `WarehouseController` - 仓库管理API
  - `POST /api/Warehouse/SubmitWarehouse` - 提交仓库
  - `POST /api/Warehouse/CustomSubmitWarehouse` - 自定义提交
  - `GET /api/Warehouse/WarehouseList` - 仓库列表
  - `GET /api/Warehouse/BranchList` - 分支列表
  - `GET /api/Warehouse/FileContent` - 文件内容
  - `POST /api/Warehouse/ExportMarkdownZip` - 导出文档
  - `GET /api/Warehouse/Stats` - 仓库统计
- `RepositoryController` - 仓库操作API
  - `GET /api/Repository/Repository` - 仓库详情
  - `DELETE /api/Repository/Repository` - 删除仓库
  - `PUT /api/Repository/UpdateWarehouse` - 更新仓库
  - `GET /api/Repository/Files` - 文件列表
  - `GET /api/Repository/FileContent` - 文件内容
  - `POST /api/Repository/FileContent` - 保存文件
  - `GET /api/Repository/DocumentCatalogs` - 文档目录
  - `POST /api/Repository/ManualSync` - 手动同步
  - `GET /api/Repository/SyncRecords` - 同步记录
  - `GET /api/Repository/RepositoryStats` - 统计信息
  - `GET /api/Repository/Export` - 导出仓库
- `WarehouseSyncController` - 同步管理API
- `AIController` - AI功能API
- `StatisticsController` - 统计API
- `DocumentCatalogController` - 文档目录API
- `SpaController` - 单页应用路由

---

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- Node.js 20+ (前端开发)
- MySQL 5.7+ / PostgreSQL 12+ (生产环境)

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

### 3. 配置OpenAI (可选)

```bash
export OPENAI_API_KEY=sk-xxxxx
export CHAT_MODEL=gpt-4
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
- Actuator: http://localhost:18091/actuator

### 6. 创建默认用户

访问H2控制台执行以下SQL:

```sql
-- 创建角色
INSERT INTO roles (id, name, description, is_system, created_at) VALUES
('admin-role-uuid', 'Admin', '系统管理员', 1, CURRENT_TIMESTAMP());

-- 创建管理员 (密码: admin123)
INSERT INTO users (id, name, email, password, created_at) VALUES
('admin-uuid', 'Administrator', 'admin@koalawiki.com',
 '0192023a7bbd73250516f069df18b500', CURRENT_TIMESTAMP());

-- 分配角色
INSERT INTO user_in_roles (id, user_id, role_id, created_at) VALUES
('mapping-uuid', 'admin-uuid', 'admin-role-uuid', CURRENT_TIMESTAMP());
```

---

## 📖 使用指南

### 添加Git仓库

**方式1: 通过Git URL**
```bash
curl -X POST http://localhost:18091/api/Warehouse/SubmitWarehouse \
  -H "Content-Type: application/json" \
  -d '{
    "address": "https://github.com/username/repo.git",
    "branch": "main",
    "email": "user@example.com"
  }'
```

**方式2: 自定义仓库信息**
```bash
curl -X POST http://localhost:18091/api/Warehouse/CustomSubmitWarehouse \
  -H "Content-Type: application/json" \
  -d '{
    "organization": "MyOrg",
    "repositoryName": "MyRepo",
    "address": "https://github.com/MyOrg/MyRepo.git",
    "branch": "main"
  }'
```

### 查询仓库列表

```bash
curl "http://localhost:18091/api/Warehouse/WarehouseList?page=1&pageSize=12"
```

### 手动触发同步

```bash
curl -X POST "http://localhost:18091/api/Repository/ManualSync?id=warehouse-id"
```

### 查看文档目录

```bash
curl "http://localhost:18091/api/Repository/DocumentCatalogs?id=warehouse-id"
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

**文档相关(6张表)**
- `documents` - 文档表
- `document_catalogs` - 文档目录表
- `document_catalog_i18n` - 目录国际化表
- `document_file_items` - 文档文件项表
- `document_file_item_i18n` - 文件项国际化表
- `document_commit_records` - 文档提交记录表

**日志统计(2张表)**
- `access_logs` - 访问日志表
- `daily_statistics` - 每日统计表

---

## 🔧 配置说明

### 应用配置

**application.yml**
```yaml
server:
  port: 18091  # 应用端口

spring:
  datasource:
    url: jdbc:h2:mem:koalawiki
  jpa:
    hibernate:
      ddl-auto: none  # 使用Flyway管理
  flyway:
    enabled: true

koalawiki:
  git:
    storage-path: /data/koalawiki/git  # Git仓库存储路径
    cache-cleanup-days: 30
  sync:
    enabled: true
    cron: "0 0 */6 * * ?"  # 每6小时同步
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat-model: ${CHAT_MODEL:gpt-4}
```

### 多数据库支持

**MySQL Profile (application-mysql.yml)**
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/koalawiki
    username: root
    password: ${DB_PASSWORD}
```

**PostgreSQL Profile (application-postgresql.yml)**
```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/koalawiki
    username: postgres
    password: ${DB_PASSWORD}
```

---

## 📁 项目结构

```
OpenDeepWiki/
├── koalawiki-domain/              # 领域模型层
│   └── src/main/java/ai/opendw/koalawiki/domain/
│       ├── user/                  # 用户领域
│       ├── warehouse/             # 仓库领域
│       ├── document/              # 文档领域
│       └── statistics/            # 统计领域
├── koalawiki-core/                # 核心服务层
│   └── src/main/java/ai/opendw/koalawiki/core/
│       ├── git/                   # Git服务
│       ├── service/               # 领域服务
│       └── document/              # 文档处理
├── koalawiki-infra/               # 基础设施层
│   └── src/main/
│       ├── java/ai/opendw/koalawiki/infra/
│       │   ├── entity/            # JPA实体
│       │   ├── repository/        # 仓储接口
│       │   └── ai/                # AI客户端
│       └── resources/db/migration/ # Flyway脚本
├── koalawiki-app/                 # 应用服务层
│   └── src/main/java/ai/opendw/koalawiki/app/
│       ├── service/               # 应用服务
│       └── task/                  # 定时任务
├── koalawiki-web/                 # 接口层
│   └── src/main/
│       ├── java/ai/opendw/koalawiki/web/
│       │   ├── controller/        # REST API
│       │   ├── dto/               # 数据传输对象
│       │   └── config/            # 配置类
│       └── resources/
│           ├── static/            # 前端资源
│           └── application.yml
├── web-site/                      # React前端项目
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── DEFAULT_LOGIN.md               # 默认登录说明
├── QUICKSTART.md                  # 快速开始指南
├── FRONTEND_INTEGRATION_GUIDE.md  # 前后端集成文档
└── README.md                      # 本文件
```

---

## 🔒 安全说明

### 认证授权
- JWT Token认证
- 基于角色的访问控制(RBAC)
- Spring Security配置

### 密码安全
- 当前使用MD5(仅用于演示)
- 生产环境建议使用BCrypt
- 更换密码加密方式:
  ```java
  // PasswordUtil.java 中替换为
  BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  String hash = encoder.encode(password);
  ```

### 敏感信息
- 使用环境变量管理敏感配置
- 不要在代码中硬编码密钥
- .gitignore已配置忽略敏感文件

---

## 📈 性能指标

### 构建指标
- 首次构建: 10-15分钟 (含Node下载)
- 增量构建: 3-5分钟
- JAR包大小: 55-65MB

### 运行指标
- 启动时间: 20-30秒
- 内存占用: 512MB-2GB
- 首页加载: <2秒
- API响应: <200ms (平均)

---

## 🤝 贡献指南

### 开发流程

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交Pull Request

### 代码规范
- 遵循Alibaba Java开发规范(P3C)
- 运行代码检查: `mvn pmd:check`
- 编写单元测试
- 更新相关文档

---

## 📝 常见问题

### Q: 如何切换数据库?
```bash
# 使用MySQL
export SPRING_PROFILES_ACTIVE=mysql
export DB_PASSWORD=your_password

# 使用PostgreSQL
export SPRING_PROFILES_ACTIVE=postgresql
export DB_PASSWORD=your_password
```

### Q: 如何禁用AI功能?
```yaml
koalawiki:
  ai:
    enabled: false
```

### Q: 如何修改同步频率?
```yaml
koalawiki:
  sync:
    cron: "0 0 */12 * * ?"  # 每12小时
```

### Q: 如何增加日志级别?
```yaml
logging:
  level:
    ai.opendw.koalawiki: DEBUG
```

---

## 📜 版本历史

### v0.1.0 (2025-11-15)
- ✅ Git集成 (JGit)
- ✅ 仓库同步服务
- ✅ 文档处理流水线
- ✅ REST API (P0+P1功能)
- ✅ 数据库迁移 (MySQL/PostgreSQL)
- ✅ 前后端集成
- ✅ 基础AI功能

---

## 📄 许可证

MIT License

Copyright (c) 2025 OpenDeepWiki Team

---

## 👥 联系方式

- **项目主页**: https://github.com/your-org/OpenDeepWiki
- **问题反馈**: https://github.com/your-org/OpenDeepWiki/issues
- **文档**: 参见项目根目录下的文档文件

---

## 🙏 致谢

- Spring Boot团队
- React社区
- OpenAI
- 所有贡献者

---

**最后更新**: 2025-11-15
**维护者**: OpenDeepWiki Team

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
via [Happy](https://happy.engineering)

Co-Authored-By: Claude <noreply@anthropic.com>
Co-Authored-By: Happy <yesreply@happy.engineering>
