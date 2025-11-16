package ai.opendw.koalawiki.core.ai;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文档生成提示词构建器
 *
 * <p>职责: 根据代码和上下文构建AI提示词</p>
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Component
public class DocumentPromptBuilder {

    /**
     * 为Claude构建中文提示词
     *
     * @param code    源代码
     * @param context 上下文信息
     * @return 提示词文本
     */
    public String buildChinesePrompt(String code, Map<String, Object> context) {
        String className = (String) context.getOrDefault("className", "Unknown");
        String packageName = (String) context.getOrDefault("packageName", "");
        String language = (String) context.getOrDefault("language", "java");

        return "请为以下" + language + "类生成详细的技术文档。\n\n" +
            "类名: " + className + "\n" +
            "包名: " + packageName + "\n\n" +
            "源代码:\n" +
            "```" + language + "\n" +
            code + "\n" +
            "```\n\n" +
            "请按以下格式生成Markdown文档:\n\n" +
            "# " + className + "\n\n" +
            "## 概述\n" +
            "(用2-3句话描述这个类的作用和职责)\n\n" +
            "## 核心功能\n" +
            "(列出主要功能点,每个功能点一段简短描述)\n\n" +
            "## 主要方法\n" +
            "(为每个public方法生成说明,包括:\n" +
            "- 方法签名\n" +
            "- 功能描述\n" +
            "- 参数说明\n" +
            "- 返回值说明\n" +
            "- 简单的使用示例)\n\n" +
            "## 使用示例\n" +
            "(提供1-2个实际使用示例代码)\n\n" +
            "## 注意事项\n" +
            "(如有特殊注意事项,列出来)\n\n" +
            "要求:\n" +
            "1. 使用清晰简洁的中文\n" +
            "2. 代码示例使用" + language + "语法高亮\n" +
            "3. 重点突出类的设计意图和使用场景\n" +
            "4. 避免过度技术化的术语\n";
    }

    /**
     * 为Codex构建英文提示词
     *
     * @param code    源代码
     * @param context 上下文信息
     * @return 提示词文本
     */
    public String buildEnglishPrompt(String code, Map<String, Object> context) {
        String className = (String) context.getOrDefault("className", "Unknown");
        String packageName = (String) context.getOrDefault("packageName", "");
        String language = (String) context.getOrDefault("language", "java");

        return "Generate detailed technical documentation for the following " + language + " class.\n\n" +
            "Class Name: " + className + "\n" +
            "Package: " + packageName + "\n\n" +
            "Source Code:\n" +
            "```" + language + "\n" +
            code + "\n" +
            "```\n\n" +
            "Please generate Markdown documentation with:\n\n" +
            "# " + className + "\n\n" +
            "## Overview\n" +
            "(2-3 sentences describing the purpose and responsibility)\n\n" +
            "## Core Features\n" +
            "(List main features with brief descriptions)\n\n" +
            "## Main Methods\n" +
            "(Document each public method with:\n" +
            "- Method signature\n" +
            "- Description\n" +
            "- Parameters\n" +
            "- Return value\n" +
            "- Usage example)\n\n" +
            "## Usage Examples\n" +
            "(Provide 1-2 practical code examples)\n\n" +
            "## Notes\n" +
            "(Any special considerations)\n\n" +
            "Requirements:\n" +
            "1. Use clear and concise language\n" +
            "2. Use " + language + " syntax highlighting for code examples\n" +
            "3. Focus on design intent and usage scenarios\n" +
            "4. Avoid overly technical jargon\n";
    }
}
