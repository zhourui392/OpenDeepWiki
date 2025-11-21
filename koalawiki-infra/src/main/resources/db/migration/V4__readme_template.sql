-- =============================================
-- V4: README文档生成支持
-- 1) 插入README提示词模板
-- =============================================

-- 插入README生成模板
INSERT INTO `ai_prompt_template` (
    `id`,
    `template_name`,
    `description`,
    `prompt_type`,
    `agent_type`,
    `template_content`,
    `created_at`,
    `updated_at`
) VALUES (
    UUID(),
    'README生成模板',
    '用于生成项目README.md文档的提示词模板',
    'README',
    'all',
    '# 生成README.md任务

请根据以下项目信息，生成完整的README.md文档。

## 项目信息
{{projectInfo}}

## Maven模块结构
{{modules}}

## 包结构
{{packages}}

## 功能列表
{{features}}

## 启动配置
{{startupGuide}}

## 测试信息
{{testGuide}}

## 数据模型
{{dataModels}}

---

请生成一个专业的README.md，包含以下章节：
1. 项目简介
2. 技术栈
3. 模块说明
4. 功能特性
5. 快速开始（包含环境要求、安装步骤、启动命令）
6. 测试（包含测试命令和覆盖率）
7. 数据模型（使用表格展示）
8. API文档（如有接口则列出）

要求：
- 使用清晰的Markdown格式
- 代码块使用正确的语言标识
- 使用表格展示结构化数据
- 添加必要的emoji图标增强可读性
- 保持专业和简洁',
    NOW(),
    NOW()
);
