-- 修复所有表中的时间戳格式（毫秒时间戳转为日期字符串）

-- 1. 修复warehouse_sync_records表
CREATE TABLE warehouse_sync_records_temp (
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

INSERT INTO warehouse_sync_records_temp SELECT id, warehouse_id, trigger, status,
    CASE WHEN start_time GLOB '[0-9]*' AND LENGTH(start_time) > 10 THEN datetime(CAST(start_time AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN start_time GLOB '[0-9]*' THEN datetime(CAST(start_time AS INTEGER), 'unixepoch', 'localtime')
         ELSE start_time END,
    CASE WHEN end_time GLOB '[0-9]*' AND LENGTH(end_time) > 10 THEN datetime(CAST(end_time AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN end_time GLOB '[0-9]*' THEN datetime(CAST(end_time AS INTEGER), 'unixepoch', 'localtime')
         ELSE end_time END,
    duration_ms, from_version, to_version, file_count, updated_file_count, added_file_count, deleted_file_count, error_message, error_stack, progress, details,
    CASE WHEN created_at GLOB '[0-9]*' AND LENGTH(created_at) > 10 THEN datetime(CAST(created_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN created_at GLOB '[0-9]*' THEN datetime(CAST(created_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE COALESCE(created_at, datetime('now')) END,
    CASE WHEN updated_at GLOB '[0-9]*' AND LENGTH(updated_at) > 10 THEN datetime(CAST(updated_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN updated_at GLOB '[0-9]*' THEN datetime(CAST(updated_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE updated_at END,
    CASE WHEN deleted_at GLOB '[0-9]*' AND LENGTH(deleted_at) > 10 THEN datetime(CAST(deleted_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN deleted_at GLOB '[0-9]*' THEN datetime(CAST(deleted_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE deleted_at END
FROM warehouse_sync_records;

DROP TABLE warehouse_sync_records;
ALTER TABLE warehouse_sync_records_temp RENAME TO warehouse_sync_records;
CREATE INDEX IF NOT EXISTS idx_warehouse_sync_records_warehouse_id ON warehouse_sync_records(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_sync_records_status ON warehouse_sync_records(status);

-- 2. 修复generation_task表
CREATE TABLE generation_task_temp (
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

INSERT INTO generation_task_temp SELECT id, warehouse_id, status, total_files, completed_files, failed_files, agent_type, service_id, doc_type,
    CASE WHEN started_at GLOB '[0-9]*' AND LENGTH(started_at) > 10 THEN datetime(CAST(started_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN started_at GLOB '[0-9]*' THEN datetime(CAST(started_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE started_at END,
    CASE WHEN completed_at GLOB '[0-9]*' AND LENGTH(completed_at) > 10 THEN datetime(CAST(completed_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN completed_at GLOB '[0-9]*' THEN datetime(CAST(completed_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE completed_at END,
    CASE WHEN created_at GLOB '[0-9]*' AND LENGTH(created_at) > 10 THEN datetime(CAST(created_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN created_at GLOB '[0-9]*' THEN datetime(CAST(created_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE COALESCE(created_at, datetime('now')) END
FROM generation_task;

DROP TABLE generation_task;
ALTER TABLE generation_task_temp RENAME TO generation_task;
CREATE INDEX IF NOT EXISTS idx_generation_task_warehouse_id ON generation_task(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_generation_task_status ON generation_task(status);
CREATE INDEX IF NOT EXISTS idx_generation_task_service_id ON generation_task(service_id);

-- 3. 修复ai_document表
CREATE TABLE ai_document_temp (
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

INSERT INTO ai_document_temp SELECT id, warehouse_id, source_file, title, content, status, agent_type, error_message, service_id, service_name, doc_type, prompt_template_id, metadata,
    CASE WHEN created_at GLOB '[0-9]*' AND LENGTH(created_at) > 10 THEN datetime(CAST(created_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN created_at GLOB '[0-9]*' THEN datetime(CAST(created_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE COALESCE(created_at, datetime('now')) END,
    CASE WHEN updated_at GLOB '[0-9]*' AND LENGTH(updated_at) > 10 THEN datetime(CAST(updated_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN updated_at GLOB '[0-9]*' THEN datetime(CAST(updated_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE COALESCE(updated_at, datetime('now')) END
FROM ai_document;

DROP TABLE ai_document;
ALTER TABLE ai_document_temp RENAME TO ai_document;
CREATE INDEX IF NOT EXISTS idx_ai_document_warehouse_id ON ai_document(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_ai_document_status ON ai_document(status);
CREATE INDEX IF NOT EXISTS idx_ai_document_service_id ON ai_document(service_id);
CREATE INDEX IF NOT EXISTS idx_ai_document_doc_type ON ai_document(doc_type);

-- 4. 修复service_document_config表
CREATE TABLE service_document_config_temp (
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

INSERT INTO service_document_config_temp SELECT id, warehouse_id, domain_id, service_id, service_name, description, document_content, doc_type, prompt_template_id, agent_type, source_globs, enabled,
    CASE WHEN created_at GLOB '[0-9]*' AND LENGTH(created_at) > 10 THEN datetime(CAST(created_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN created_at GLOB '[0-9]*' THEN datetime(CAST(created_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE COALESCE(created_at, datetime('now')) END,
    CASE WHEN updated_at GLOB '[0-9]*' AND LENGTH(updated_at) > 10 THEN datetime(CAST(updated_at AS INTEGER)/1000, 'unixepoch', 'localtime')
         WHEN updated_at GLOB '[0-9]*' THEN datetime(CAST(updated_at AS INTEGER), 'unixepoch', 'localtime')
         ELSE COALESCE(updated_at, datetime('now')) END
FROM service_document_config;

DROP TABLE service_document_config;
ALTER TABLE service_document_config_temp RENAME TO service_document_config;
CREATE INDEX IF NOT EXISTS idx_service_document_config_service_name ON service_document_config(service_name);
CREATE INDEX IF NOT EXISTS idx_service_document_config_domain_id ON service_document_config(domain_id);
