package ai.opendw.koalawiki.core.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * AI提示词模板加载器(资源文件版)
 *
 * <p>职责: 从resources目录加载markdown格式的AI提示词模板</p>
 *
 * @author zhourui(V33215020)
 * @since 2025/11/20
 */
@Slf4j
@Service
public class ResourceBasedAIPromptTemplateLoader {

    private static final String TEMPLATE_BASE_PATH = "prompts";

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

        String templatePath = buildTemplatePath(promptType, agentType);
        String content = readTemplateFromResource(templatePath);

        if (content == null) {
            String fallbackPath = buildTemplatePath(promptType, "all");
            content = readTemplateFromResource(fallbackPath);
        }

        if (content == null) {
            log.warn("未找到提示词模板: promptType={}, agentType={}", promptType, agentType);
            throw new IllegalStateException(
                String.format("未找到提示词模板: promptType=%s, agentType=%s", promptType, agentType)
            );
        }

        log.info("成功加载提示词模板: {}/{}", promptType, agentType);
        return content;
    }

    /**
     * 渲染模板(替换占位符)
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

    private String buildTemplatePath(String promptType, String agentType) {
        return String.format("%s/%s/%s.md", TEMPLATE_BASE_PATH, agentType, promptType);
    }

    private String readTemplateFromResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                log.debug("模板文件不存在: {}", path);
                return null;
            }
            byte[] bytes = new byte[(int) resource.contentLength()];
            resource.getInputStream().read(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("读取模板文件失败: {}, 原因: {}", path, e.getMessage());
            return null;
        }
    }
}
