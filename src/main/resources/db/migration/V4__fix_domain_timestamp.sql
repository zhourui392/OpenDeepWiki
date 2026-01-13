-- 修复domain_info表中的时间戳格式（毫秒时间戳转为日期字符串）

-- 创建临时表
CREATE TABLE domain_info_temp (
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

-- 复制数据，转换时间戳格式
INSERT INTO domain_info_temp (id, warehouse_id, name, description, code, document_content, created_at, updated_at)
SELECT id, warehouse_id, name, description, code, document_content,
    CASE
        WHEN created_at GLOB '[0-9]*' AND LENGTH(created_at) > 10 THEN datetime(CAST(created_at AS INTEGER)/1000, 'unixepoch', 'localtime')
        WHEN created_at GLOB '[0-9]*' THEN datetime(CAST(created_at AS INTEGER), 'unixepoch', 'localtime')
        ELSE COALESCE(created_at, datetime('now'))
    END,
    CASE
        WHEN updated_at GLOB '[0-9]*' AND LENGTH(updated_at) > 10 THEN datetime(CAST(updated_at AS INTEGER)/1000, 'unixepoch', 'localtime')
        WHEN updated_at GLOB '[0-9]*' THEN datetime(CAST(updated_at AS INTEGER), 'unixepoch', 'localtime')
        ELSE COALESCE(updated_at, datetime('now'))
    END
FROM domain_info;

-- 删除旧表
DROP TABLE domain_info;

-- 重命名新表
ALTER TABLE domain_info_temp RENAME TO domain_info;

-- 重建索引
CREATE INDEX IF NOT EXISTS idx_domain_info_warehouse_id ON domain_info(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_domain_info_code ON domain_info(code);
