package ai.opendw.koalawiki.app.ai;

import ai.opendw.koalawiki.infra.ai.ChatMessage;
import ai.opendw.koalawiki.infra.ai.IAIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI服务实现
 */
@Slf4j
@Service
public class AIServiceImpl implements IAIService {

    @Autowired
    private IAIClient aiClient;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Override
    @Cacheable(value = "aiReadme", key = "#context.repositoryName + '_' + #context.owner")
    public String generateReadme(ReadmeContext context) {
        log.info("生成README: {}/{}", context.getOwner(), context.getRepositoryName());

        try {
            String prompt = promptTemplateService.getReadmePrompt(context);
            String result = aiClient.complete(prompt);

            log.info("README生成成功，长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("README生成失败", e);
            throw new RuntimeException("README生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Cacheable(value = "aiCatalog", key = "#catalogData.hashCode() + '_' + #maxFiles")
    public String optimizeCatalog(String catalogData, int maxFiles) {
        log.info("优化目录，目标文件数: {}", maxFiles);

        try {
            String prompt = promptTemplateService.getCatalogOptimizationPrompt(catalogData, maxFiles);
            String result = aiClient.complete(prompt);

            log.info("目录优化成功");
            return result;
        } catch (Exception e) {
            log.error("目录优化失败", e);
            // 优化失败时返回原始数据
            log.warn("返回原始目录数据");
            return catalogData;
        }
    }

    @Override
    @Cacheable(value = "aiSummary", key = "#content.hashCode() + '_' + #maxLength")
    public String summarizeDocument(String content, int maxLength) {
        log.info("生成文档摘要，内容长度: {}, 目标长度: {}", content.length(), maxLength);

        try {
            String prompt = promptTemplateService.getSummaryPrompt(content, maxLength);
            String result = aiClient.complete(prompt);

            log.info("摘要生成成功，长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("摘要生成失败", e);
            // 摘要失败时返回前N个字符
            return content.substring(0, Math.min(maxLength, content.length())) + "...";
        }
    }

    @Override
    public String answerQuestion(String question, String documentContext) {
        log.info("回答问题: {}", question);

        try {
            String prompt = promptTemplateService.getQAPrompt(question, documentContext);

            // 使用聊天模式，提供更好的对话体验
            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.system("你是一个专业的技术文档助手，负责回答关于代码仓库的问题。"),
                    ChatMessage.user(prompt)
            );

            String result = aiClient.chat(messages);
            log.info("问题回答完成");
            return result;
        } catch (Exception e) {
            log.error("问题回答失败", e);
            return "抱歉，我无法回答这个问题。请尝试重新表述或查看文档。";
        }
    }

    @Override
    @Cacheable(value = "aiTags", key = "#content.hashCode() + '_' + #maxTags")
    public List<String> generateTags(String content, int maxTags) {
        log.info("生成标签，最多: {}", maxTags);

        try {
            String prompt = promptTemplateService.getTagsPrompt(content, maxTags);
            String result = aiClient.complete(prompt);

            // 解析标签（每行一个）
            List<String> tags = Arrays.stream(result.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#")) // 过滤Markdown标题
                    .filter(line -> !line.matches("^\\d+\\..*")) // 过滤编号
                    .limit(maxTags)
                    .collect(Collectors.toList());

            log.info("生成标签数量: {}", tags.size());
            return tags;
        } catch (Exception e) {
            log.error("标签生成失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    @Cacheable(value = "aiTechStack", key = "#fileList.hashCode()")
    public List<String> analyzeTechStack(List<String> fileList, String readmeContent) {
        log.info("分析技术栈，文件数: {}", fileList.size());

        try {
            String fileListStr = String.join("\n", fileList);
            String prompt = promptTemplateService.getTechStackPrompt(fileListStr, readmeContent);
            String result = aiClient.complete(prompt);

            // 解析技术栈（每行一个）
            List<String> techStack = Arrays.stream(result.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))
                    .filter(line -> !line.matches("^\\d+\\..*"))
                    .limit(10)
                    .collect(Collectors.toList());

            log.info("识别到技术栈: {}", techStack);
            return techStack;
        } catch (Exception e) {
            log.error("技术栈分析失败", e);
            return inferTechStackFromFiles(fileList);
        }
    }

    @Override
    @Cacheable(value = "aiDescription", key = "#context.hashCode()")
    public String generateProjectDescription(Map<String, Object> context) {
        log.info("生成项目描述");

        try {
            String prompt = promptTemplateService.getProjectDescriptionPrompt(context);
            String result = aiClient.complete(prompt);

            log.info("项目描述生成成功");
            return result.trim();
        } catch (Exception e) {
            log.error("项目描述生成失败", e);
            return "一个开源项目";
        }
    }

    /**
     * 从文件列表推断技术栈（降级方案）
     */
    private List<String> inferTechStackFromFiles(List<String> fileList) {
        List<String> techStack = new ArrayList<>();

        // 简单的文件扩展名匹配
        if (fileList.stream().anyMatch(f -> f.endsWith(".java"))) {
            techStack.add("Java");
        }
        if (fileList.stream().anyMatch(f -> f.endsWith(".js") || f.endsWith(".jsx"))) {
            techStack.add("JavaScript");
        }
        if (fileList.stream().anyMatch(f -> f.endsWith(".ts") || f.endsWith(".tsx"))) {
            techStack.add("TypeScript");
        }
        if (fileList.stream().anyMatch(f -> f.endsWith(".py"))) {
            techStack.add("Python");
        }
        if (fileList.stream().anyMatch(f -> f.endsWith(".go"))) {
            techStack.add("Go");
        }
        if (fileList.stream().anyMatch(f -> f.endsWith(".rs"))) {
            techStack.add("Rust");
        }
        if (fileList.stream().anyMatch(f -> f.contains("pom.xml"))) {
            techStack.add("Maven");
        }
        if (fileList.stream().anyMatch(f -> f.contains("package.json"))) {
            techStack.add("Node.js");
        }
        if (fileList.stream().anyMatch(f -> f.contains("requirements.txt"))) {
            techStack.add("Python");
        }
        if (fileList.stream().anyMatch(f -> f.contains("Dockerfile"))) {
            techStack.add("Docker");
        }

        return techStack;
    }
}
