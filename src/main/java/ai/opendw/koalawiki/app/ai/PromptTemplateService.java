package ai.opendw.koalawiki.app.ai;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 提示词模板服务
 * 提供各种AI提示词模板
 *
 * @author zhourui(V33215020)
 * @since 2025/01/14
 */
@Service
public class PromptTemplateService {

    public String getReadmePrompt(ReadmeContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下项目生成README文档：\n\n");
        prompt.append("项目名称: ").append(context.getRepositoryName()).append("\n");
        prompt.append("所有者: ").append(context.getOwner()).append("\n");
        if (context.getDescription() != null) {
            prompt.append("描述: ").append(context.getDescription()).append("\n");
        }
        if (context.getFiles() != null && !context.getFiles().isEmpty()) {
            prompt.append("\n文件列表:\n");
            context.getFiles().forEach(f -> prompt.append("- ").append(f).append("\n"));
        }
        prompt.append("\n请生成完整的Markdown格式README文档。");
        return prompt.toString();
    }

    public String getCatalogOptimizationPrompt(String catalogData, int maxFiles) {
        return String.format(
            "请优化以下目录结构，保留最重要的%d个文件：\n\n%s\n\n" +
            "请返回优化后的目录结构。",
            maxFiles, catalogData
        );
    }

    public String getSummaryPrompt(String content, int maxLength) {
        return String.format(
            "请为以下内容生成不超过%d字的摘要：\n\n%s",
            maxLength, content
        );
    }

    public String getQAPrompt(String question, String documentContext) {
        return String.format(
            "基于以下文档内容回答问题：\n\n文档内容：\n%s\n\n问题：%s",
            documentContext, question
        );
    }

    public String getTagsPrompt(String content, int maxTags) {
        return String.format(
            "请为以下内容生成最多%d个标签，每行一个：\n\n%s",
            maxTags, content
        );
    }

    public String getTechStackPrompt(String fileListStr, String readmeContent) {
        return String.format(
            "请分析以下项目的技术栈，每行列出一个技术：\n\n文件列表：\n%s\n\nREADME内容：\n%s",
            fileListStr, readmeContent != null ? readmeContent : ""
        );
    }

    public String getProjectDescriptionPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下项目生成简短描述（不超过200字）：\n\n");
        context.forEach((k, v) -> prompt.append(k).append(": ").append(v).append("\n"));
        return prompt.toString();
    }
}
