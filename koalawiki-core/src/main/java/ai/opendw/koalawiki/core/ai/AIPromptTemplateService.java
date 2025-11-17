package ai.opendw.koalawiki.core.ai;

import ai.opendw.koalawiki.infra.entity.AIPromptTemplateEntity;
import ai.opendw.koalawiki.infra.repository.AIPromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI提示词模板服务（数据库版）
 *
 * <p>职责: 从数据库加载和管理AI提示词模板</p>
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIPromptTemplateService {

    private final AIPromptTemplateRepository templateRepository;

    /**
     * 加载提示词模板内容
     *
     * @param promptType 提示词类型
     * @param agentType Agent类型
     * @return 模板内容
     */
    @Cacheable(value = "promptTemplates", key = "#promptType + '_' + #agentType")
    public String loadTemplate(String promptType, String agentType) {
        log.debug("加载提示词模板: promptType={}, agentType={}", promptType, agentType);

        // 1. 尝试查找精确匹配的默认模板
        AIPromptTemplateEntity template = templateRepository
                .findByPromptTypeAndAgentTypeAndIsActiveAndIsDefault(promptType, agentType, true, true)
                .orElse(null);

        // 2. 如果没找到,尝试查找通用Agent(all)的模板
        if (template == null) {
            template = templateRepository
                    .findByPromptTypeAndAgentTypeAndIsActiveAndIsDefault(promptType, "all", true, true)
                    .orElse(null);
        }

        if (template == null) {
            log.warn("未找到提示词模板: promptType={}, agentType={}", promptType, agentType);
            throw new IllegalStateException(
                String.format("未找到提示词模板: promptType=%s, agentType=%s", promptType, agentType)
            );
        }

        log.info("成功加载提示词模板: {}, 版本: {}", template.getTemplateName(), template.getVersion());
        return template.getTemplateContent();
    }

    /**
     * 渲染模板（替换占位符）
     *
     * @param template 模板内容
     * @param variables 变量Map
     * @return 渲染后的内容
     */
    public String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }

        return result;
    }

    /**
     * 加载并渲染模板
     *
     * @param promptType 提示词类型
     * @param agentType Agent类型
     * @param variables 变量Map
     * @return 渲染后的内容
     */
    public String loadAndRender(String promptType, String agentType, Map<String, String> variables) {
        String template = loadTemplate(promptType, agentType);
        return renderTemplate(template, variables);
    }
}
