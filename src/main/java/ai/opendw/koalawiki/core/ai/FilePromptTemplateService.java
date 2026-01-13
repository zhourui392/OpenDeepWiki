package ai.opendw.koalawiki.core.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件提示词模板服务
 * 从resources/prompts目录加载提示词文件
 *
 * @author zhourui(V33215020)
 * @since 2025/01/13
 */
@Slf4j
@Service
public class FilePromptTemplateService {

    private static final String PROMPT_DIR = "prompts/";
    private static final String SERVICE_DOC_PROMPT = "service-doc-prompt.txt";
    private static final String DOMAIN_DOC_PROMPT = "domain-doc-prompt.txt";

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    /**
     * 加载服务文档提示词
     */
    public String loadServiceDocPrompt() {
        return loadPromptFile(SERVICE_DOC_PROMPT);
    }

    /**
     * 加载领域文档提示词
     */
    public String loadDomainDocPrompt() {
        return loadPromptFile(DOMAIN_DOC_PROMPT);
    }

    /**
     * 加载并渲染服务文档提示词
     */
    public String renderServiceDocPrompt(String serviceName, String serviceDescription,
                                          String domainName, String sourceCode) {
        String template = loadServiceDocPrompt();
        return template
                .replace("{serviceName}", serviceName != null ? serviceName : "")
                .replace("{serviceDescription}", serviceDescription != null ? serviceDescription : "")
                .replace("{domainName}", domainName != null ? domainName : "")
                .replace("{sourceCode}", sourceCode != null ? sourceCode : "");
    }

    /**
     * 加载并渲染领域文档提示词
     */
    public String renderDomainDocPrompt(String domainName, String domainDescription,
                                         String serviceList, String sourceCodeSummary) {
        String template = loadDomainDocPrompt();
        return template
                .replace("{domainName}", domainName != null ? domainName : "")
                .replace("{domainDescription}", domainDescription != null ? domainDescription : "")
                .replace("{serviceList}", serviceList != null ? serviceList : "")
                .replace("{sourceCodeSummary}", sourceCodeSummary != null ? sourceCodeSummary : "");
    }

    /**
     * 加载提示词文件
     */
    private String loadPromptFile(String fileName) {
        return templateCache.computeIfAbsent(fileName, this::doLoadPromptFile);
    }

    private String doLoadPromptFile(String fileName) {
        String path = PROMPT_DIR + fileName;
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                String content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                log.info("成功加载提示词文件: {}", path);
                return content;
            }
        } catch (IOException e) {
            log.error("加载提示词文件失败: {}", path, e);
            throw new IllegalStateException("无法加载提示词文件: " + path, e);
        }
    }

    /**
     * 清除缓存（用于热更新）
     */
    public void clearCache() {
        templateCache.clear();
        log.info("提示词模板缓存已清除");
    }
}
