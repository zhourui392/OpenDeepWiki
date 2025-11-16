# 系统整体架构设计

> 文档来源：基于OpenDeepWiki现有DDD架构，扩展AI文档生成能力
>
> 最后更新：2025-11-16

## 1. 系统架构概览

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          前端层 (Vue 3)                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ 仓库管理 │  │ 文档浏览 │  │ AI生成   │  │ 智能问答 │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└────────────────────────┬────────────────────────────────────────┘
                         │ REST API / WebSocket
┌────────────────────────┴────────────────────────────────────────┐
│                      Web层 (Spring MVC)                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │Warehouse │  │Document  │  │AI Doc    │  │QA        │        │
│  │Controller│  │Controller│  │Controller│  │Controller│        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────┴────────────────────────────────────────┐
│                    应用服务层 (Application)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │Warehouse │  │Document  │  │AI Doc    │  │QA        │        │
│  │Service   │  │Service   │  │Generator │  │Service   │        │
│  └──────────┘  └──────────┘  └────┬─────┘  └──────────┘        │
└─────────────────────────────────┬─┴──────────────────────────────┘
                                  │
┌─────────────────────────────────┴──────────────────────────────┐
│                     核心业务层 (Core)                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │代码解析模块   │  │AI集成模块     │  │文档生成模块   │          │
│  │              │  │              │  │              │          │
│  │ JavaParser   │  │ LLM Client   │  │ MD Generator │          │
│  │ PythonAST    │  │ Prompt Mgr   │  │ Template Eng │          │
│  │ JSParser     │  │ Token Limiter│  │ Version Ctrl │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │同步调度模块   │  │增量更新模块   │  │全文搜索模块   │          │
│  │              │  │              │  │              │          │
│  │ Sync Scheduler│ │ Delta Detect │  │ ES Indexer   │          │
│  │ Job Queue    │  │ Change Track │  │ Search Query │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
┌─────────────────────────────────┴──────────────────────────────┐
│                      领域层 (Domain)                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │Warehouse     │  │Document      │  │CodeElement   │          │
│  │              │  │              │  │              │          │
│  │ - Repository │  │ - Catalog    │  │ - Class      │          │
│  │ - Branch     │  │ - Content    │  │ - Method     │          │
│  │ - SyncRecord │  │ - Version    │  │ - Interface  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐                            │
│  │AIDocument    │  │GenerationTask│                            │
│  │              │  │              │                            │
│  │ - SourceCode │  │ - Status     │                            │
│  │ - Generated  │  │ - Progress   │                            │
│  │ - Metadata   │  │ - ErrorMsg   │                            │
│  └──────────────┘  └──────────────┘                            │
└─────────────────────────────────┬────────────────────────────────┘
                                  │
┌─────────────────────────────────┴──────────────────────────────┐
│                    基础设施层 (Infrastructure)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │PostgreSQL/   │  │Git Client    │  │File Storage  │          │
│  │MySQL         │  │              │  │              │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌────────────────────────────────────────────────┐            │
│  │         外部服务 (External Services)             │            │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐    │            │
│  │  │ OpenAI   │  │ Claude   │  │ Ollama   │    │            │
│  │  │ API      │  │ API      │  │ (Local)  │    │            │
│  │  └──────────┘  └──────────┘  └──────────┘    │            │
│  └────────────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────────┘
```

## 2. 核心技术栈

### 2.1 后端技术栈

| 技术分类 | 技术选型 | 版本 | 用途 |
|---------|---------|------|------|
| 开发语言 | Java | 17 | 主要开发语言 |
| 开发框架 | Spring Boot | 3.2.x | 应用框架 |
| ORM框架 | MyBatis Plus | 3.5.x | 数据持久化 |
| 代码解析 | JavaParser | 3.25.x | Java代码AST解析 |
| | Python AST | Built-in | Python代码解析 |
| | Esprima | 4.0.x | JavaScript解析 |
| AI集成 | OpenAI Java SDK | 0.18.x | OpenAI API调用 |
| | Anthropic Java SDK | TBD | Claude API调用 |
| 缓存 | Caffeine | Built-in | 本地内存缓存 |
| 搜索引擎 | JPA Specification | Built-in | 数据库搜索 |
| 数据库 | PostgreSQL | 15.x | 主数据库 |
| 任务调度 | Spring Task | Built-in | 定时任务 |
| 监控 | Spring Actuator | Built-in | 应用监控 |

### 2.2 前端技术栈

| 技术分类 | 技术选型 | 版本 | 用途 |
|---------|---------|------|------|
| 开发语言 | TypeScript | 5.x | 主要开发语言 |
| 框架 | Vue | 3.3.x | 前端框架 |
| 构建工具 | Vite | 5.x | 构建工具 |
| UI组件库 | Element Plus | 2.4.x | UI组件 |
| 状态管理 | Pinia | 2.1.x | 状态管理 |
| 路由 | Vue Router | 4.x | 路由管理 |
| HTTP客户端 | Axios | 1.6.x | API请求 |
| Markdown渲染 | markdown-it | 13.x | Markdown渲染 |
| 代码高亮 | Prism.js | 1.29.x | 代码语法高亮 |
| 图表可视化 | Mermaid.js | 10.x | 流程图/时序图 |
| WebSocket | socket.io-client | 4.x | 实时通信 |

## 3. 架构分层设计

### 3.1 DDD分层架构

遵循现有的DDD（领域驱动设计）架构：

```
┌─────────────────────────────────────────────────────────┐
│                     展现层 (Presentation)                 │
│                   koalawiki-web / web-vue                │
│  - REST Controllers                                      │
│  - DTO / Request / Response                              │
│  - Vue Components                                        │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    应用服务层 (Application)               │
│                       koalawiki-app                      │
│  - Application Services                                  │
│  - Event Handlers                                        │
│  - Task Coordinators                                     │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                     核心业务层 (Core)                     │
│                      koalawiki-core                      │
│  - Business Logic                                        │
│  - Domain Services                                       │
│  - **AI Document Generator (新增)**                      │
│  - **Code Parser (新增)**                                │
│  - **LLM Integration (新增)**                            │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                      领域层 (Domain)                      │
│                     koalawiki-domain                     │
│  - Domain Models                                         │
│  - Value Objects                                         │
│  - Domain Events                                         │
│  - **AIDocument (新增)**                                 │
│  - **CodeElement (新增)**                                │
│  - **GenerationTask (新增)**                             │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   基础设施层 (Infrastructure)             │
│                      koalawiki-infra                     │
│  - Repository Implementations                            │
│  - External Service Clients                              │
│  - Message Queue                                         │
│  - **LLM API Client (新增)**                             │
│  - **Elasticsearch Client (新增)**                       │
└─────────────────────────────────────────────────────────┘
```

### 3.2 新增模块说明

#### 3.2.1 代码解析模块 (Code Parser)
**位置**: `koalawiki-core/src/main/java/.../parser/`

**职责**:
- 解析多种语言的源代码（Java, Python, JavaScript等）
- 提取代码结构（类、方法、接口等）
- 分析代码依赖关系
- 计算代码复杂度

**核心类**:
```java
ai.opendw.koalawiki.core.parser.CodeParser
ai.opendw.koalawiki.core.parser.JavaCodeParser
ai.opendw.koalawiki.core.parser.PythonCodeParser
ai.opendw.koalawiki.core.parser.CodeElement
```

#### 3.2.2 AI集成模块 (LLM Integration)
**位置**: `koalawiki-core/src/main/java/.../ai/`

**职责**:
- 封装LLM API调用
- 管理Prompt模板
- 控制Token使用
- 处理API限流和重试

**核心类**:
```java
ai.opendw.koalawiki.core.ai.LLMClient
ai.opendw.koalawiki.core.ai.PromptManager
ai.opendw.koalawiki.core.ai.TokenLimiter
```

#### 3.2.3 文档生成模块 (Doc Generator)
**位置**: `koalawiki-core/src/main/java/.../generator/`

**职责**:
- 协调代码解析和AI生成
- 生成Markdown文档
- 管理文档模板
- 处理文档版本

**核心类**:
```java
ai.opendw.koalawiki.core.generator.AIDocumentGenerator
ai.opendw.koalawiki.core.generator.MarkdownTemplate
ai.opendw.koalawiki.core.generator.DocumentVersionManager
```

## 4. 数据流设计

### 4.1 文档生成流程

```
用户触发同步
     ↓
仓库代码拉取 → Git Clone/Pull
     ↓
扫描代码文件 → 识别可处理文件类型
     ↓
代码解析 → AST分析 → 提取代码元素
     ↓
分批处理 → 避免单次处理过多文件
     ↓
AI文档生成 → 调用LLM API → 生成Markdown
     ↓
文档存储 → 保存到数据库 → 建立索引
     ↓
通知前端 → WebSocket推送 → 更新UI
```

### 4.2 增量更新流程

```
定时任务触发
     ↓
检测代码变更 → Git Diff
     ↓
识别变更文件 → 新增/修改/删除
     ↓
仅处理变更文件 → 提高效率
     ↓
更新相关文档 → 保持版本历史
     ↓
更新索引 → Elasticsearch
```

### 4.3 智能问答流程

```
用户提问
     ↓
问题预处理 → 关键词提取
     ↓
向量检索 → 查找相关文档
     ↓
上下文构建 → 组装Prompt
     ↓
调用LLM → 生成答案
     ↓
答案后处理 → 格式化、引用标注
     ↓
返回用户
```

## 5. 部署架构

### 5.1 开发环境

```
┌──────────────────────────────────────────────────┐
│              开发者本地环境                        │
│  ┌────────────┐  ┌────────────┐                  │
│  │ Spring Boot│  │ Vue Dev    │                  │
│  │ :18081     │  │ Server     │                  │
│  │            │  │ :5173      │                  │
│  └────────────┘  └────────────┘                  │
│                                                   │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐ │
│  │ PostgreSQL │  │ Redis      │  │ RabbitMQ   │ │
│  │ :5432      │  │ :6379      │  │ :5672      │ │
│  └────────────┘  └────────────┘  └────────────┘ │
└──────────────────────────────────────────────────┘
```

### 5.2 生产环境

```
                    ┌──────────────┐
                    │   Nginx      │
                    │   (负载均衡)  │
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼─────┐      ┌────▼─────┐      ┌────▼─────┐
   │ Spring   │      │ Spring   │      │ Spring   │
   │ Boot #1  │      │ Boot #2  │      │ Boot #3  │
   └────┬─────┘      └────┬─────┘      └────┬─────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼─────┐      ┌────▼─────┐      ┌────▼─────┐
   │PostgreSQL│      │  Redis   │      │ RabbitMQ │
   │ (主从)   │      │ (集群)   │      │ (集群)   │
   └──────────┘      └──────────┘      └──────────┘
```

## 6. 性能和扩展性设计

### 6.1 性能优化策略

1. **缓存策略**
   - Caffeine本地内存缓存
   - 缓存代码解析结果
   - 缓存AI生成结果

2. **异步处理**
   - Spring @Async异步处理
   - 线程池配置优化
   - WebSocket实时推送进度

3. **批量处理**
   - 批量提交AI任务
   - 批量写入数据库
   - JDBC批量操作

4. **限流控制**
   - Guava RateLimiter限流
   - Token使用记录(数据库)
   - 并发任务控制(线程池)

### 6.2 扩展性设计

1. **水平扩展**
   - 无状态服务设计
   - 负载均衡支持
   - 数据库读写分离

2. **垂直扩展**
   - 模块化设计
   - 插件机制
   - 配置化管理

3. **多语言支持**
   - 解析器插件化
   - 统一的代码元素模型
   - 模板化文档生成

## 7. 安全设计

### 7.1 API安全

- JWT认证
- RBAC权限控制
- API限流防刷

### 7.2 数据安全

- 敏感信息加密存储
- API Key管理
- 审计日志

### 7.3 代码安全

- 代码扫描防注入
- 沙箱执行环境
- 依赖安全检查

## 8. 监控和运维

### 8.1 应用监控

- Spring Actuator健康检查
- Prometheus指标采集
- Grafana可视化

### 8.2 日志管理

- ELK日志收集
- 分级日志记录
- 错误告警

### 8.3 性能监控

- API响应时间
- AI调用统计
- 资源使用情况

## 9. 技术债务和风险

### 9.1 已知风险

1. **AI成本控制** - LLM API调用成本可能较高
2. **生成质量** - AI生成文档质量需要人工审核
3. **性能瓶颈** - 大规模代码仓库处理可能耗时
4. **API稳定性** - 依赖第三方AI服务的稳定性

### 9.2 缓解措施

1. 实施Token使用限额和预算控制
2. 提供人工审核和编辑功能
3. 采用增量更新和异步处理
4. 支持多个LLM供应商，实现降级策略

## 10. 下一步

请继续阅读：
- [核心模块详细设计](./02-core-modules.md)
- [数据库设计](./03-database-schema.md)
- [AI集成方案](./04-ai-integration.md)
