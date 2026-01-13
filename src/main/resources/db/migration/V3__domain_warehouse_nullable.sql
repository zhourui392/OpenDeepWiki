-- 允许领域不依赖仓库（全局领域）
-- SQLite不支持直接修改列约束，需要重建表

-- 创建临时表
CREATE TABLE domain_info_new (
    id TEXT PRIMARY KEY,
    warehouse_id TEXT,
    name TEXT NOT NULL,
    description TEXT,
    code TEXT,
    document_content TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

-- 复制数据
INSERT INTO domain_info_new SELECT id, warehouse_id, name, description, code, document_content, created_at, updated_at FROM domain_info;

-- 删除旧表
DROP TABLE domain_info;

-- 重命名新表
ALTER TABLE domain_info_new RENAME TO domain_info;

-- 重建索引
CREATE INDEX IF NOT EXISTS idx_domain_info_warehouse_id ON domain_info(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_domain_info_code ON domain_info(code);
