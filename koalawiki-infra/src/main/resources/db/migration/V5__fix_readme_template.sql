-- =============================================
-- V5: 修复README模板缺失字段
-- =============================================

UPDATE `ai_prompt_template`
SET
    `is_active` = 1,
    `is_default` = 1,
    `version` = '1.0.0'
WHERE `prompt_type` = 'README'
  AND `agent_type` = 'all'
  AND (`is_active` IS NULL OR `is_default` IS NULL OR `version` IS NULL);
