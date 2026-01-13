-- 修复warehouses表中的时间戳格式（毫秒时间戳转为日期字符串）

-- 创建临时表
CREATE TABLE warehouses_temp (
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

-- 复制数据，转换时间戳格式
INSERT INTO warehouses_temp (id, organization_name, name, description, address, git_user_name, git_password, email, type, branch, status, error, version, is_embedded, is_recommended, classify, stars, forks, user_id, enable_sync, ai_doc_enabled, created_at, deleted_at)
SELECT id, organization_name, name, description, address, git_user_name, git_password, email, type, branch, status, error, version, is_embedded, is_recommended, classify, stars, forks, user_id, enable_sync, ai_doc_enabled,
    CASE
        WHEN created_at GLOB '[0-9]*' AND LENGTH(created_at) > 10 THEN datetime(CAST(created_at AS INTEGER)/1000, 'unixepoch', 'localtime')
        WHEN created_at GLOB '[0-9]*' THEN datetime(CAST(created_at AS INTEGER), 'unixepoch', 'localtime')
        ELSE COALESCE(created_at, datetime('now'))
    END,
    CASE
        WHEN deleted_at GLOB '[0-9]*' AND LENGTH(deleted_at) > 10 THEN datetime(CAST(deleted_at AS INTEGER)/1000, 'unixepoch', 'localtime')
        WHEN deleted_at GLOB '[0-9]*' THEN datetime(CAST(deleted_at AS INTEGER), 'unixepoch', 'localtime')
        ELSE deleted_at
    END
FROM warehouses;

-- 删除旧表
DROP TABLE warehouses;

-- 重命名新表
ALTER TABLE warehouses_temp RENAME TO warehouses;

-- 重建索引
CREATE INDEX IF NOT EXISTS idx_warehouses_name ON warehouses(name);
CREATE INDEX IF NOT EXISTS idx_warehouses_status ON warehouses(status);
CREATE INDEX IF NOT EXISTS idx_warehouses_user_id ON warehouses(user_id);
