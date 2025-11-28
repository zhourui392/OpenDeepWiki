-- =============================================
-- OpenDeepWiki 服务依赖图缓存迁移脚本
-- 版本: V11
-- 描述: 创建服务依赖图缓存表结构
-- 支持: MySQL 5.7+, MariaDB 10.3+
-- @author zhourui(V33215020)
-- @since 2025/11/28
-- =============================================

-- =============================================
-- 1. 服务依赖图缓存表
-- =============================================

CREATE TABLE IF NOT EXISTS `service_dependency_graph` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '图ID（UUID）',
    `cluster_id` VARCHAR(36) NOT NULL COMMENT '所属集群ID',
    `graph_type` VARCHAR(32) NOT NULL COMMENT '图类型: FULL, DOMAIN, SERVICE',
    `scope_id` VARCHAR(36) COMMENT '作用域ID（领域ID或服务ID，NULL表示全集群）',
    `graph_data` LONGTEXT NOT NULL COMMENT '图数据（JSON格式）',
    `node_count` INT NOT NULL DEFAULT 0 COMMENT '节点数',
    `edge_count` INT NOT NULL DEFAULT 0 COMMENT '边数',
    `mermaid_code` LONGTEXT COMMENT 'Mermaid图代码',
    `generated_at` DATETIME NOT NULL COMMENT '生成时间',
    `expires_at` DATETIME COMMENT '过期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY `uk_cluster_type_scope` (`cluster_id`, `graph_type`, `scope_id`),
    INDEX `idx_cluster_id` (`cluster_id`),
    INDEX `idx_graph_type` (`graph_type`),
    INDEX `idx_expires_at` (`expires_at`),
    INDEX `idx_generated_at` (`generated_at`),

    CONSTRAINT `fk_sdg_cluster` FOREIGN KEY (`cluster_id`)
        REFERENCES `service_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务依赖图缓存表';
