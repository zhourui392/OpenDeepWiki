package ai.opendw.koalawiki.app.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 提示词模板服务
 * 管理所有AI提示词模板
 */
@Slf4j
@Component
public class PromptTemplateService {

    /**
     * 获取README生成提示词
     */
    public String getReadmePrompt(ReadmeContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的技术文档编写专家。请为以下项目生成一个完整、专业的README.md文档。\n\n");

        prompt.append("## 项目信息\n");
        prompt.append("- 项目名称: ").append(context.getRepositoryName()).append("\n");
        prompt.append("- 所有者: ").append(context.getOwner()).append("\n");

        if (context.getDescription() != null) {
            prompt.append("- 描述: ").append(context.getDescription()).append("\n");
        }

        if (context.getPrimaryLanguage() != null) {
            prompt.append("- 主要语言: ").append(context.getPrimaryLanguage()).append("\n");
        }

        if (context.getTechStack() != null && !context.getTechStack().isEmpty()) {
            prompt.append("- 技术栈: ").append(String.join(", ", context.getTechStack())).append("\n");
        }

        if (context.getDirectoryStructure() != null) {
            prompt.append("\n## 目录结构\n```\n");
            prompt.append(context.getDirectoryStructure());
            prompt.append("\n```\n");
        }

        prompt.append("\n## 要求\n");
        prompt.append("1. 生成的README应该包含以下部分：\n");
        prompt.append("   - 项目标题和简介\n");
        prompt.append("   - 主要功能和特性\n");
        prompt.append("   - 技术栈说明\n");
        prompt.append("   - 快速开始指南\n");
        prompt.append("   - 目录结构说明（如果提供）\n");
        prompt.append("   - 贡献指南（可选）\n");
        prompt.append("   - 许可证信息（如果能推断）\n\n");

        prompt.append("2. 语言要求: ");
        if ("Chinese".equalsIgnoreCase(context.getLanguage())) {
            prompt.append("使用中文编写\n");
        } else {
            prompt.append("使用英文编写\n");
        }

        prompt.append("3. 格式要求: 使用标准Markdown格式，结构清晰，易读性强\n");
        prompt.append("4. 风格要求: 专业、简洁、友好\n\n");

        prompt.append("请直接输出README.md的内容，不要包含其他说明文字：\n");

        return prompt.toString();
    }

    /**
     * 获取目录优化提示词
     */
    public String getCatalogOptimizationPrompt(String catalogData, int maxFiles) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个代码仓库分析专家。请分析以下文件目录，并智能过滤出最重要的文件。\n\n");

        prompt.append("## 原始目录\n```\n");
        prompt.append(catalogData);
        prompt.append("\n```\n\n");

        prompt.append("## 任务要求\n");
        prompt.append("1. 保留以下类型的重要文件：\n");
        prompt.append("   - 文档文件（README, CHANGELOG, LICENSE等）\n");
        prompt.append("   - 配置文件（package.json, pom.xml, build.gradle等）\n");
        prompt.append("   - 核心源代码文件\n");
        prompt.append("   - 测试文件（主要的测试文件）\n\n");

        prompt.append("2. 过滤以下类型的文件：\n");
        prompt.append("   - 构建产物（dist/, build/, target/等）\n");
        prompt.append("   - 依赖文件（node_modules/, vendor/等）\n");
        prompt.append("   - 临时文件和缓存\n");
        prompt.append("   - 重复或冗余的文件\n\n");

        prompt.append("3. 目标数量: 优化后保留约").append(maxFiles).append("个最重要的文件\n\n");

        prompt.append("4. 输出格式: 请直接输出优化后的目录结构，保持原有的格式，不要添加额外的说明\n\n");

        return prompt.toString();
    }

    /**
     * 获取文档摘要提示词
     */
    public String getSummaryPrompt(String content, int maxLength) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为以下文档内容生成一个简洁的摘要。\n\n");

        prompt.append("## 文档内容\n```\n");
        // 限制内容长度，避免token超限
        if (content.length() > 4000) {
            prompt.append(content.substring(0, 4000));
            prompt.append("\n... (内容已截断)\n");
        } else {
            prompt.append(content);
        }
        prompt.append("\n```\n\n");

        prompt.append("## 要求\n");
        prompt.append("1. 摘要长度: 不超过").append(maxLength).append("个字符\n");
        prompt.append("2. 提取文档的核心要点和主要内容\n");
        prompt.append("3. 语言简洁明了，避免冗余\n");
        prompt.append("4. 直接输出摘要内容，不要包含\"摘要：\"等前缀\n\n");

        return prompt.toString();
    }

    /**
     * 获取问答提示词
     */
    public String getQAPrompt(String question, String documentContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个技术文档助手。请基于提供的文档内容回答用户的问题。\n\n");

        prompt.append("## 文档内容\n```\n");
        // 限制上下文长度
        if (documentContext.length() > 3000) {
            prompt.append(documentContext.substring(0, 3000));
            prompt.append("\n... (内容已截断)\n");
        } else {
            prompt.append(documentContext);
        }
        prompt.append("\n```\n\n");

        prompt.append("## 用户问题\n");
        prompt.append(question).append("\n\n");

        prompt.append("## 要求\n");
        prompt.append("1. 仅基于提供的文档内容回答\n");
        prompt.append("2. 如果文档中没有相关信息，请明确说明\n");
        prompt.append("3. 回答要准确、简洁、有帮助\n");
        prompt.append("4. 如果可以，提供具体的代码示例或引用\n\n");

        return prompt.toString();
    }

    /**
     * 获取标签生成提示词
     */
    public String getTagsPrompt(String content, int maxTags) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为以下内容生成相关的标签（tags）。\n\n");

        prompt.append("## 内容\n```\n");
        if (content.length() > 2000) {
            prompt.append(content.substring(0, 2000));
            prompt.append("\n... (内容已截断)\n");
        } else {
            prompt.append(content);
        }
        prompt.append("\n```\n\n");

        prompt.append("## 要求\n");
        prompt.append("1. 生成").append(maxTags).append("个最相关的标签\n");
        prompt.append("2. 标签应该简洁、准确，能代表内容的关键主题\n");
        prompt.append("3. 每行一个标签，不要编号\n");
        prompt.append("4. 标签可以是技术名称、概念、主题等\n\n");

        return prompt.toString();
    }

    /**
     * 获取技术栈分析提示词
     */
    public String getTechStackPrompt(String fileListStr, String readmeContent) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个代码分析专家。请分析以下项目信息，识别项目使用的技术栈。\n\n");

        prompt.append("## 文件列表\n```\n");
        prompt.append(fileListStr);
        prompt.append("\n```\n\n");

        if (readmeContent != null && !readmeContent.isEmpty()) {
            prompt.append("## README内容\n```\n");
            if (readmeContent.length() > 2000) {
                prompt.append(readmeContent.substring(0, 2000));
                prompt.append("\n... (内容已截断)\n");
            } else {
                prompt.append(readmeContent);
            }
            prompt.append("\n```\n\n");
        }

        prompt.append("## 要求\n");
        prompt.append("1. 基于文件扩展名、配置文件等识别技术栈\n");
        prompt.append("2. 包括：编程语言、框架、数据库、工具等\n");
        prompt.append("3. 每行一个技术名称，不要添加版本号或说明\n");
        prompt.append("4. 按重要程度排序\n");
        prompt.append("5. 最多列出10个主要技术\n\n");

        return prompt.toString();
    }

    /**
     * 获取项目描述生成提示词
     */
    public String getProjectDescriptionPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个技术项目分析专家。请为以下项目生成一个简洁的描述。\n\n");

        prompt.append("## 项目信息\n");
        context.forEach((key, value) -> {
            prompt.append("- ").append(key).append(": ").append(value).append("\n");
        });

        prompt.append("\n## 要求\n");
        prompt.append("1. 描述长度: 2-3句话，约50-100字\n");
        prompt.append("2. 突出项目的核心价值和主要功能\n");
        prompt.append("3. 语言简洁专业，易于理解\n");
        prompt.append("4. 直接输出描述内容，不要包含其他文字\n\n");

        return prompt.toString();
    }

    /**
     * 替换模板变量
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
