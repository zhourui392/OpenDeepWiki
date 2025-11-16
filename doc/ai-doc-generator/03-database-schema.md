# 数据库设计

> 文档来源：基于核心模块需求，设计数据库Schema
>
> 最后更新：2025-11-16

## 1. ER图

```
┌─────────────────┐          ┌──────────────────┐          ┌──────────────────┐
│   warehouse     │ 1      * │  ai_document     │ *      1 │ document_version │
│─────────────────│◄─────────┤──────────────────┤◄─────────┤──────────────────│
│ id              │          │ id               │          │ id               │
│ name            │          │ warehouse_id     │          │ document_id      │
│ address         │          │ source_file      │          │ version          │
│ ...             │          │ title            │          │ content          │
└─────────────────┘          │ content          │          │ created_at       │
                             │ doc_type         │          └──────────────────┘
                             │ status           │
                             └──────────────────┘
                                      │
                                      │ *
                                      ▼ 1
                             ┌──────────────────┐
                             │ generation_task  │
                             │──────────────────│
                             │ id               │
                             │ warehouse_id     │
                             │ status           │
                             │ total_files      │
                             │ completed_files  │
                             └──────────────────┘
```

## 2. 表结构设计

### 2.1 AI文档表 (ai_document)

存储AI生成的文档内容。

```sql
CREATE TABLE ai_document (
    id VARCHAR(36) PRIMARY KEY COMMENT '文档ID',
    warehouse_id VARCHAR(36) NOT NULL COMMENT '仓库ID',
    source_file VARCHAR(500) NOT NULL COMMENT '源代码文件路径',
    source_file_hash VARCHAR(64) COMMENT '源文件内容hash,用于变更检测',

    -- 文档基本信息
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content LONGTEXT NOT NULL COMMENT '文档内容(Markdown格式)',
    doc_type ENUM('CLASS', 'METHOD', 'API', 'MODULE', 'ARCHITECTURE', 'README') NOT NULL COMMENT '文档类型',
    language VARCHAR(20) COMMENT '编程语言',

    -- 代码元素信息(JSON格式)
    code_elements JSON COMMENT '代码元素信息(类名、方法名等)',
    /*
    示例：
    {
        "className": "UserService",
        "packageName": "com.example.service",
        "methods": ["createUser", "updateUser", "deleteUser"],
        "complexity": 15
    }
    */

    -- 生成元数据
    generation_model VARCHAR(50) COMMENT '使用的AI模型(gpt-4/claude-3等)',
    generation_tokens INT COMMENT '消耗的token数',
    generation_time_ms INT COMMENT '生成耗时(毫秒)',
    prompt_template_id VARCHAR(36) COMMENT 'Prompt模板ID',

    -- 文档状态
    status ENUM('GENERATING', 'COMPLETED', 'FAILED', 'ARCHIVED') NOT NULL DEFAULT 'GENERATING',
    error_message TEXT COMMENT '错误信息',
    quality_score DECIMAL(3,2) COMMENT '文档质量评分(0-1)',

    -- 审核和编辑
    is_reviewed BOOLEAN DEFAULT FALSE COMMENT '是否已人工审核',
    reviewed_by VARCHAR(36) COMMENT '审核人ID',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    is_edited BOOLEAN DEFAULT FALSE COMMENT '是否被人工编辑',
    edited_by VARCHAR(36) COMMENT '编辑人ID',
    edited_at TIMESTAMP NULL COMMENT '编辑时间',

    -- 版本管理
    version INT DEFAULT 1 COMMENT '文档版本号',
    parent_version_id VARCHAR(36) COMMENT '父版本ID',

    -- 元数据
    metadata JSON COMMENT '扩展元数据',
    tags VARCHAR(500) COMMENT '标签,逗号分隔',

    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) COMMENT '创建人ID',
    updated_by VARCHAR(36) COMMENT '更新人ID',

    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_source_file (warehouse_id, source_file),
    INDEX idx_doc_type (doc_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_warehouse_source (warehouse_id, source_file),
    FULLTEXT INDEX ft_content (title, content),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成的文档';
```

### 2.2 文档版本表 (document_version)

存储文档的历史版本,支持版本对比和回滚。

```sql
CREATE TABLE document_version (
    id VARCHAR(36) PRIMARY KEY COMMENT '版本ID',
    document_id VARCHAR(36) NOT NULL COMMENT '文档ID',

    version INT NOT NULL COMMENT '版本号',
    content LONGTEXT NOT NULL COMMENT '该版本的文档内容',
    content_diff TEXT COMMENT '与上一版本的diff',

    -- 变更信息
    change_type ENUM('CREATE', 'AI_REGENERATE', 'MANUAL_EDIT', 'MERGE') NOT NULL COMMENT '变更类型',
    change_reason VARCHAR(500) COMMENT '变更原因',
    source_file_hash VARCHAR(64) COMMENT '源文件hash',

    -- 生成信息(如果是AI生成的)
    generation_model VARCHAR(50) COMMENT 'AI模型',
    generation_tokens INT COMMENT 'Token消耗',

    -- 审计信息
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NOT NULL COMMENT '创建人ID',

    INDEX idx_document_id (document_id),
    INDEX idx_version (document_id, version),
    FOREIGN KEY (document_id) REFERENCES ai_document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档版本历史';
```

### 2.3 文档生成任务表 (generation_task)

跟踪批量文档生成任务的进度。

```sql
CREATE TABLE generation_task (
    id VARCHAR(36) PRIMARY KEY COMMENT '任务ID',
    warehouse_id VARCHAR(36) NOT NULL COMMENT '仓库ID',
    task_type ENUM('FULL', 'INCREMENTAL', 'SINGLE_FILE') NOT NULL COMMENT '任务类型',

    -- 任务状态
    status ENUM('PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    progress DECIMAL(5,2) DEFAULT 0.00 COMMENT '进度百分比(0-100)',

    -- 文件统计
    total_files INT DEFAULT 0 COMMENT '总文件数',
    completed_files INT DEFAULT 0 COMMENT '已完成文件数',
    failed_files INT DEFAULT 0 COMMENT '失败文件数',
    skipped_files INT DEFAULT 0 COMMENT '跳过文件数',

    -- 任务配置
    config JSON COMMENT '任务配置',
    /*
    {
        "model": "gpt-4",
        "temperature": 0.3,
        "maxTokensPerFile": 2000,
        "languages": ["java", "python"],
        "excludePatterns": ["test/**", "**/target/**"]
    }
    */

    -- 执行信息
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    error_message TEXT COMMENT '错误信息',

    -- Token统计
    total_tokens_used INT DEFAULT 0 COMMENT '总token消耗',
    estimated_cost DECIMAL(10,4) COMMENT '预估成本(美元)',

    -- 任务详情
    file_list JSON COMMENT '文件列表及处理状态',
    /*
    [
        {
            "path": "src/main/java/Service.java",
            "status": "completed",
            "documentId": "xxx-xxx",
            "tokens": 1500
        }
    ]
    */

    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NOT NULL COMMENT '创建人ID',

    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档生成任务';
```

### 2.4 Prompt模板表 (prompt_template)

管理AI文档生成的Prompt模板。

```sql
CREATE TABLE prompt_template (
    id VARCHAR(36) PRIMARY KEY COMMENT '模板ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    doc_type ENUM('CLASS', 'METHOD', 'API', 'MODULE', 'ARCHITECTURE', 'README') NOT NULL COMMENT '适用文档类型',
    language VARCHAR(20) COMMENT '适用编程语言(NULL表示通用)',

    -- 模板内容
    system_prompt TEXT NOT NULL COMMENT '系统提示词',
    user_prompt_template TEXT NOT NULL COMMENT '用户提示词模板',
    /*
    支持变量替换:
    {{CODE}} - 源代码
    {{CLASS_NAME}} - 类名
    {{METHODS}} - 方法列表
    {{LANGUAGE}} - 编程语言
    ...
    */

    -- 模板配置
    model VARCHAR(50) COMMENT '推荐使用的模型',
    temperature DECIMAL(3,2) DEFAULT 0.30 COMMENT '温度参数',
    max_tokens INT DEFAULT 2000 COMMENT '最大token数',

    -- 版本和状态
    version INT DEFAULT 1 COMMENT '模板版本',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认模板',

    -- 性能指标
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    avg_quality_score DECIMAL(3,2) COMMENT '平均质量评分',
    avg_tokens_used INT COMMENT '平均token消耗',

    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) COMMENT '创建人ID',
    updated_by VARCHAR(36) COMMENT '更新人ID',

    INDEX idx_doc_type (doc_type),
    INDEX idx_is_active (is_active),
    UNIQUE KEY uk_name_version (name, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt模板';
```

### 2.5 Token使用统计表 (token_usage)

记录AI API的token使用情况,用于成本控制和分析。

```sql
CREATE TABLE token_usage (
    id VARCHAR(36) PRIMARY KEY COMMENT '记录ID',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    warehouse_id VARCHAR(36) COMMENT '仓库ID',
    task_id VARCHAR(36) COMMENT '任务ID',

    -- API调用信息
    model VARCHAR(50) NOT NULL COMMENT 'AI模型',
    operation_type ENUM('GENERATE_DOC', 'QA', 'SUMMARY', 'OTHER') NOT NULL COMMENT '操作类型',

    -- Token统计
    prompt_tokens INT NOT NULL COMMENT 'Prompt tokens',
    completion_tokens INT NOT NULL COMMENT 'Completion tokens',
    total_tokens INT NOT NULL COMMENT '总tokens',

    -- 成本
    estimated_cost DECIMAL(10,6) COMMENT '预估成本(美元)',
    /*
    根据模型定价计算:
    gpt-4: $0.03/1K prompt tokens, $0.06/1K completion tokens
    claude-3-opus: $0.015/1K prompt tokens, $0.075/1K completion tokens
    */

    -- 性能
    response_time_ms INT COMMENT '响应时间(毫秒)',

    -- 时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_created_at (created_at),
    INDEX idx_model (model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token使用统计';
```

### 2.6 智能问答记录表 (qa_conversation)

存储用户与AI的问答对话历史。

```sql
CREATE TABLE qa_conversation (
    id VARCHAR(36) PRIMARY KEY COMMENT '对话ID',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    warehouse_id VARCHAR(36) COMMENT '相关仓库ID',

    -- 会话信息
    session_id VARCHAR(36) NOT NULL COMMENT '会话ID(用于关联多轮对话)',
    sequence_num INT NOT NULL COMMENT '对话序号',

    -- 问答内容
    question TEXT NOT NULL COMMENT '用户问题',
    answer TEXT NOT NULL COMMENT 'AI回答',
    context JSON COMMENT '上下文信息',
    /*
    {
        "relatedDocuments": ["doc-id-1", "doc-id-2"],
        "codeSnippets": [...],
        "previousQuestions": [...]
    }
    */

    -- AI信息
    model VARCHAR(50) NOT NULL COMMENT '使用的AI模型',
    tokens_used INT COMMENT 'Token消耗',
    response_time_ms INT COMMENT '响应时间',

    -- 用户反馈
    is_helpful BOOLEAN COMMENT '是否有帮助',
    feedback TEXT COMMENT '用户反馈',

    -- 审计
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能问答记录';
```

## 3. 修改现有表

### 3.1 修改 warehouse 表

添加AI文档生成相关配置。

```sql
ALTER TABLE warehouse ADD COLUMN ai_doc_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用AI文档生成';
ALTER TABLE warehouse ADD COLUMN ai_doc_config JSON COMMENT 'AI文档生成配置';
/*
{
    "preferredModel": "gpt-4",
    "autoGenerate": true,
    "languages": ["java", "python"],
    "excludePatterns": ["test/**"]
}
*/
ALTER TABLE warehouse ADD COLUMN last_doc_generation_at TIMESTAMP NULL COMMENT '最后一次文档生成时间';
ALTER TABLE warehouse ADD COLUMN doc_generation_status ENUM('IDLE', 'GENERATING', 'COMPLETED', 'FAILED') DEFAULT 'IDLE' COMMENT '文档生成状态';
```

### 3.2 修改 document_catalog 表

关联AI生成的文档。

```sql
ALTER TABLE document_catalog ADD COLUMN ai_document_id VARCHAR(36) COMMENT '关联的AI文档ID';
ALTER TABLE document_catalog ADD COLUMN is_ai_generated BOOLEAN DEFAULT FALSE COMMENT '是否AI生成';
ALTER TABLE document_catalog ADD CONSTRAINT fk_ai_document
    FOREIGN KEY (ai_document_id) REFERENCES ai_document(id) ON DELETE SET NULL;
```

## 4. 索引优化建议

### 4.1 复合索引

```sql
-- ai_document表
CREATE INDEX idx_warehouse_status_type ON ai_document(warehouse_id, status, doc_type);
CREATE INDEX idx_reviewed_edited ON ai_document(is_reviewed, is_edited);

-- generation_task表
CREATE INDEX idx_warehouse_status_created ON generation_task(warehouse_id, status, created_at DESC);

-- token_usage表
CREATE INDEX idx_user_created ON token_usage(user_id, created_at);
CREATE INDEX idx_warehouse_model ON token_usage(warehouse_id, model);
```

### 4.2 全文索引

```sql
-- ai_document表已有全文索引
-- 如果使用MySQL 8.0+,可以添加:
ALTER TABLE ai_document ADD FULLTEXT INDEX ft_code_elements (code_elements) WITH PARSER ngram;
```

## 5. 分区策略

对于大数据量表,建议使用分区:

```sql
-- 按月份分区 token_usage 表
ALTER TABLE token_usage PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    -- ...
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## 6. 数据迁移脚本

### 6.1 迁移现有 document_catalog 到 ai_document

```sql
-- 标记现有文档为非AI生成
UPDATE document_catalog SET is_ai_generated = FALSE WHERE ai_document_id IS NULL;

-- 为将来新增的AI文档建立关联
-- 由应用层在生成文档时自动关联
```

## 7. 性能优化

### 7.1 读写分离

- `ai_document`, `document_version`, `qa_conversation` - 高读场景,使用读副本
- `generation_task`, `token_usage` - 高写场景,使用写主库

### 7.2 缓存策略

```java
// Redis缓存热点文档
@Cacheable(value = "ai:document", key = "#documentId", ttl = 3600)
public AIDocument getDocument(String documentId) {
    return repository.findById(documentId);
}

// 缓存生成任务进度
@Cacheable(value = "task:progress", key = "#taskId", ttl = 60)
public TaskProgress getTaskProgress(String taskId) {
    return taskRepository.getProgress(taskId);
}
```

## 8. 数据备份策略

1. **每日全量备份** - 所有表
2. **每小时增量备份** - `ai_document`, `generation_task`
3. **实时归档** - `token_usage` 超过3个月的数据归档到历史表
4. **版本保留策略** - `document_version` 保留最近10个版本,旧版本归档

## 9. 监控指标

```sql
-- 每日生成的文档数
SELECT DATE(created_at) as date, COUNT(*) as doc_count
FROM ai_document
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(created_at);

-- 每日Token消耗
SELECT DATE(created_at) as date, SUM(total_tokens) as total_tokens, SUM(estimated_cost) as total_cost
FROM token_usage
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(created_at);

-- 文档生成成功率
SELECT status, COUNT(*) as count
FROM generation_task
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY status;
```

## 10. 下一步

请继续阅读：
- [AI集成方案](./04-ai-integration.md)
- [API接口设计](./05-api-design.md)
