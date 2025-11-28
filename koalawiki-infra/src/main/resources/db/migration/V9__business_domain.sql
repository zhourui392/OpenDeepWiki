-- =============================================
-- OpenDeepWiki 业务领域迁移脚本
-- 版本: V9
-- 描述: 创建业务领域相关表结构
-- 支持: MySQL 5.7+, MariaDB 10.3+
-- @author zhourui(V33215020)
-- @since 2025/11/28
-- =============================================

-- =============================================
-- 1. 业务领域表
-- =============================================

CREATE TABLE IF NOT EXISTS `business_domain` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '领域ID（UUID）',
    `cluster_id` VARCHAR(36) NOT NULL COMMENT '所属集群ID',
    `name` VARCHAR(64) NOT NULL COMMENT '领域名称',
    `code` VARCHAR(32) NOT NULL COMMENT '领域编码',
    `description` VARCHAR(500) COMMENT '领域描述',
    `owner` VARCHAR(64) COMMENT '领域负责人',
    `color` VARCHAR(20) DEFAULT '#1890ff' COMMENT '显示颜色',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',

    UNIQUE KEY `uk_cluster_code` (`cluster_id`, `code`),
    INDEX `idx_cluster_id` (`cluster_id`),
    INDEX `idx_code` (`code`),
    INDEX `idx_sort_order` (`sort_order`),
    INDEX `idx_deleted_at` (`deleted_at`),

    CONSTRAINT `fk_domain_cluster` FOREIGN KEY (`cluster_id`)
        REFERENCES `service_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务领域表';

-- =============================================
-- 2. 服务领域映射表（多对多）
-- =============================================

CREATE TABLE IF NOT EXISTS `service_domain_mapping` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT 'ID（UUID）',
    `domain_id` VARCHAR(36) NOT NULL COMMENT '领域ID',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID（即服务）',
    `service_name` VARCHAR(128) COMMENT '服务名称',
    `service_type` VARCHAR(20) NOT NULL DEFAULT 'PROVIDER' COMMENT '服务类型: PROVIDER, CONSUMER, GATEWAY, SCHEDULER, MIDDLEWARE',
    `description` VARCHAR(500) COMMENT '服务描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    `is_primary` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主领域',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY `uk_domain_warehouse` (`domain_id`, `warehouse_id`),
    INDEX `idx_domain_id` (`domain_id`),
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_service_type` (`service_type`),
    INDEX `idx_is_primary` (`is_primary`),

    CONSTRAINT `fk_sdm_domain` FOREIGN KEY (`domain_id`)
        REFERENCES `business_domain`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sdm_warehouse` FOREIGN KEY (`warehouse_id`)
        REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务领域映射表';

-- =============================================
-- 3. 初始化默认领域
-- =============================================

INSERT INTO `business_domain` (`id`, `cluster_id`, `name`, `code`, `description`, `owner`, `color`, `sort_order`, `created_at`)
VALUES (
    'default-domain-uuid-0001',
    'default-cluster-uuid-0001',
    '未分类',
    'unclassified',
    '未分类的服务，请将服务分配到对应的业务领域',
    'system',
    '#8c8c8c',
    999,
    NOW()
) ON DUPLICATE KEY UPDATE `name` = `name`;
