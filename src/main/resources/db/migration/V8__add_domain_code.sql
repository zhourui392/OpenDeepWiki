-- 领域表添加code字段
ALTER TABLE domain_info ADD COLUMN code VARCHAR(64);

-- 服务表添加last_commit_id字段（用于检测更新）
ALTER TABLE service_document_config ADD COLUMN last_commit_id VARCHAR(64);
