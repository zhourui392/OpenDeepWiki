# 仓库同步自动生成文档功能

## 功能说明

系统支持在仓库同步完成后自动触发文档生成,实现无人值守的文档更新。

## 工作流程

```
定时任务 (每6小时)
  ↓
查找需要同步的仓库
  ↓
触发 Git 同步
  ↓
克隆/更新代码
  ↓
发布 WarehouseSyncCompletedEvent 事件
  ↓
WarehouseSyncEventListener 监听事件
  ↓
自动调用 DocumentGenerationService.generateForProject()
  ↓
生成架构文档和 README.md
```

## 关键组件

### 1. 定时任务
**类**: `WarehouseAutoSyncTask`
**位置**: `koalawiki-app/src/main/java/ai/opendw/koalawiki/app/task/WarehouseAutoSyncTask.java`

- 默认每6小时执行一次
- 可通过 `koalawiki.sync.cron` 配置执行时间
- 可通过 `koalawiki.sync.enabled` 开关控制

### 2. 同步服务
**类**: `WarehouseSyncServiceImpl`
**位置**: `koalawiki-core/src/main/java/ai/opendw/koalawiki/core/service/WarehouseSyncServiceImpl.java`

- 执行 Git 仓库的克隆和更新
- 同步成功后发布 `WarehouseSyncCompletedEvent` 事件

### 3. 事件监听器 (新增)
**类**: `WarehouseSyncEventListener`
**位置**: `koalawiki-app/src/main/java/ai/opendw/koalawiki/app/service/ai/WarehouseSyncEventListener.java`

- 监听 `WarehouseSyncCompletedEvent` 事件
- 异步执行文档生成任务
- 可通过 `koalawiki.doc.auto-generate` 开关控制

### 4. 文档生成服务
**类**: `DocumentGenerationService`
**位置**: `koalawiki-app/src/main/java/ai/opendw/koalawiki/app/service/ai/DocumentGenerationService.java`

- 并行生成架构文档和 README.md
- 使用 Claude Agent 执行文档生成

## 配置说明

### application.yml 配置项

```yaml
koalawiki:
  # 仓库同步配置
  sync:
    enabled: true               # 是否启用自动同步
    cron: "0 0 */6 * * ?"      # 同步任务执行时间(默认每6小时)
    batch-size: 10             # 每批处理的仓库数量
    max-concurrent: 3          # 最大并发同步数

  # 文档自动生成配置
  doc:
    auto-generate: true        # 是否在仓库同步完成后自动生成文档
    agent-type: claude         # 默认使用的AI代理类型
```

### 配置项说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `koalawiki.sync.enabled` | `true` | 是否启用定时同步任务 |
| `koalawiki.sync.cron` | `0 0 */6 * * ?` | 定时任务执行时间(Cron表达式) |
| `koalawiki.sync.batch-size` | `10` | 每批处理的仓库数量 |
| `koalawiki.sync.max-concurrent` | `3` | 最大并发同步任务数 |
| `koalawiki.doc.auto-generate` | `true` | 是否自动生成文档 |
| `koalawiki.doc.agent-type` | `claude` | 文档生成使用的AI代理 |

## 仓库同步条件

仓库需要满足以下条件才会被自动同步:

1. `enableSync = true` - 启用了自动同步
2. `status = COMPLETED` - 仓库状态正常
3. 距离上次成功同步超过1天
4. 当前未在同步中

## 文档生成内容

每次同步完成后会自动生成:

1. **架构文档** - 包含:
   - 项目概述
   - 模块结构
   - 服务功能清单
   - 服务入口汇总
   - 核心业务链路

2. **README.md** - 包含:
   - 项目简介
   - 技术栈
   - 项目结构
   - 快速开始
   - 核心功能
   - API文档

## 如何禁用自动生成

如果不需要自动生成文档,可以通过以下方式禁用:

```yaml
koalawiki:
  doc:
    auto-generate: false  # 禁用自动文档生成
```

或者禁用整个同步任务:

```yaml
koalawiki:
  sync:
    enabled: false  # 禁用定时同步
```

## 日志监控

相关日志输出:

```
# 定时任务触发
开始执行仓库自动同步任务
找到 X 个需要同步的仓库

# 同步完成
仓库同步任务已触发: warehouseId=xxx, syncRecordId=xxx

# 事件监听
收到仓库同步完成事件,准备生成文档: warehouseId=xxx

# 文档生成成功
仓库文档自动生成成功: warehouseId=xxx, documentId=xxx, title=xxx

# 文档生成失败
仓库文档自动生成失败: warehouseId=xxx, localPath=xxx
```

## 注意事项

1. 文档生成是异步执行的,不会阻塞同步流程
2. 文档生成失败不会影响仓库同步状态
3. 每个仓库的文档生成互不干扰
4. Claude CLI 执行超时时间为10分钟(600秒)

## 手动触发

除了自动触发,也可以通过API手动触发文档生成:

```
POST /api/ai/documents/project
{
  "warehouseId": "xxx",
  "localPath": "/path/to/repo",
  "agentType": "claude"
}
```
