-- =============================================
-- V3: 服务文档库 schema 扩展
-- 1) ai_document/generation_task 新增 service/docType 等字段
-- 2) 创建 service_document_config 表
-- =============================================

-- -------- ai_document 扩展 --------
ALTER TABLE `ai_document`
    ADD COLUMN `service_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '服务ID',
    ADD COLUMN `service_name` VARCHAR(128) NOT NULL DEFAULT 'Default Service' COMMENT '服务名称',
    ADD COLUMN `doc_type` VARCHAR(32) NOT NULL DEFAULT 'ARCHITECTURE' COMMENT '文档类型',
    ADD COLUMN `prompt_template_id` VARCHAR(36) NULL COMMENT '提示词模板ID',
    ADD COLUMN `metadata` LONGTEXT NULL COMMENT '扩展字段(JSON)';

ALTER TABLE `ai_document`
    ADD INDEX `idx_service_id` (`service_id`),
    ADD INDEX `idx_doc_type` (`doc_type`);

-- -------- generation_task 扩展 --------
ALTER TABLE `generation_task`
    ADD COLUMN `service_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '服务ID',
    ADD COLUMN `doc_type` VARCHAR(32) NOT NULL DEFAULT 'ARCHITECTURE' COMMENT '文档类型';

ALTER TABLE `generation_task`
    ADD INDEX `idx_service_id` (`service_id`);

-- -------- service_document_config 表 --------
CREATE TABLE IF NOT EXISTS `service_document_config` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '配置ID（UUID）',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID',
    `service_id` VARCHAR(64) NOT NULL COMMENT '服务标识（英文/拼音）',
    `service_name` VARCHAR(128) NOT NULL COMMENT '服务名称',
    `description` VARCHAR(255) COMMENT '服务描述',
    `doc_type` VARCHAR(32) NOT NULL DEFAULT 'ARCHITECTURE' COMMENT '默认文档类型',
    `prompt_template_id` VARCHAR(36) COMMENT '默认提示词模板ID',
    `agent_type` VARCHAR(32) COMMENT '默认Agent类型',
    `source_globs` JSON NULL COMMENT '源码匹配规则(JSON数组)',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_warehouse_service` (`warehouse_id`, `service_id`),
    INDEX `idx_service_name` (`service_name`),
    INDEX `idx_doc_type` (`doc_type`),
    CONSTRAINT `fk_service_doc_cfg_warehouse`
        FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务文档库配置';
