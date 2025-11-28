-- =============================================
-- OpenDeepWiki 服务集群迁移脚本
-- 版本: V8
-- 描述: 创建服务集群相关表结构
-- 支持: MySQL 5.7+, MariaDB 10.3+
-- @author zhourui(V33215020)
-- @since 2025/11/28
-- =============================================

-- =============================================
-- 1. 服务集群表
-- =============================================

CREATE TABLE IF NOT EXISTS `service_cluster` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '集群ID（UUID）',
    `name` VARCHAR(128) NOT NULL COMMENT '集群名称',
    `code` VARCHAR(64) NOT NULL COMMENT '集群编码（唯一标识）',
    `description` TEXT COMMENT '集群描述',
    `tech_stack` VARCHAR(255) COMMENT '技术栈描述',
    `owner` VARCHAR(64) COMMENT '负责人',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INACTIVE, ARCHIVED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',

    UNIQUE KEY `uk_code` (`code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_owner` (`owner`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务集群表';

-- =============================================
-- 2. 集群仓库关联表
-- =============================================

CREATE TABLE IF NOT EXISTS `cluster_warehouse` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT 'ID（UUID）',
    `cluster_id` VARCHAR(36) NOT NULL COMMENT '集群ID',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY `uk_cluster_warehouse` (`cluster_id`, `warehouse_id`),
    INDEX `idx_cluster_id` (`cluster_id`),
    INDEX `idx_warehouse_id` (`warehouse_id`),

    CONSTRAINT `fk_cw_cluster` FOREIGN KEY (`cluster_id`)
        REFERENCES `service_cluster`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cw_warehouse` FOREIGN KEY (`warehouse_id`)
        REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='集群仓库关联表';

-- =============================================
-- 3. 初始化默认集群
-- =============================================

INSERT INTO `service_cluster` (`id`, `name`, `code`, `description`, `tech_stack`, `owner`, `status`, `created_at`)
VALUES (
    'default-cluster-uuid-0001',
    '默认集群',
    'default',
    '系统默认集群，用于管理未分类的仓库',
    'Spring Boot + MySQL',
    'system',
    'ACTIVE',
    NOW()
) ON DUPLICATE KEY UPDATE `name` = `name`;
