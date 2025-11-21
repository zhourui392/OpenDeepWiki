-- =============================================
-- OpenDeepWiki 数据库迁移脚本
-- 版本: V2
-- 描述: 创建AI提示词模板配置表
-- 支持: MySQL 5.7+, MariaDB 10.3+
-- =============================================

-- AI提示词模板表
CREATE TABLE IF NOT EXISTS `ai_prompt_template` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '模板ID',

    -- 模板标识
    `prompt_type` VARCHAR(50) NOT NULL COMMENT '提示词类型: project_analysis, class_chinese, class_english',
    `agent_type` VARCHAR(20) NOT NULL COMMENT '适用的Agent类型: claude, all',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',

    -- 模板内容
    `template_content` LONGTEXT NOT NULL COMMENT '模板内容（支持占位符）',
    `description` VARCHAR(500) COMMENT '模板描述',

    -- 状态和版本
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `version` VARCHAR(20) NOT NULL DEFAULT '1.0.0' COMMENT '模板版本号',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为默认模板',

    -- 审计字段
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    UNIQUE KEY `uk_prompt_agent_type` (`prompt_type`, `agent_type`, `version`),
    INDEX `idx_prompt_type` (`prompt_type`),
    INDEX `idx_agent_type` (`agent_type`),
    INDEX `idx_is_active` (`is_active`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI提示词模板配置表';

-- 初始化项目架构分析提示词模板
INSERT INTO `ai_prompt_template` (`id`, `prompt_type`, `agent_type`, `template_name`, `template_content`, `description`, `is_active`, `version`, `is_default`, `created_at`) VALUES
(
    'project-analysis-claude-v1',
    'project_analysis',
    'claude',
    '项目架构分析提示词(Claude)',
    '## 输出要求

**重要**: 请直接输出完整的Markdown格式项目架构文档，不要输出总结或说明，直接从文档标题开始。

文档必须包含以下章节：

# [项目名称] 架构文档

## 1. 项目概述
- 项目的整体功能和定位
- 技术栈分析（基于识别的框架和组件）
- 架构风格（单体/微服务等）

## 2. 模块结构
- 项目的模块划分
- 各模块的职责和功能

## 3. 服务功能清单
- 按功能域对HTTP接口进行分组
- 列出每个功能域的主要接口和功能

## 4. 服务入口汇总
- HTTP接口的功能分类
- Dubbo服务的对外能力
- 定时任务的调度说明
- MQ消费的业务场景

## 5. 核心业务链路
- 识别3-5个核心业务流程
- 描述每个流程的调用链路
- 标注关键的业务节点

## 6. 架构特点和建议
- 当前架构的优点
- 潜在的改进点
- 技术债务提示

**格式要求**：
- 直接输出Markdown文档内容，不要有任何前言或总结
- 使用表格和列表增强可读性
- 使用中文编写
- 重点突出业务价值和架构设计',
    '用于项目架构分析的Claude提示词模板',
    1,
    '1.0.0',
    1,
    NOW()
),
(
    'class-chinese-claude-v1',
    'class_chinese',
    'claude',
    '类文档生成提示词(Claude-中文)',
    '请为以下{language}类生成详细的技术文档。

类名: {className}
包名: {packageName}

源代码:
```{language}
{code}
```

请按以下格式生成Markdown文档:

# {className}

## 概述
(用2-3句话描述这个类的作用和职责)

## 核心功能
(列出主要功能点,每个功能点一段简短描述)

## 主要方法
(为每个public方法生成说明,包括:
- 方法签名
- 功能描述
- 参数说明
- 返回值说明
- 简单的使用示例)

## 使用示例
(提供1-2个实际使用示例代码)

## 注意事项
(如有特殊注意事项,列出来)

要求:
1. 使用清晰简洁的中文
2. 代码示例使用{language}语法高亮
3. 重点突出类的设计意图和使用场景
4. 避免过度技术化的术语',
    '用于生成中文类文档的Claude提示词模板',
    1,
    '1.0.0',
    1,
    NOW()
);

-- =============================================
-- 数据库迁移完成
-- =============================================
