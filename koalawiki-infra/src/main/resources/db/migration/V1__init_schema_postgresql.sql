-- =============================================
-- OpenDeepWiki 数据库初始化脚本
-- 版本: V1
-- 描述: 创建所有基础表结构
-- 支持: PostgreSQL 12+
-- =============================================

-- =============================================
-- 1. 用户和权限表
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    avatar VARCHAR(500),
    bio VARCHAR(500),
    location VARCHAR(100),
    website VARCHAR(200),
    company VARCHAR(100),
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID（UUID）';
COMMENT ON COLUMN users.name IS '用户名称';
COMMENT ON COLUMN users.email IS '用户邮箱';
COMMENT ON COLUMN users.password IS '用户密码（加密后）';

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_system SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_roles_created_at ON roles(created_at);

COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.id IS '角色ID（UUID）';
COMMENT ON COLUMN roles.name IS '角色名称';
COMMENT ON COLUMN roles.is_system IS '是否系统角色（不可删除）';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_in_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_in_roles_user_id ON user_in_roles(user_id);
CREATE INDEX idx_user_in_roles_role_id ON user_in_roles(role_id);

COMMENT ON TABLE user_in_roles IS '用户角色关联表';

-- =============================================
-- 2. 仓库表
-- =============================================

-- 仓库表
CREATE TABLE IF NOT EXISTS warehouses (
    id VARCHAR(36) PRIMARY KEY,
    organization_name VARCHAR(100),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    address VARCHAR(500) NOT NULL,
    git_user_name VARCHAR(100),
    git_password VARCHAR(200),
    email VARCHAR(100),
    type VARCHAR(50),
    branch VARCHAR(100) DEFAULT 'main',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error VARCHAR(2000),
    version VARCHAR(50),
    is_embedded SMALLINT NOT NULL DEFAULT 0,
    is_recommended SMALLINT NOT NULL DEFAULT 0,
    classify VARCHAR(50),
    stars INT NOT NULL DEFAULT 0,
    forks INT NOT NULL DEFAULT 0,
    user_id VARCHAR(36),
    enable_sync SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_warehouses_name ON warehouses(name);
CREATE INDEX idx_warehouses_organization ON warehouses(organization_name);
CREATE INDEX idx_warehouses_status ON warehouses(status);
CREATE INDEX idx_warehouses_classify ON warehouses(classify);
CREATE INDEX idx_warehouses_user_id ON warehouses(user_id);
CREATE INDEX idx_warehouses_created_at ON warehouses(created_at);
CREATE INDEX idx_warehouses_is_recommended ON warehouses(is_recommended);

COMMENT ON TABLE warehouses IS '仓库表';

-- 仓库同步记录表
CREATE TABLE IF NOT EXISTS warehouse_sync_records (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    trigger VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    from_version VARCHAR(50),
    to_version VARCHAR(50),
    files_changed INT,
    files_added INT,
    files_deleted INT,
    error_message TEXT,
    error_stack TEXT,
    progress INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

CREATE INDEX idx_warehouse_sync_records_warehouse_id ON warehouse_sync_records(warehouse_id);
CREATE INDEX idx_warehouse_sync_records_status ON warehouse_sync_records(status);
CREATE INDEX idx_warehouse_sync_records_trigger ON warehouse_sync_records(trigger);
CREATE INDEX idx_warehouse_sync_records_start_time ON warehouse_sync_records(start_time);
CREATE INDEX idx_warehouse_sync_records_created_at ON warehouse_sync_records(created_at);

COMMENT ON TABLE warehouse_sync_records IS '仓库同步记录表';

-- =============================================
-- 3. 文档表
-- =============================================

-- 文档表
CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    last_update TIMESTAMP,
    description VARCHAR(2000),
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    git_path VARCHAR(500),
    status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

CREATE INDEX idx_documents_warehouse_id ON documents(warehouse_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at);

COMMENT ON TABLE documents IS '文档表';

-- 文档目录表
CREATE TABLE IF NOT EXISTS document_catalogs (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    parent_id VARCHAR(36),
    title VARCHAR(500) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    "order" INT NOT NULL DEFAULT 0,
    depth INT NOT NULL DEFAULT 0,
    is_directory SMALLINT NOT NULL DEFAULT 0,
    file_size BIGINT,
    file_extension VARCHAR(20),
    content_preview TEXT,
    metadata TEXT,
    version VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_catalogs_warehouse_id ON document_catalogs(warehouse_id);
CREATE INDEX idx_document_catalogs_parent_id ON document_catalogs(parent_id);
CREATE INDEX idx_document_catalogs_path ON document_catalogs(path);
CREATE INDEX idx_document_catalogs_order ON document_catalogs("order");
CREATE INDEX idx_document_catalogs_created_at ON document_catalogs(created_at);

COMMENT ON TABLE document_catalogs IS '文档目录表';

-- 文档目录国际化表
CREATE TABLE IF NOT EXISTS document_catalog_i18n (
    id VARCHAR(36) PRIMARY KEY,
    catalog_id VARCHAR(36) NOT NULL,
    language VARCHAR(10) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content_preview TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (catalog_id, language),
    FOREIGN KEY (catalog_id) REFERENCES document_catalogs(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_catalog_i18n_catalog_id ON document_catalog_i18n(catalog_id);
CREATE INDEX idx_document_catalog_i18n_language ON document_catalog_i18n(language);

COMMENT ON TABLE document_catalog_i18n IS '文档目录国际化表';

-- 文档文件项表
CREATE TABLE IF NOT EXISTS document_file_items (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36),
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    content TEXT,
    source VARCHAR(20),
    "order" INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_file_items_warehouse_id ON document_file_items(warehouse_id);
CREATE INDEX idx_document_file_items_document_id ON document_file_items(document_id);
CREATE INDEX idx_document_file_items_file_path ON document_file_items(file_path);
CREATE INDEX idx_document_file_items_created_at ON document_file_items(created_at);

COMMENT ON TABLE document_file_items IS '文档文件项表';

-- 文档文件项国际化表
CREATE TABLE IF NOT EXISTS document_file_item_i18n (
    id VARCHAR(36) PRIMARY KEY,
    file_item_id VARCHAR(36) NOT NULL,
    language VARCHAR(10) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (file_item_id, language),
    FOREIGN KEY (file_item_id) REFERENCES document_file_items(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_file_item_i18n_file_item_id ON document_file_item_i18n(file_item_id);
CREATE INDEX idx_document_file_item_i18n_language ON document_file_item_i18n(language);

COMMENT ON TABLE document_file_item_i18n IS '文档文件项国际化表';

-- 文档提交记录表
CREATE TABLE IF NOT EXISTS document_commit_records (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36),
    commit_id VARCHAR(255) NOT NULL,
    commit_message TEXT,
    author VARCHAR(100),
    author_email VARCHAR(100),
    commit_time TIMESTAMP,
    files_changed INT,
    additions INT,
    deletions INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_commit_records_warehouse_id ON document_commit_records(warehouse_id);
CREATE INDEX idx_document_commit_records_document_id ON document_commit_records(document_id);
CREATE INDEX idx_document_commit_records_commit_id ON document_commit_records(commit_id);
CREATE INDEX idx_document_commit_records_commit_time ON document_commit_records(commit_time);
CREATE INDEX idx_document_commit_records_author ON document_commit_records(author);

COMMENT ON TABLE document_commit_records IS '文档提交记录表';

-- =============================================
-- 4. 访问日志和统计表
-- =============================================

-- 访问日志表
CREATE TABLE IF NOT EXISTS access_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    warehouse_id VARCHAR(36),
    document_id VARCHAR(36),
    uri VARCHAR(500),
    method VARCHAR(10),
    params TEXT,
    status_code INT,
    response_time INT,
    error_message TEXT,
    ip VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    action_type VARCHAR(20),
    request_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_access_logs_user_id ON access_logs(user_id);
CREATE INDEX idx_access_logs_warehouse_id ON access_logs(warehouse_id);
CREATE INDEX idx_access_logs_document_id ON access_logs(document_id);
CREATE INDEX idx_access_logs_request_time ON access_logs(request_time);
CREATE INDEX idx_access_logs_ip ON access_logs(ip);
CREATE INDEX idx_access_logs_action_type ON access_logs(action_type);
CREATE INDEX idx_access_logs_status_code ON access_logs(status_code);
CREATE INDEX idx_access_logs_created_at ON access_logs(created_at);

COMMENT ON TABLE access_logs IS '访问日志表';

-- 每日统计表
CREATE TABLE IF NOT EXISTS daily_statistics (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36),
    date DATE NOT NULL,
    page_views BIGINT NOT NULL DEFAULT 0,
    unique_visitors BIGINT NOT NULL DEFAULT 0,
    unique_ips BIGINT NOT NULL DEFAULT 0,
    total_requests BIGINT NOT NULL DEFAULT 0,
    success_requests BIGINT NOT NULL DEFAULT 0,
    failed_requests BIGINT NOT NULL DEFAULT 0,
    error_rate DECIMAL(5,2),
    avg_response_time INT,
    max_response_time INT,
    min_response_time INT,
    status_code_stats TEXT,
    action_type_stats TEXT,
    top_documents TEXT,
    top_keywords TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (warehouse_id, date),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

CREATE INDEX idx_daily_statistics_date ON daily_statistics(date);
CREATE INDEX idx_daily_statistics_warehouse_id ON daily_statistics(warehouse_id);

COMMENT ON TABLE daily_statistics IS '每日统计表';

-- =============================================
-- 5. 初始化数据
-- =============================================

-- 插入默认角色
INSERT INTO roles (id, name, description, is_system, created_at) VALUES
('admin-role-uuid', 'Admin', '系统管理员，拥有所有权限', 1, CURRENT_TIMESTAMP),
('user-role-uuid', 'User', '普通用户，拥有基本权限', 1, CURRENT_TIMESTAMP),
('guest-role-uuid', 'Guest', '访客，拥有只读权限', 1, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- =============================================
-- 数据库初始化完成
-- =============================================
