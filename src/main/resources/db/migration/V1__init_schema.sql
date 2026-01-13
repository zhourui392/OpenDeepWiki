-- =============================================
-- OpenDeepWiki SQLite 数据库初始化脚本
-- 版本: V1
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    avatar TEXT,
    bio TEXT,
    location TEXT,
    website TEXT,
    company TEXT,
    last_login_at TEXT,
    last_login_ip TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT,
    deleted_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    updated_at TEXT,
    is_system_role INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    deleted_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_in_roles (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    role_id TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_user_in_roles_user_id ON user_in_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_in_roles_role_id ON user_in_roles(role_id);

-- 仓库表
CREATE TABLE IF NOT EXISTS warehouses (
    id TEXT PRIMARY KEY,
    organization_name TEXT,
    name TEXT NOT NULL,
    description TEXT,
    address TEXT NOT NULL,
    git_user_name TEXT,
    git_password TEXT,
    email TEXT,
    type TEXT,
    branch TEXT DEFAULT 'main',
    status TEXT NOT NULL DEFAULT 'PENDING',
    error TEXT,
    version TEXT,
    is_embedded INTEGER NOT NULL DEFAULT 0,
    is_recommended INTEGER NOT NULL DEFAULT 0,
    classify TEXT,
    stars INTEGER NOT NULL DEFAULT 0,
    forks INTEGER NOT NULL DEFAULT 0,
    user_id TEXT,
    enable_sync INTEGER NOT NULL DEFAULT 1,
    ai_doc_enabled INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    deleted_at TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_warehouses_name ON warehouses(name);
CREATE INDEX IF NOT EXISTS idx_warehouses_status ON warehouses(status);
CREATE INDEX IF NOT EXISTS idx_warehouses_user_id ON warehouses(user_id);

-- 仓库同步记录表
CREATE TABLE IF NOT EXISTS warehouse_sync_records (
    id TEXT PRIMARY KEY,
    warehouse_id TEXT NOT NULL,
    trigger TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    start_time TEXT,
    end_time TEXT,
    duration_ms INTEGER,
    from_version TEXT,
    to_version TEXT,
    file_count INTEGER,
    updated_file_count INTEGER,
    added_file_count INTEGER,
    deleted_file_count INTEGER,
    error_message TEXT,
    error_stack TEXT,
    progress INTEGER DEFAULT 0,
    details TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT,
    deleted_at TEXT,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_warehouse_sync_records_warehouse_id ON warehouse_sync_records(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_sync_records_status ON warehouse_sync_records(status);

-- AI文档表
CREATE TABLE IF NOT EXISTS ai_document (
    id TEXT PRIMARY KEY,
    warehouse_id TEXT NOT NULL,
    source_file TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'GENERATING',
    agent_type TEXT,
    error_message TEXT,
    service_id TEXT NOT NULL DEFAULT 'default',
    service_name TEXT NOT NULL DEFAULT 'Default Service',
    doc_type TEXT NOT NULL DEFAULT 'ARCHITECTURE',
    prompt_template_id TEXT,
    metadata TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(warehouse_id, source_file),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ai_document_warehouse_id ON ai_document(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_ai_document_status ON ai_document(status);
CREATE INDEX IF NOT EXISTS idx_ai_document_service_id ON ai_document(service_id);
CREATE INDEX IF NOT EXISTS idx_ai_document_doc_type ON ai_document(doc_type);

-- 文档生成任务表
CREATE TABLE IF NOT EXISTS generation_task (
    id TEXT PRIMARY KEY,
    warehouse_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    total_files INTEGER DEFAULT 0,
    completed_files INTEGER DEFAULT 0,
    failed_files INTEGER DEFAULT 0,
    agent_type TEXT,
    service_id TEXT NOT NULL DEFAULT 'default',
    doc_type TEXT NOT NULL DEFAULT 'ARCHITECTURE',
    started_at TEXT,
    completed_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_generation_task_warehouse_id ON generation_task(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_generation_task_status ON generation_task(status);
CREATE INDEX IF NOT EXISTS idx_generation_task_service_id ON generation_task(service_id);

-- 访问日志表
CREATE TABLE IF NOT EXISTS access_log (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    warehouse_id TEXT,
    document_id TEXT,
    action TEXT NOT NULL,
    ip_address TEXT,
    user_agent TEXT,
    request_uri TEXT,
    request_method TEXT,
    response_time INTEGER,
    status_code INTEGER,
    access_time TEXT NOT NULL,
    referer TEXT,
    session_id TEXT,
    request_params TEXT,
    error_message TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_access_log_user_id ON access_log(user_id);
CREATE INDEX IF NOT EXISTS idx_access_log_warehouse_id ON access_log(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_access_log_access_time ON access_log(access_time);

-- 每日统计表
CREATE TABLE IF NOT EXISTS daily_statistics (
    id TEXT PRIMARY KEY,
    statistics_date TEXT NOT NULL,
    warehouse_id TEXT,
    view_count INTEGER,
    unique_user_count INTEGER,
    unique_ip_count INTEGER,
    document_view_count INTEGER,
    search_count INTEGER,
    download_count INTEGER,
    avg_response_time REAL,
    max_response_time INTEGER,
    min_response_time INTEGER,
    total_requests INTEGER,
    success_requests INTEGER,
    failed_requests INTEGER,
    error_rate REAL,
    action_counts TEXT,
    top_documents TEXT,
    top_search_keywords TEXT,
    new_user_count INTEGER,
    active_user_count INTEGER,
    remarks TEXT,
    calculated_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT,
    UNIQUE(warehouse_id, statistics_date),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_daily_statistics_date ON daily_statistics(statistics_date);
CREATE INDEX IF NOT EXISTS idx_daily_statistics_warehouse_id ON daily_statistics(warehouse_id);

-- AI提示词模板表
CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id TEXT PRIMARY KEY,
    prompt_type TEXT NOT NULL,
    agent_type TEXT NOT NULL,
    template_name TEXT NOT NULL,
    template_content TEXT NOT NULL,
    description TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    version TEXT NOT NULL DEFAULT '1.0.0',
    is_default INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(prompt_type, agent_type, version)
);
CREATE INDEX IF NOT EXISTS idx_ai_prompt_template_prompt_type ON ai_prompt_template(prompt_type);
CREATE INDEX IF NOT EXISTS idx_ai_prompt_template_agent_type ON ai_prompt_template(agent_type);

-- 领域信息表
CREATE TABLE IF NOT EXISTS domain_info (
    id TEXT PRIMARY KEY,
    warehouse_id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    document_content TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_domain_info_warehouse_id ON domain_info(warehouse_id);

-- 服务文档配置表
CREATE TABLE IF NOT EXISTS service_document_config (
    id TEXT PRIMARY KEY,
    warehouse_id TEXT NOT NULL,
    domain_id TEXT,
    service_id TEXT NOT NULL,
    service_name TEXT NOT NULL,
    description TEXT,
    document_content TEXT,
    doc_type TEXT NOT NULL DEFAULT 'SERVICE',
    prompt_template_id TEXT,
    agent_type TEXT,
    source_globs TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(warehouse_id, service_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    FOREIGN KEY (domain_id) REFERENCES domain_info(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_service_document_config_service_name ON service_document_config(service_name);
CREATE INDEX IF NOT EXISTS idx_service_document_config_domain_id ON service_document_config(domain_id);

-- 业务流程文档表
CREATE TABLE IF NOT EXISTS business_flow_documents (
    id TEXT PRIMARY KEY,
    keyword TEXT NOT NULL,
    relevance_score INTEGER,
    entry_type TEXT,
    api_path TEXT,
    class_name TEXT,
    method_name TEXT,
    method_signature TEXT,
    call_chain_json TEXT,
    mermaid_diagram TEXT,
    node_count INTEGER,
    max_depth INTEGER,
    related_services TEXT,
    primary_repository TEXT,
    repository_version TEXT,
    dependency_repositories TEXT,
    description TEXT,
    user_id TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT,
    UNIQUE(keyword, repository_version, api_path)
);
CREATE INDEX IF NOT EXISTS idx_business_flow_documents_keyword ON business_flow_documents(keyword);
CREATE INDEX IF NOT EXISTS idx_business_flow_documents_api_path ON business_flow_documents(api_path);
CREATE INDEX IF NOT EXISTS idx_business_flow_documents_class_name ON business_flow_documents(class_name);
CREATE INDEX IF NOT EXISTS idx_business_flow_documents_repository_version ON business_flow_documents(repository_version);
CREATE INDEX IF NOT EXISTS idx_business_flow_documents_user_id ON business_flow_documents(user_id);

-- 初始化数据
INSERT OR IGNORE INTO roles (id, name, description, is_active, is_system_role, created_at) VALUES
('admin-role-uuid', 'Admin', '系统管理员,拥有所有权限', 1, 1, datetime('now')),
('user-role-uuid', 'User', '普通用户,拥有基本权限', 1, 1, datetime('now')),
('guest-role-uuid', 'Guest', '访客,拥有只读权限', 1, 1, datetime('now'));

INSERT OR IGNORE INTO users (id, name, email, password, bio, created_at) VALUES
('default-admin-uuid-0001', 'Administrator', 'admin@koalawiki.com', '123456', '系统默认管理员账号', datetime('now'));

INSERT OR IGNORE INTO user_in_roles (id, user_id, role_id, created_at) VALUES
('admin-role-mapping-0001', 'default-admin-uuid-0001', 'admin-role-uuid', datetime('now'));

-- 初始化AI提示词模板
INSERT OR IGNORE INTO ai_prompt_template (id, prompt_type, agent_type, template_name, template_content, description, is_active, version, is_default, created_at, updated_at) VALUES
('project-analysis-claude-v1', 'project_analysis', 'claude', '项目架构分析提示词(Claude)',
'## 输出要求

**重要**: 请直接输出完整的Markdown格式项目架构文档，不要输出总结或说明，直接从文档标题开始。

文档必须包含以下章节：

# [项目名称] 架构文档

## 1. 项目概述
- 项目的整体功能和定位
- 技术栈分析（基于识别的框架和组件）
- 架构风格（单体/微服务等）

## 2. 模块结构
- 项目的模块划分
- 各模块的职责和功能

## 3. 服务功能清单
- 按功能域对HTTP接口进行分组
- 列出每个功能域的主要接口和功能

## 4. 服务入口汇总
- HTTP接口的功能分类
- Dubbo服务的对外能力
- 定时任务的调度说明
- MQ消费的业务场景

## 5. 核心业务链路
- 识别3-5个核心业务流程
- 描述每个流程的调用链路
- 标注关键的业务节点

## 6. 架构特点和建议
- 当前架构的优点
- 潜在的改进点
- 技术债务提示

**格式要求**：
- 直接输出Markdown文档内容，不要有任何前言或总结
- 使用表格和列表增强可读性
- 使用中文编写
- 重点突出业务价值和架构设计',
'用于项目架构分析的Claude提示词模板', 1, '1.0.0', 1, datetime('now'), datetime('now')),

('class-chinese-claude-v1', 'class_chinese', 'claude', '类文档生成提示词(Claude-中文)',
'请为以下{language}类生成详细的技术文档。

类名: {className}
包名: {packageName}

源代码:
```{language}
{code}
```

请按以下格式生成Markdown文档:

# {className}

## 概述
(用2-3句话描述这个类的作用和职责)

## 核心功能
(列出主要功能点,每个功能点一段简短描述)

## 主要方法
(为每个public方法生成说明,包括:
- 方法签名
- 功能描述
- 参数说明
- 返回值说明
- 简单的使用示例)

## 使用示例
(提供1-2个实际使用示例代码)

## 注意事项
(如有特殊注意事项,列出来)

要求:
1. 使用清晰简洁的中文
2. 代码示例使用{language}语法高亮
3. 重点突出类的设计意图和使用场景
4. 避免过度技术化的术语',
'用于生成中文类文档的Claude提示词模板', 1, '1.0.0', 1, datetime('now'), datetime('now')),

('system-doc-claude-v1', 'SYSTEM_DOC', 'claude', '系统说明文档生成模板',
'## 核心身份

**系统分析师**：精通主流技术栈（Spring生态/分布式架构/云原生），具有丰富的系统分析经验
- 分析系统架构、模块划分和关键决策点
- 理解数据模型、业务规则和开发规范
- 识别系统中的设计模式和最佳实践
- 必须展开抽象类/接口的所有实现子类（≥3个典型实现）
- 追踪跨模块调用链，自动识别关键业务方法（调用深度≥3层）

请根据以下项目信息，生成完整的系统说明文档。

## 项目信息
{projectInfo}

## Maven模块结构
{modules}

## 包结构
{packages}

## 功能列表
{features}

## 启动配置
{startupGuide}

## 测试信息
{testGuide}

## 数据模型
{dataModels}

---

## 输出要求

**重要**: 请直接输出完整的Markdown格式项目架构文档，不要输出总结或说明，直接从文档标题开始。

文档必须包含以下章节：
1. 项目简介
2. 技术栈
3. 模块说明
4. 功能特性
5. 快速开始（包含环境要求、安装步骤、如何在本地运行）
6. 测试（如何进行功能测试）
7. 数据模型（使用表格展示）
8. API文档（如有接口则列出）

要求：
- 使用清晰的Markdown格式
- 代码块使用正确的语言标识
- 使用表格展示结构化数据
- 添加必要的emoji图标增强可读性
- 保持专业和简洁',
'用于生成项目系统说明文档的提示词模板,包含系统分析师角色定位', 1, '2.0.0', 1, datetime('now'), datetime('now'));
