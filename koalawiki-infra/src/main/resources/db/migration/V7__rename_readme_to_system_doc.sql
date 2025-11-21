-- =============================================
-- V7: 将 README 重命名为 SYSTEM_DOC (系统说明文档)
-- =============================================

-- 1. 更新 ai_prompt_template 表中的 prompt_type
UPDATE `ai_prompt_template`
SET
    `prompt_type` = 'SYSTEM_DOC',
    `template_name` = '系统说明文档生成模板',
    `description` = '用于生成项目系统说明文档的提示词模板,包含系统分析师角色定位',
    `updated_at` = NOW()
WHERE `prompt_type` = 'README'
  AND `agent_type` = 'claude';

-- 2. 更新 ai_document 表中的 doc_type
UPDATE `ai_document`
SET
    `doc_type` = 'SYSTEM_DOC',
    `title` = '系统说明文档',
    `updated_at` = NOW()
WHERE `doc_type` = 'README';

-- 3. 更新 ai_document 表中的 source_file (将旧的路径转换为新格式)
--    旧格式: projectPath 或 "/"
--    新格式: projectPath + "/SYSTEM_DOC"
UPDATE `ai_document`
SET
    `source_file` = CONCAT(`source_file`, '/SYSTEM_DOC'),
    `updated_at` = NOW()
WHERE `doc_type` = 'SYSTEM_DOC'
  AND `source_file` NOT LIKE '%/SYSTEM_DOC';

-- =============================================
-- 迁移完成
-- =============================================
