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
    `description` VARCHAR(200) COMMENT '角色描述',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `updated_at` DATETIME COMMENT '更新时间',
    `is_system_role` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统角色（不可删除）',
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
    `ai_doc_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用AI文档生成',
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
    `file_count` INT COMMENT '总文件数',
    `updated_file_count` INT COMMENT '更新的文件数',
    `added_file_count` INT COMMENT '新增的文件数',
    `deleted_file_count` INT COMMENT '删除的文件数',
    `error_message` TEXT COMMENT '错误信息',
    `error_stack` TEXT COMMENT '错误堆栈',
    `progress` INT DEFAULT 0 COMMENT '同步进度（百分比）',
    `details` TEXT COMMENT '同步详情（JSON格式）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    `deleted_at` DATETIME COMMENT '删除时间（软删除）',
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_trigger` (`trigger`),
    INDEX `idx_start_time` (`start_time`),
    INDEX `idx_created_at` (`created_at`),
    FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库同步记录表';

-- =============================================
-- 3. AI文档生成系统表
-- =============================================

-- AI文档表
CREATE TABLE IF NOT EXISTS `ai_document` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '文档ID',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID',
    `source_file` VARCHAR(500) NOT NULL COMMENT '源代码文件路径',

    -- 文档内容
    `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
    `content` LONGTEXT NOT NULL COMMENT 'Markdown格式文档内容',

    -- 状态
    `status` VARCHAR(20) NOT NULL DEFAULT 'GENERATING' COMMENT '状态: GENERATING, COMPLETED, FAILED',
    `agent_type` VARCHAR(20) COMMENT '使用的Agent: claude',
    `error_message` TEXT COMMENT '错误信息',

    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    UNIQUE KEY `uk_warehouse_file` (`warehouse_id`, `source_file`),
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`),

    -- 外键
    CONSTRAINT `fk_ai_document_warehouse`
        FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成的文档';

-- 文档生成任务表
CREATE TABLE IF NOT EXISTS `generation_task` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '任务ID',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓库ID',

    -- 任务状态
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING, RUNNING, COMPLETED, FAILED',

    -- 进度统计
    `total_files` INT DEFAULT 0 COMMENT '总文件数',
    `completed_files` INT DEFAULT 0 COMMENT '已完成文件数',
    `failed_files` INT DEFAULT 0 COMMENT '失败文件数',

    -- Agent配置
    `agent_type` VARCHAR(20) COMMENT '使用的Agent: claude',

    -- 时间
    `started_at` DATETIME NULL COMMENT '开始时间',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`),

    -- 外键
    CONSTRAINT `fk_generation_task_warehouse`
        FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档生成任务';

-- =============================================
-- 4. 访问日志和统计表
-- =============================================

-- 访问日志表
CREATE TABLE IF NOT EXISTS `access_log` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '日志ID（UUID）',
    `user_id` VARCHAR(50) COMMENT '用户ID',
    `warehouse_id` VARCHAR(50) COMMENT '仓库ID',
    `document_id` VARCHAR(50) COMMENT '文档ID',
    `action` VARCHAR(20) NOT NULL COMMENT '访问动作',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT 'User Agent',
    `request_uri` VARCHAR(500) COMMENT '请求URI',
    `request_method` VARCHAR(10) COMMENT '请求方法',
    `response_time` INT COMMENT '响应时间（毫秒）',
    `status_code` INT COMMENT 'HTTP状态码',
    `access_time` DATETIME NOT NULL COMMENT '访问时间',
    `referer` VARCHAR(500) COMMENT 'Referer',
    `session_id` VARCHAR(100) COMMENT '会话ID',
    `request_params` TEXT COMMENT '请求参数（JSON格式）',
    `error_message` VARCHAR(1000) COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_warehouse_id` (`warehouse_id`),
    INDEX `idx_document_id` (`document_id`),
    INDEX `idx_access_time` (`access_time`),
    INDEX `idx_ip_address` (`ip_address`),
    INDEX `idx_action` (`action`),
    INDEX `idx_status_code` (`status_code`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志表';

-- 每日统计表
CREATE TABLE IF NOT EXISTS `daily_statistics` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '统计ID（UUID）',
    `statistics_date` DATE NOT NULL COMMENT '统计日期',
    `warehouse_id` VARCHAR(50) COMMENT '仓库ID（NULL表示全局统计）',
    `view_count` BIGINT COMMENT '页面浏览量（PV）',
    `unique_user_count` BIGINT COMMENT '独立访客数（UV）',
    `unique_ip_count` BIGINT COMMENT '独立IP数',
    `document_view_count` BIGINT COMMENT '文档访问次数',
    `search_count` BIGINT COMMENT '搜索次数',
    `download_count` BIGINT COMMENT '下载次数',
    `avg_response_time` DOUBLE COMMENT '平均响应时间（毫秒）',
    `max_response_time` INT COMMENT '最大响应时间（毫秒）',
    `min_response_time` INT COMMENT '最小响应时间（毫秒）',
    `total_requests` BIGINT COMMENT '总请求数',
    `success_requests` BIGINT COMMENT '成功请求数（状态码2xx）',
    `failed_requests` BIGINT COMMENT '失败请求数（状态码4xx, 5xx）',
    `error_rate` DOUBLE COMMENT '错误率（%）',
    `action_counts` TEXT COMMENT '各动作的统计次数（JSON格式）',
    `top_documents` TEXT COMMENT '热门文档Top10（JSON格式）',
    `top_search_keywords` TEXT COMMENT '热门搜索关键词Top10（JSON格式）',
    `new_user_count` BIGINT COMMENT '新增用户数',
    `active_user_count` BIGINT COMMENT '活跃用户数',
    `remarks` VARCHAR(500) COMMENT '备注',
    `calculated_at` DATETIME COMMENT '计算完成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    UNIQUE KEY `uk_warehouse_date` (`warehouse_id`, `statistics_date`),
    INDEX `idx_statistics_date` (`statistics_date`),
    INDEX `idx_warehouse_id` (`warehouse_id`),
    FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日统计表';

-- =============================================
-- 5. 初始化数据
-- =============================================

INSERT INTO `roles` (`id`, `name`, `description`, `is_active`, `is_system_role`, `created_at`) VALUES
('admin-role-uuid', 'Admin', '系统管理员,拥有所有权限', 1, 1, NOW()),
('user-role-uuid', 'User', '普通用户,拥有基本权限', 1, 1, NOW()),
('guest-role-uuid', 'Guest', '访客,拥有只读权限', 1, 1, NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 插入默认管理员用户（邮箱: admin@koalawiki.com，密码: 123456，按当前登录逻辑的格式存储）
INSERT INTO `users` (`id`, `name`, `email`, `password`, `bio`, `created_at`) VALUES
('default-admin-uuid-0001', 'Administrator', 'admin@koalawiki.com', '123456', '系统默认管理员账号', NOW())
ON DUPLICATE KEY UPDATE email=email;

-- 为默认管理员分配 Admin 角色
INSERT INTO `user_in_roles` (`id`, `user_id`, `role_id`, `created_at`) VALUES
('admin-role-mapping-0001', 'default-admin-uuid-0001', 'admin-role-uuid', NOW())
ON DUPLICATE KEY UPDATE user_id=user_id;

-- =============================================
-- 数据库初始化完成
-- =============================================
