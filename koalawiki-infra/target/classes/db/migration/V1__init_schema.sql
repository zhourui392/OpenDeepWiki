-- =============================================
-- OpenDeepWiki 数据库初始化脚本
-- 版本: V1
-- 描述: 创建所有基础表结构
-- 支持: MySQL 5.7+, MariaDB 10.3+
-- =============================================

-- =============================================
-- 1. 用户和权限表
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '用户ID（UUID）',
    `name` VARCHAR(50) NOT NULL COMMENT '用户名称',
    `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '用户邮箱',
    `password` VARCHAR(255) NOT NULL COMMENT '用户密码（加密后）',
    `avatar` VARCHAR(500) COMMENT '用户头像URL',
    `bio` VARCHAR(500) COMMENT '用户简介',
    `location` VARCHAR(100) COMMENT '用户位置',
    `website` VARCHAR(200) COMMENT '用户个人网站',
    `company` VARCHAR(100) COMMENT '用户公司',
    `last_login_at` DATETIME COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(45) COMMENT '最后登录IP',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',
    INDEX `idx_email` (`email`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `roles` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '角色ID（UUID）',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    `description` VARCHAR(500) COMMENT '角色描述',
    `is_system` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统角色（不可删除）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',
    INDEX `idx_name` (`name`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_in_roles` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT 'ID（UUID）',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `role_id` VARCHAR(36) NOT NULL COMMENT '角色ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_role_id` (`role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- =============================================
-- 2. 仓库表
-- =============================================

-- 仓库表
CREATE TABLE IF NOT EXISTS `warehouses` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '仓库ID（UUID）',
    `organization_name` VARCHAR(100) COMMENT '组织名称',
    `name` VARCHAR(200) NOT NULL COMMENT '仓库名称',
    `description` VARCHAR(1000) COMMENT '仓库描述',
    `address` VARCHAR(500) NOT NULL COMMENT '仓库地址（Git URL）',
    `git_user_name` VARCHAR(100) COMMENT '私有化Git账号',
    `git_password` VARCHAR(200) COMMENT '私有化Git密码',
    `email` VARCHAR(100) COMMENT '私有化Git邮箱',
    `type` VARCHAR(50) COMMENT '仓库类型',
    `branch` VARCHAR(100) DEFAULT 'main' COMMENT '仓库分支',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '仓库状态',
    `error` VARCHAR(2000) COMMENT '错误信息',
    `version` VARCHAR(50) COMMENT '仓库版本',
    `is_embedded` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否嵌入完成',
    `is_recommended` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐',
    `classify` VARCHAR(50) COMMENT '仓库类别',
    `stars` INT NOT NULL DEFAULT 0 COMMENT 'Star数量',
    `forks` INT NOT NULL DEFAULT 0 COMMENT 'Fork数量',
    `user_id` VARCHAR(36) COMMENT '创建用户ID',
    `enable_sync` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用同步',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',
    INDEX `idx_name` (`name`),
    INDEX `idx_organization` (`organization_name`),
    INDEX `idx_status` (`status`),
    INDEX `idx_classify` (`classify`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_is_recommended` (`is_recommended`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库表';

-- 仓库同步记录表
CREATE TABLE IF NOT EXISTS `warehouse_sync_records` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '同步记录ID（UUID）',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID',
    `trigger` VARCHAR(20) NOT NULL COMMENT '触发方式',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '同步状态',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `duration_ms` BIGINT COMMENT '持续时间（毫秒）',
    `from_version` VARCHAR(50) COMMENT '起始版本',
    `to_version` VARCHAR(50) COMMENT '目标版本',
    `files_changed` INT COMMENT '变更文件数',
    `files_added` INT COMMENT '新增文件数',
    `files_deleted` INT COMMENT '删除文件数',
    `error_message` TEXT COMMENT '错误信息',
    `error_stack` TEXT COMMENT '错误堆栈',
    `progress` INT DEFAULT 0 COMMENT '同步进度（百分比）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_trigger` (`trigger`),
    INDEX `idx_start_time` (`start_time`),
    INDEX `idx_created_at` (`created_at`),
    FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库同步记录表';

-- =============================================
-- 3. 文档表
-- =============================================

-- 文档表
CREATE TABLE IF NOT EXISTS `documents` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '文档ID（UUID）',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '关联仓库ID',
    `last_update` DATETIME COMMENT '最后更新时间',
    `description` VARCHAR(2000) COMMENT '文档介绍',
    `like_count` BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` BIGINT NOT NULL DEFAULT 0 COMMENT '评论数',
    `git_path` VARCHAR(500) COMMENT '本地Git仓库地址',
    `status` VARCHAR(20) COMMENT '仓库状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`),
    FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 文档目录表
CREATE TABLE IF NOT EXISTS `document_catalogs` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '目录ID（UUID）',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID',
    `parent_id` VARCHAR(36) COMMENT '父目录ID',
    `title` VARCHAR(500) NOT NULL COMMENT '目录标题',
    `path` VARCHAR(1000) NOT NULL COMMENT '文件路径',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `depth` INT NOT NULL DEFAULT 0 COMMENT '目录深度',
    `is_directory` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为目录',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `file_extension` VARCHAR(20) COMMENT '文件扩展名',
    `content_preview` TEXT COMMENT '内容预览',
    `metadata` TEXT COMMENT '元数据（JSON）',
    `version` VARCHAR(50) COMMENT '版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_path` (`path`(255)),
    INDEX `idx_order` (`order`),
    INDEX `idx_created_at` (`created_at`),
    FULLTEXT INDEX `ft_title` (`title`),
    FULLTEXT INDEX `ft_content_preview` (`content_preview`),
    FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档目录表';

-- 文档目录国际化表
CREATE TABLE IF NOT EXISTS `document_catalog_i18n` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT 'ID（UUID）',
    `catalog_id` VARCHAR(36) NOT NULL COMMENT '目录ID',
    `language` VARCHAR(10) NOT NULL COMMENT '语言代码',
    `title` VARCHAR(500) NOT NULL COMMENT '翻译后的标题',
    `content_preview` TEXT COMMENT '翻译后的预览',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    UNIQUE KEY `uk_catalog_language` (`catalog_id`, `language`),
    INDEX `idx_catalog_id` (`catalog_id`),
    INDEX `idx_language` (`language`),
    FOREIGN KEY (`catalog_id`) REFERENCES `document_catalogs`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档目录国际化表';

-- 文档文件项表
CREATE TABLE IF NOT EXISTS `document_file_items` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '文件项ID（UUID）',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID',
    `document_id` VARCHAR(36) COMMENT '文档ID',
    `file_name` VARCHAR(500) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(1000) NOT NULL COMMENT '文件路径',
    `file_type` VARCHAR(50) COMMENT '文件类型',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `content` LONGTEXT COMMENT '文件内容',
    `source` VARCHAR(20) COMMENT '文件来源',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_document_id` (`document_id`),
    INDEX `idx_file_path` (`file_path`(255)),
    INDEX `idx_created_at` (`created_at`),
    FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文件项表';

-- 文档文件项国际化表
CREATE TABLE IF NOT EXISTS `document_file_item_i18n` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT 'ID（UUID）',
    `file_item_id` VARCHAR(36) NOT NULL COMMENT '文件项ID',
    `language` VARCHAR(10) NOT NULL COMMENT '语言代码',
    `content` LONGTEXT COMMENT '翻译后的内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    UNIQUE KEY `uk_file_item_language` (`file_item_id`, `language`),
    INDEX `idx_file_item_id` (`file_item_id`),
    INDEX `idx_language` (`language`),
    FOREIGN KEY (`file_item_id`) REFERENCES `document_file_items`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文件项国际化表';

-- 文档提交记录表
CREATE TABLE IF NOT EXISTS `document_commit_records` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '提交记录ID（UUID）',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID',
    `document_id` VARCHAR(36) COMMENT '文档ID',
    `commit_id` VARCHAR(255) NOT NULL COMMENT 'Git提交ID',
    `commit_message` TEXT COMMENT '提交消息',
    `author` VARCHAR(100) COMMENT '作者',
    `author_email` VARCHAR(100) COMMENT '作者邮箱',
    `commit_time` DATETIME COMMENT '提交时间',
    `files_changed` INT COMMENT '变更文件数',
    `additions` INT COMMENT '新增行数',
    `deletions` INT COMMENT '删除行数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_document_id` (`document_id`),
    INDEX `idx_commit_id` (`commit_id`(100)),
    INDEX `idx_commit_time` (`commit_time`),
    INDEX `idx_author` (`author`),
    FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档提交记录表';

-- =============================================
-- 4. 访问日志和统计表
-- =============================================

-- 访问日志表
CREATE TABLE IF NOT EXISTS `access_logs` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '日志ID（UUID）',
    `user_id` VARCHAR(36) COMMENT '用户ID',
    `warehouse_id` VARCHAR(36) COMMENT '仓库ID',
    `document_id` VARCHAR(36) COMMENT '文档ID',
    `uri` VARCHAR(500) COMMENT '请求URI',
    `method` VARCHAR(10) COMMENT '请求方法',
    `params` TEXT COMMENT '请求参数',
    `status_code` INT COMMENT 'HTTP状态码',
    `response_time` INT COMMENT '响应时间（毫秒）',
    `error_message` TEXT COMMENT '错误信息',
    `ip` VARCHAR(45) COMMENT '客户端IP',
    `user_agent` VARCHAR(500) COMMENT 'User Agent',
    `session_id` VARCHAR(100) COMMENT '会话ID',
    `action_type` VARCHAR(20) COMMENT '动作类型',
    `request_time` DATETIME NOT NULL COMMENT '请求时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_document_id` (`document_id`),
    INDEX `idx_request_time` (`request_time`),
    INDEX `idx_ip` (`ip`),
    INDEX `idx_action_type` (`action_type`),
    INDEX `idx_status_code` (`status_code`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志表';

-- 每日统计表
CREATE TABLE IF NOT EXISTS `daily_statistics` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '统计ID（UUID）',
    `warehouse_id` VARCHAR(36) COMMENT '仓库ID（NULL表示全局统计）',
    `date` DATE NOT NULL COMMENT '统计日期',
    `page_views` BIGINT NOT NULL DEFAULT 0 COMMENT '页面浏览量（PV）',
    `unique_visitors` BIGINT NOT NULL DEFAULT 0 COMMENT '独立访客数（UV）',
    `unique_ips` BIGINT NOT NULL DEFAULT 0 COMMENT '独立IP数',
    `total_requests` BIGINT NOT NULL DEFAULT 0 COMMENT '总请求数',
    `success_requests` BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    `failed_requests` BIGINT NOT NULL DEFAULT 0 COMMENT '失败请求数',
    `error_rate` DECIMAL(5,2) COMMENT '错误率（百分比）',
    `avg_response_time` INT COMMENT '平均响应时间（毫秒）',
    `max_response_time` INT COMMENT '最大响应时间（毫秒）',
    `min_response_time` INT COMMENT '最小响应时间（毫秒）',
    `status_code_stats` TEXT COMMENT '状态码统计（JSON）',
    `action_type_stats` TEXT COMMENT '动作类型统计（JSON）',
    `top_documents` TEXT COMMENT '热门文档Top10（JSON）',
    `top_keywords` TEXT COMMENT '热门关键词（JSON）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    UNIQUE KEY `uk_warehouse_date` (`warehouse_id`, `date`),
    INDEX `idx_date` (`date`),
    INDEX `idx_warehouse_id` (`warehouse_id`),
    FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计表';

-- =============================================
-- 5. 初始化数据
-- =============================================

-- 插入默认角色
INSERT INTO `roles` (`id`, `name`, `description`, `is_system`, `created_at`) VALUES
('admin-role-uuid', 'Admin', '系统管理员，拥有所有权限', 1, NOW()),
('user-role-uuid', 'User', '普通用户，拥有基本权限', 1, NOW()),
('guest-role-uuid', 'Guest', '访客，拥有只读权限', 1, NOW())
ON DUPLICATE KEY UPDATE name=name;

-- =============================================
-- 数据库初始化完成
-- =============================================
