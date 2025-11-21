# 服务文档库方案A技术方案

## 1. 背景与目标
- 现状：`AIDocument` 仅绑定 `warehouseId`，无法表达“服务/子域”，用户只能按仓库浏览文档。
- 目标：基于提示词模板，为“服务”维度生成与管理文档库，支持按服务检索、统计和权限控制。
- 方案A：在领域模型与数据库中显式引入 `serviceId`、`docType` 等字段，构建“服务文档库”聚合，让提示词、Agent、生成任务和文档都以服务为中心。

## 2. 方案总览
1. **建模**：新增 `ServiceDocumentLibrary` 聚合，引入 `serviceId`、`serviceName`、`docType`、`promptTemplateId`、`agentType` 等属性；`AIDocument` 与 `GenerationTask` 均包含服务信息。
2. **数据层**：迁移 `ai_document` 表，新增 `service_id`、`service_name`、`doc_type`、`prompt_template_id`、`metadata` 列；创建 `service_document_config` 表维护服务清单与扫描范围。
3. **应用层**：`DocumentGenerationService` 在扫描阶段按服务过滤文件，并根据服务配置加载提示词模板；生成接口增加服务参数；任务进度含服务维度。
4. **接口层**：新增 `/warehouses/{id}/services`、`/warehouses/{id}/services/{sid}/ai-documents` 等 REST API，Agent 模板管理接口扩展 `docType` 与 `serviceId` 条件。
5. **前端**：仓库详情页改造成“服务 → 文档库”双栏布局，`AIDocuments` 视图支持服务筛选、模板选择与任务可视化。
6. **迁移**：脚本将已有文档默认归档到 `serviceId = default`，允许后续批量调整；前端在迁移完成前透出“未分类”组。

## 3. 领域模型与聚合
### 3.1 新增/调整实体
- `ServiceDocumentLibrary` (新)
  - `serviceId`、`serviceName`、`warehouseId`
  - `docType`（如 ARCHITECTURE、API_GUIDE、MODULE_GUIDE、CUSTOM）
  - `promptTemplateId`、`agentType`
  - `sourceGlobs`：匹配路径或模块
  - `documents: List<AIDocument>`
- `AIDocument` (现有) → 新增 `serviceId`、`serviceName`、`docType`、`promptTemplateId`、`metadata(Map<String, Object>)`
- `GenerationTask` → 新增 `serviceId`、`docType`
- `AgentTemplate` → 新增 `docType`、`defaultServiceId` 字段，并提供“可绑定多服务”的关联表

### 3.2 领域服务职责
- `ServiceDocumentLibraryService`
  - 维护服务清单 CRUD
  - 校验扫描路径冲突
  - 绑定/解绑提示词模板、Agent 策略
- `DocumentGenerationService`
  - 接收 `serviceId`，按 `sourceGlobs` 扫描
  - 调用 `PromptBuilder.fillTemplate(serviceConfig, codeContext)`
  - 将 `serviceId` 等信息写入 `AIDocument`

## 4. 数据模型
### 4.1 表结构变更
**表 `ai_document`**

| 列 | 类型 | 说明 |
| --- | --- | --- |
| service_id | varchar(64) | 服务 ID，NOT NULL，索引 |
| service_name | varchar(128) | 冗余服务名，便于展示 |
| doc_type | varchar(32) | 文档类别 |
| prompt_template_id | varchar(36) | 关联提示词模板 |
| metadata | json/longtext | 额外维度，如版本/模块 |

**表 `generation_task`**：新增 `service_id`、`doc_type`

**新表 `service_document_config`**

| 列 | 类型 | 说明 |
| --- | --- | --- |
| id | varchar(36) | 主键 |
| warehouse_id | varchar(36) | 对应仓库 |
| service_id | varchar(64) | 业务服务标识（唯一） |
| service_name | varchar(128) | 展示名 |
| description | varchar(255) | 描述 |
| source_globs | json | 生效路径规则数组 |
| prompt_template_id | varchar(36) | 默认模板 |
| agent_type | varchar(32) | 默认 Agent |
| doc_type | varchar(32) | 默认文档类型 |
| enabled | tinyint | 是否启用 |
| created_at / updated_at | datetime | 追踪时间 |

### 4.2 迁移策略
1. Flyway 脚本：为 `ai_document`、`generation_task` 增列并设置默认值（`service_id='default'`、`service_name='Default Service'`、`doc_type='ARCHITECTURE'`）。
2. 创建 `service_document_config` 并导入一条默认记录。
3. 提供管理脚本/后台页面，允许将历史文档重新映射到真实服务，必要时触发重新生成。

## 5. 应用层与流程
### 5.1 服务注册
1. Admin 通过 `POST /warehouses/{wid}/services` 提交服务名、描述、路径规则、默认模板和 Agent。
2. `ServiceDocumentLibraryService` 验证 `sourceGlobs` 是否与其他服务冲突。
3. 保存配置，返回 `serviceId`。

### 5.2 文档生成
1. 前端选择服务，调用 `/warehouses/{wid}/services/{sid}/generate-docs`（批量）或 `/generate-project-doc`（架构类），请求体可覆盖模板/Agent。
2. `AIDocumentController` 校验服务存在并加载配置。
3. `DocumentGenerationService`：
   - 调用 `ServiceDocumentLibraryService.resolveSources(serviceId)` 获取文件列表。
   - 构造上下文（模块、依赖、接口摘要等），并调用 `PromptBuilder.fillTemplate(...)`。
   - 执行 Agent，生成 `AIDocument` 并写入服务字段。
4. 生成批任务 `GenerationTask` 记录 `serviceId` 进度，供前端轮询。

### 5.3 文档查询
- 列表：`GET /warehouses/{wid}/services/{sid}/ai-documents?page=&size=&docType=&status=&keyword=`。
- 统计：`GET /warehouses/{wid}/services/{sid}/doc-stats`，聚合成功率、最近更新时间、Agent 使用次数等。

## 6. Prompt 与 Agent
- `DocumentPromptBuilder` 增加 `buildServicePrompt(template, context)`，支持变量：`{{serviceName}}`、`{{docType}}`、`{{filePath}}`、`{{summary}}`、`{{dependencies}}`。
- 模板管理接口新增 `docType`、`serviceId` 过滤，并允许把模板绑定到多个服务（多对多表 `service_prompt_binding`）。
- Agent 侧：`AIAgentFactory.getAgent(agentType)` 前校验是否在模板白名单；必要时提供“策略模式”以实现同一服务多 Agent 协作（例如架构=Claude，接口=Codex）。

## 7. API 设计

| Endpoint | Method | 说明 |
| --- | --- | --- |
| `/warehouses/{wid}/services` | GET | 返回服务配置及文档统计摘要 |
| `/warehouses/{wid}/services` | POST | 创建服务（body: serviceId、name、sourceGlobs、templateId、docType、agentType） |
| `/warehouses/{wid}/services/{sid}` | PUT | 更新服务配置 |
| `/warehouses/{wid}/services/{sid}` | DELETE | 删除服务（可选保留文档或清理） |
| `/warehouses/{wid}/services/{sid}/generate-docs` | POST | 触发批量生成，响应 `taskId` |
| `/warehouses/{wid}/services/{sid}/generate-project-doc` | POST | 触发架构文档生成，响应 `documentId` |
| `/warehouses/{wid}/services/{sid}/ai-documents` | GET | 分页查询文档，支持 status/docType/keyword 过滤 |
| `/warehouses/{wid}/services/{sid}/doc-stats` | GET | 服务级统计 |
| `/generation-tasks` | GET | 支持 `warehouseId`、`serviceId` 过滤，提供前端任务看板数据 |
| `/ai-prompt-template` | GET/POST/PUT | 增加 `docType`、`serviceId` 查询 / 绑定能力 |

## 8. 前端改造
1. **服务概览**：`Repository.vue` 左侧展示服务列表（状态、默认模板、最后生成时间），点击进入 `ServiceDocuments` 子路由。
2. **文档列表**：`AIDocuments.vue` 接收 `warehouseId + serviceId`，搜索框将 `keyword` 传给 API，顶部提供 `docType` tab 与状态过滤。
3. **生成弹窗**：统一组件 `GenerateDocDialog`，可选择模板/Agent/文档类型，提交到新接口并通过通知+任务抽屉展示进度。
4. **Agent 管理**：增加 docType/服务筛选，显示绑定关系，可在卡片内一键绑定/解绑服务。
5. **默认服务兼容**：在迁移阶段展示“未分类文档”分组，引导用户配置服务并重新生成。

## 9. 测试策略
- **单元**：`ServiceDocumentLibraryServiceTest`（路径冲突、绑定校验）、`DocumentPromptBuilderTest`（模板变量渲染）、`DocumentGenerationServiceTest`（serviceId 透传）。
- **集成**：REST 套件覆盖服务 CRUD、触发生成、文档查询、统计接口；使用 Mock Agent 确认数据写入正确字段。
- **端到端**：脚本模拟“创建服务 → 绑定模板 → 触发生成 → 前端查询”全链路，验证 UI 渲染与任务轮询。
- **迁移验证**：执行 Flyway 后，检查旧文档是否落在 `default` 服务；随机抽取文档确认查询/渲染正常。

## 10. 风险与备选
| 风险 | 描述 | 缓解措施 |
| --- | --- | --- |
| 历史文档缺少服务信息 | 无法自动归属真实服务 | 先归档到 `default`，提供批量迁移脚本与 UI 辅助 |
| 服务路径冲突 | 多个服务匹配同一文件 | 注册时静态校验 + 控制台提示；允许“共享模式”并记录优先级 |
| 模板频繁调整 | 生产模板易被误改 | 为模板引入版本号与发布流程；任务记录模板版本 |
| 需求变更导致推迟 schema 变更 | 需要临时方案 | 备用方案B：仍写 metadata JSON，后续迁移到字段 |

## 11. 实施步骤
1. **数据库迁移**：编写 Flyway 脚本，更新实体/Repository，部署到测试环境。
2. **服务配置模块**：实现 `ServiceDocumentLibraryService`、JPA 仓储及 REST Controller，完成基础 CRUD。
3. **模板与 Agent 扩展**：升级 PromptBuilder 与 Agent 接口，支持 service/docType 维度。
4. **生成流程改造**：`AIDocumentController` 与 `DocumentGenerationService` 接入 serviceId，新增路径解析逻辑与任务记录。
5. **前端联调**：实现服务概览、文档列表筛选、生成弹窗与 Agent 绑定功能。
6. **迁移与验证**：执行数据迁移脚本，回归关键 API、E2E 流程，修复发现的问题。
7. **上线与监控**：发布后监控任务失败率、文档生成耗时，收集用户反馈以规划后续功能（权限、推荐等）。

## 12. 任务列表

| 序号 | 任务名称 | 范围与交付物 | 责任角色 | 依赖 |
| --- | --- | --- | --- | --- |
| T1 | 数据库 schema 迁移 | 编写/执行 Flyway 脚本：`ai_document`、`generation_task` 加列；创建 `service_document_config`；提供回滚脚本 | 后端/DBA | 无 |
| T2 | 领域与仓储改造 | 更新 `AIDocument`、`GenerationTask`、实体与 Repository，确保 service 字段可读写；补充单元测试 | 后端 | T1 |
| T3 | 服务配置模块 | 实现 `ServiceDocumentLibraryService`、JPA 仓储与 REST API（CRUD、路径校验、模板绑定）；编写集成测试 | 后端 | T2 |
| T4 | Prompt/Agent 扩展 | PromptBuilder 支持模板变量填充；Agent 模板接口增加 service/docType 过滤与绑定；实现模板版本记录 | 后端 + AI 工程 | T3 |
| T5 | 生成流程重构 | `AIDocumentController`、`DocumentGenerationService` 接入 serviceId，新增 `resolveSources`、任务记录；覆盖批量与架构生成 | 后端 | T3, T4 |
| T6 | 前端仓库/文档改造 | 仓库详情服务概览、`AIDocuments` 服务筛选、生成弹窗、任务抽屉；Agent 页增加绑定能力 | 前端 | T3-T5 |
| T7 | 数据迁移与工具 | 默认服务导入、历史文档归档；提供脚本/后台操作以批量调整 serviceId；编写使用指南 | 后端 + 运维 | T1-T5 |
| T8 | 测试与验收 | 覆盖单元/集成/E2E，用 Mock Agent 验证流程；整理测试报告与回归 Checklist | QA | T1-T7 |
| T9 | 上线与监控 | 制定发布方案、开关控制、监控指标（任务失败率、生成耗时）；收集用户反馈，准备下一迭代 backlog | 后端 + 运维 + PM | T8 |
