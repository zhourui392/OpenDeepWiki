-- =============================================
-- OpenDeepWiki Dubbo接口注册表迁移脚本
-- 版本: V10
-- 描述: 创建Dubbo接口注册相关表结构
-- 支持: MySQL 5.7+, MariaDB 10.3+
-- @author zhourui(V33215020)
-- @since 2025/11/28
-- =============================================

-- =============================================
-- 1. Dubbo接口注册表
-- =============================================

CREATE TABLE IF NOT EXISTS `dubbo_interface_registry` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '注册ID（UUID）',
    `cluster_id` VARCHAR(36) NOT NULL COMMENT '所属集群ID',
    `interface_name` VARCHAR(255) NOT NULL COMMENT '接口全限定名',
    `version` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '接口版本',
    `group_name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '接口分组',
    `provider_warehouse_id` VARCHAR(36) COMMENT '提供者仓库ID',
    `provider_service_name` VARCHAR(128) COMMENT '提供者服务名',
    `description` TEXT COMMENT '接口描述（从Javadoc提取）',
    `methods` JSON COMMENT '方法列表（JSON格式）',
    `deprecated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否废弃',
    `deprecated_reason` VARCHAR(500) COMMENT '废弃原因',
    `source_file` VARCHAR(500) COMMENT '源文件路径',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY `uk_interface` (`cluster_id`, `interface_name`, `version`, `group_name`),
    INDEX `idx_cluster_id` (`cluster_id`),
    INDEX `idx_provider_warehouse` (`provider_warehouse_id`),
    INDEX `idx_interface_name` (`interface_name`),
    INDEX `idx_deprecated` (`deprecated`),
    INDEX `idx_created_at` (`created_at`),

    CONSTRAINT `fk_dir_cluster` FOREIGN KEY (`cluster_id`)
        REFERENCES `service_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dubbo接口注册表';

-- =============================================
-- 2. Dubbo接口消费者关联表
-- =============================================

CREATE TABLE IF NOT EXISTS `dubbo_interface_consumer` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT 'ID（UUID）',
    `interface_registry_id` VARCHAR(36) NOT NULL COMMENT '接口注册ID',
    `consumer_warehouse_id` VARCHAR(36) NOT NULL COMMENT '消费者仓库ID',
    `consumer_service_name` VARCHAR(128) COMMENT '消费者服务名',
    `source_class` VARCHAR(255) COMMENT '使用该接口的类',
    `source_field` VARCHAR(64) COMMENT '注入的字段名',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY `uk_consumer` (`interface_registry_id`, `consumer_warehouse_id`, `source_class`),
    INDEX `idx_interface` (`interface_registry_id`),
    INDEX `idx_consumer` (`consumer_warehouse_id`),

    CONSTRAINT `fk_dic_registry` FOREIGN KEY (`interface_registry_id`)
        REFERENCES `dubbo_interface_registry`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dic_consumer` FOREIGN KEY (`consumer_warehouse_id`)
        REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dubbo接口消费者表';
