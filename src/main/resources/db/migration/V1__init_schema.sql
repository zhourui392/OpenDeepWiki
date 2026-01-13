-- =============================================
-- OpenDeepWiki MySQL 数据库初始化脚本
-- 版本: V1
-- =============================================

-- 仓库表
CREATE TABLE IF NOT EXISTS warehouses (
    id VARCHAR(64) PRIMARY KEY,
    organization_name VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(1024) NOT NULL,
    git_user_name VARCHAR(255),
    git_password VARCHAR(512),
    email VARCHAR(255),
    type VARCHAR(64),
    branch VARCHAR(255) DEFAULT 'main',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    error TEXT,
    version VARCHAR(64),
    is_embedded TINYINT(1) NOT NULL DEFAULT 0,
    is_recommended TINYINT(1) NOT NULL DEFAULT 0,
    classify VARCHAR(64),
    stars INT NOT NULL DEFAULT 0,
    forks INT NOT NULL DEFAULT 0,
    enable_sync TINYINT(1) NOT NULL DEFAULT 1,
    ai_doc_enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    INDEX idx_warehouses_name (name),
    INDEX idx_warehouses_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 仓库同步记录表
CREATE TABLE IF NOT EXISTS warehouse_sync_records (
    id VARCHAR(64) PRIMARY KEY,
    warehouse_id VARCHAR(64) NOT NULL,
    `trigger` VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    start_time DATETIME,
    end_time DATETIME,
    duration_ms BIGINT,
    from_version VARCHAR(64),
    to_version VARCHAR(64),
    file_count INT,
    updated_file_count INT,
    added_file_count INT,
    deleted_file_count INT,
    error_message TEXT,
    error_stack TEXT,
    progress INT DEFAULT 0,
    details TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted_at DATETIME,
    INDEX idx_warehouse_sync_records_warehouse_id (warehouse_id),
    INDEX idx_warehouse_sync_records_status (status),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI文档表
CREATE TABLE IF NOT EXISTS ai_document (
    id VARCHAR(64) PRIMARY KEY,
    warehouse_id VARCHAR(64) NOT NULL,
    source_file VARCHAR(1024) NOT NULL,
    title VARCHAR(512) NOT NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'GENERATING',
    agent_type VARCHAR(64),
    error_message TEXT,
    service_id VARCHAR(64) NOT NULL DEFAULT 'default',
    service_name VARCHAR(255) NOT NULL DEFAULT 'Default Service',
    doc_type VARCHAR(64) NOT NULL DEFAULT 'ARCHITECTURE',
    prompt_template_id VARCHAR(64),
    metadata TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_warehouse_source (warehouse_id, source_file(255)),
    INDEX idx_ai_document_warehouse_id (warehouse_id),
    INDEX idx_ai_document_status (status),
    INDEX idx_ai_document_service_id (service_id),
    INDEX idx_ai_document_doc_type (doc_type),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文档生成任务表
CREATE TABLE IF NOT EXISTS generation_task (
    id VARCHAR(64) PRIMARY KEY,
    warehouse_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_files INT DEFAULT 0,
    completed_files INT DEFAULT 0,
    failed_files INT DEFAULT 0,
    agent_type VARCHAR(64),
    service_id VARCHAR(64) NOT NULL DEFAULT 'default',
    doc_type VARCHAR(64) NOT NULL DEFAULT 'ARCHITECTURE',
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_generation_task_warehouse_id (warehouse_id),
    INDEX idx_generation_task_status (status),
    INDEX idx_generation_task_service_id (service_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 领域信息表
CREATE TABLE IF NOT EXISTS domain_info (
    id VARCHAR(64) PRIMARY KEY,
    warehouse_id VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    code VARCHAR(64),
    document_content LONGTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain_info_warehouse_id (warehouse_id),
    INDEX idx_domain_info_code (code),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 服务文档配置表
CREATE TABLE IF NOT EXISTS service_document_config (
    id VARCHAR(64) PRIMARY KEY,
    warehouse_id VARCHAR(64) NOT NULL,
    domain_id VARCHAR(64),
    service_id VARCHAR(64) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    description TEXT,
    document_content LONGTEXT,
    doc_type VARCHAR(64) NOT NULL DEFAULT 'SERVICE',
    prompt_template_id VARCHAR(64),
    agent_type VARCHAR(64),
    source_globs TEXT,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_commit_id VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_warehouse_service (warehouse_id, service_id),
    INDEX idx_service_document_config_service_name (service_name),
    INDEX idx_service_document_config_domain_id (domain_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    FOREIGN KEY (domain_id) REFERENCES domain_info(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 业务流程文档表
CREATE TABLE IF NOT EXISTS business_flow_documents (
    id VARCHAR(64) PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    relevance_score INT,
    entry_type VARCHAR(64),
    api_path VARCHAR(512),
    class_name VARCHAR(512),
    method_name VARCHAR(255),
    method_signature VARCHAR(1024),
    call_chain_json LONGTEXT,
    mermaid_diagram LONGTEXT,
    node_count INT,
    max_depth INT,
    related_services TEXT,
    primary_repository VARCHAR(512),
    repository_version VARCHAR(64),
    dependency_repositories TEXT,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    UNIQUE KEY uk_keyword_version_api (keyword, repository_version, api_path(255)),
    INDEX idx_business_flow_documents_keyword (keyword),
    INDEX idx_business_flow_documents_api_path (api_path(255)),
    INDEX idx_business_flow_documents_class_name (class_name(255)),
    INDEX idx_business_flow_documents_repository_version (repository_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
