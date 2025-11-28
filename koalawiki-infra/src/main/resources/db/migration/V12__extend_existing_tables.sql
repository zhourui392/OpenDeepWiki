-- =============================================
-- OpenDeepWiki 现有表扩展迁移脚本
-- 版本: V12
-- 描述: 扩展现有表结构以支持微服务集群管理
-- 支持: MySQL 5.7+, MariaDB 10.3+
-- @author zhourui(V33215020)
-- @since 2025/11/28
-- =============================================

-- =============================================
-- 1. 扩展 warehouses 表
-- =============================================

-- 添加集群关联字段
ALTER TABLE `warehouses`
    ADD COLUMN IF NOT EXISTS `cluster_id` VARCHAR(36) COMMENT '所属集群ID' AFTER `user_id`,
    ADD COLUMN IF NOT EXISTS `primary_domain_id` VARCHAR(36) COMMENT '主领域ID' AFTER `cluster_id`,
    ADD COLUMN IF NOT EXISTS `service_type` VARCHAR(20) DEFAULT 'PROVIDER' COMMENT '服务类型: PROVIDER, CONSUMER, GATEWAY, SCHEDULER, MIDDLEWARE' AFTER `primary_domain_id`;

-- 添加索引（如果不存在）
-- 注意: MySQL 5.7不支持 IF NOT EXISTS 语法用于索引，需要用存储过程或忽略错误
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'warehouses' AND INDEX_NAME = 'idx_cluster_id') = 0,
    'ALTER TABLE `warehouses` ADD INDEX `idx_cluster_id` (`cluster_id`)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'warehouses' AND INDEX_NAME = 'idx_primary_domain_id') = 0,
    'ALTER TABLE `warehouses` ADD INDEX `idx_primary_domain_id` (`primary_domain_id`)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'warehouses' AND INDEX_NAME = 'idx_service_type') = 0,
    'ALTER TABLE `warehouses` ADD INDEX `idx_service_type` (`service_type`)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================
-- 2. 扩展 ai_document 表
-- =============================================

-- 添加集群和领域关联字段
ALTER TABLE `ai_document`
    ADD COLUMN IF NOT EXISTS `cluster_id` VARCHAR(36) COMMENT '所属集群ID' AFTER `warehouse_id`,
    ADD COLUMN IF NOT EXISTS `domain_id` VARCHAR(36) COMMENT '所属领域ID' AFTER `cluster_id`,
    ADD COLUMN IF NOT EXISTS `ai_context_level` VARCHAR(20) COMMENT 'AI上下文级别: L1, L2, L3, L4' AFTER `domain_id`,
    ADD COLUMN IF NOT EXISTS `doc_type` VARCHAR(32) DEFAULT 'SYSTEM_DOC' COMMENT '文档类型' AFTER `ai_context_level`;

-- 添加索引
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_document' AND INDEX_NAME = 'idx_ai_doc_cluster') = 0,
    'ALTER TABLE `ai_document` ADD INDEX `idx_ai_doc_cluster` (`cluster_id`)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_document' AND INDEX_NAME = 'idx_ai_doc_domain') = 0,
    'ALTER TABLE `ai_document` ADD INDEX `idx_ai_doc_domain` (`domain_id`)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_document' AND INDEX_NAME = 'idx_ai_doc_type') = 0,
    'ALTER TABLE `ai_document` ADD INDEX `idx_ai_doc_type` (`doc_type`)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================
-- 3. 扩展 generation_task 表
-- =============================================

ALTER TABLE `generation_task`
    ADD COLUMN IF NOT EXISTS `doc_type` VARCHAR(32) DEFAULT 'SYSTEM_DOC' COMMENT '文档类型' AFTER `agent_type`;

-- =============================================
-- 4. 将历史仓库关联到默认集群
-- =============================================

UPDATE `warehouses`
SET `cluster_id` = 'default-cluster-uuid-0001'
WHERE `cluster_id` IS NULL;
