# AI代码文档自动生成系统 - 技术设计方案

> 文档来源：基于OpenDeepWiki现有架构，设计AI驱动的代码文档自动生成系统
>
> 创建日期：2025-11-16
>
> 版本：v1.0

## 文档索引

1. [系统整体架构设计](./01-architecture.md) - 系统架构、技术栈、核心模块
2. [核心模块详细设计](./02-core-modules.md) - 代码分析、AI生成、文档管理
3. [数据库设计](./03-database-schema.md) - 数据模型、表结构、索引设计
4. [AI集成方案](./04-ai-integration.md) - LLM选型、Prompt设计、API集成
5. [API接口设计](./05-api-design.md) - RESTful API规范
6. [实施计划与路线图](./06-implementation-roadmap.md) - 分期计划、里程碑

## 项目背景

### 当前问题
- 系统将所有源代码文件作为"文档"展示
- 缺少代码分析和文档生成能力
- 无法自动生成技术文档

### 目标
构建一个**AI驱动的代码文档自动生成系统**，能够：
- 自动分析代码仓库中的源代码
- 利用AI生成高质量技术文档
- 按模块/包结构组织文档
- 支持增量更新和版本管理

## 快速开始

请按照以下顺序阅读设计文档：

1. 先阅读 [系统整体架构](./01-architecture.md) 了解全局设计
2. 深入 [核心模块设计](./02-core-modules.md) 理解实现细节
3. 查看 [数据库设计](./03-database-schema.md) 了解数据结构
4. 理解 [AI集成方案](./04-ai-integration.md) 掌握AI应用
5. 参考 [API设计](./05-api-design.md) 进行前后端对接
6. 根据 [实施计划](./06-implementation-roadmap.md) 开始开发

## 技术栈概览

### 后端
- **语言**: Java 17
- **框架**: Spring Boot 3.x
- **代码解析**: JavaParser, Python AST
- **AI集成**: OpenAI API / Claude API
- **数据库**: PostgreSQL / MySQL
- **缓存**: Caffeine (本地缓存)
- **异步处理**: Spring @Async + 线程池

### 前端
- **框架**: Vue 3 + TypeScript
- **UI组件**: Element Plus / Ant Design Vue
- **Markdown渲染**: markdown-it / marked
- **代码高亮**: Prism.js / highlight.js
- **图表可视化**: Mermaid.js

## 核心特性

### 1. 智能代码分析
- 多语言支持（Java, Python, JavaScript, Go等）
- AST语法树分析
- 依赖关系提取
- 代码复杂度分析

### 2. AI文档生成
- 类/接口文档自动生成
- 方法/函数说明
- 架构设计文档
- API文档
- 流程图/时序图生成

### 3. 文档管理
- 版本控制
- 增量更新
- 全文搜索
- 目录树结构

### 4. 智能问答
- 基于文档的Q&A
- 代码解释
- 最佳实践建议

## 项目结构

```
OpenDeepWiki/
├── doc/ai-doc-generator/          # 技术设计文档
│   ├── 01-architecture.md          # 架构设计
│   ├── 02-core-modules.md          # 核心模块
│   ├── 03-database-schema.md       # 数据库设计
│   ├── 04-ai-integration.md        # AI集成
│   ├── 05-api-design.md            # API设计
│   └── 06-implementation-roadmap.md # 实施计划
├── koalawiki-core/                 # 核心业务逻辑
│   ├── src/main/java/.../ai/       # AI集成模块（新增）
│   ├── src/main/java/.../parser/   # 代码解析模块（新增）
│   └── src/main/java/.../generator/ # 文档生成模块（新增）
├── koalawiki-app/                  # 应用服务层
├── koalawiki-domain/               # 领域模型
├── koalawiki-infra/                # 基础设施层
└── koalawiki-web-vue/              # Vue前端
```

## 下一步

1. 阅读详细设计文档
2. 评审技术方案
3. 准备开发环境
4. 按照实施计划开始开发

## 联系方式

如有疑问，请参考各子文档的详细说明或联系技术负责人。
