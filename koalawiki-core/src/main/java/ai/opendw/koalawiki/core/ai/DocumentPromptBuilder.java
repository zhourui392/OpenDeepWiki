package ai.opendw.koalawiki.core.ai;

import ai.opendw.koalawiki.core.analysis.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 构建项目级架构分析提示词
     *
     * @param structure 项目结构
     * @return 提示词文本
     */
    public String buildProjectAnalysisPrompt(ProjectStructure structure) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# 项目架构分析任务\n\n");
        prompt.append("请基于以下信息，生成完整的项目架构文档。\n\n");

        // 1. 项目基本信息
        prompt.append("## 项目信息\n\n");
        prompt.append(String.format("- **项目名称**: %s\n", structure.getProjectName()));
        prompt.append(String.format("- **模块数量**: %d\n", structure.getModules().size()));
        prompt.append(String.format("- **类总数**: %d\n", structure.getClasses().size()));
        prompt.append(String.format("- **入口点总数**: %d\n\n", structure.getEntryPoints().size()));

        // 2. 模块列表
        if (structure.getModules().size() > 1) {
            prompt.append("### 模块列表\n\n");
            prompt.append("| 模块名 | 类数量 | 包数量 | 描述 |\n");
            prompt.append("|--------|--------|--------|------|\n");

            for (ModuleInfo module : structure.getModules()) {
                prompt.append(String.format("| %s | %d | %d | %s |\n",
                        module.getName(),
                        module.getClassCount(),
                        module.getPackageCount(),
                        module.getDescription() != null ? module.getDescription() : "-"
                ));
            }
            prompt.append("\n");
        }

        // 3. 入口点统计
        Map<EntryType, Long> typeCount = structure.getEntryPoints().stream()
                .collect(Collectors.groupingBy(EntryPoint::getType, Collectors.counting()));

        prompt.append("### 入口点分布\n\n");
        typeCount.forEach((type, count) ->
                prompt.append(String.format("- %s: %d个\n", type, count))
        );
        prompt.append("\n");

        // 4. HTTP接口列表
        List<EntryPoint> httpEndpoints = structure.getEntryPoints().stream()
                .filter(ep -> ep.getType() == EntryType.HTTP)
                .collect(Collectors.toList());

        if (!httpEndpoints.isEmpty()) {
            prompt.append("## HTTP接口清单\n\n");

            // 如果是多模块，按模块分组
            if (structure.getModules().size() > 1) {
                prompt.append("| 模块 | HTTP方法 | 路径 | 控制器方法 | 主要调用 |\n");
                prompt.append("|------|----------|------|------------|----------|\n");

                httpEndpoints.forEach(ep -> {
                    String moduleName = (String) ep.getAnnotations().get("module");
                    String calls = ep.getDirectCalls().isEmpty() ? "-" :
                            ep.getDirectCalls().stream().limit(3).collect(Collectors.joining(", "));
                    prompt.append(String.format("| %s | %s | %s | %s.%s() | %s |\n",
                            moduleName != null ? moduleName : "-",
                            ep.getHttpMethod(),
                            ep.getPath(),
                            getSimpleClassName(ep.getClassName()),
                            ep.getMethodName(),
                            calls
                    ));
                });
            } else {
                prompt.append("| HTTP方法 | 路径 | 控制器方法 | 主要调用 |\n");
                prompt.append("|----------|------|------------|----------|\n");

                httpEndpoints.forEach(ep -> {
                    String calls = ep.getDirectCalls().isEmpty() ? "-" :
                            ep.getDirectCalls().stream().limit(3).collect(Collectors.joining(", "));
                    prompt.append(String.format("| %s | %s | %s.%s() | %s |\n",
                            ep.getHttpMethod(),
                            ep.getPath(),
                            getSimpleClassName(ep.getClassName()),
                            ep.getMethodName(),
                            calls
                    ));
                });
            }
            prompt.append("\n");
        }

        // 4. Dubbo服务列表
        List<EntryPoint> dubboServices = structure.getEntryPoints().stream()
                .filter(ep -> ep.getType() == EntryType.DUBBO)
                .collect(Collectors.toList());

        if (!dubboServices.isEmpty()) {
            prompt.append("## Dubbo服务清单\n\n");
            prompt.append("| 服务接口 | 方法 | 主要调用 |\n");
            prompt.append("|----------|------|----------|\n");

            // 按类分组
            Map<String, List<EntryPoint>> byClass = dubboServices.stream()
                    .collect(Collectors.groupingBy(EntryPoint::getClassName));

            byClass.forEach((className, methods) -> {
                String methodList = methods.stream()
                        .map(EntryPoint::getMethodName)
                        .collect(Collectors.joining(", "));
                String calls = methods.get(0).getDirectCalls().isEmpty() ? "-" :
                        methods.get(0).getDirectCalls().stream().limit(3).collect(Collectors.joining(", "));

                prompt.append(String.format("| %s | %s | %s |\n",
                        getSimpleClassName(className),
                        methodList,
                        calls
                ));
            });
            prompt.append("\n");
        }

        // 5. 定时任务列表
        List<EntryPoint> scheduledTasks = structure.getEntryPoints().stream()
                .filter(ep -> ep.getType() == EntryType.SCHEDULED)
                .collect(Collectors.toList());

        if (!scheduledTasks.isEmpty()) {
            prompt.append("## 定时任务清单\n\n");
            prompt.append("| 任务类 | 任务方法 | Cron表达式 | 主要调用 |\n");
            prompt.append("|--------|----------|------------|----------|\n");

            scheduledTasks.forEach(ep -> {
                String calls = ep.getDirectCalls().isEmpty() ? "-" :
                        ep.getDirectCalls().stream().limit(3).collect(Collectors.joining(", "));
                prompt.append(String.format("| %s | %s() | %s | %s |\n",
                        getSimpleClassName(ep.getClassName()),
                        ep.getMethodName(),
                        ep.getPath() != null ? ep.getPath() : "-",
                        calls
                ));
            });
            prompt.append("\n");
        }

        // 6. MQ消费者列表
        List<EntryPoint> mqConsumers = structure.getEntryPoints().stream()
                .filter(ep -> ep.getType() == EntryType.MQ)
                .collect(Collectors.toList());

        if (!mqConsumers.isEmpty()) {
            prompt.append("## MQ消费者清单\n\n");
            prompt.append("| 消费者类 | 消费方法 | 队列/主题 | 主要调用 |\n");
            prompt.append("|----------|----------|-----------|----------|\n");

            mqConsumers.forEach(ep -> {
                String calls = ep.getDirectCalls().isEmpty() ? "-" :
                        ep.getDirectCalls().stream().limit(3).collect(Collectors.joining(", "));
                prompt.append(String.format("| %s | %s() | %s | %s |\n",
                        getSimpleClassName(ep.getClassName()),
                        ep.getMethodName(),
                        ep.getPath() != null ? ep.getPath() : "-",
                        calls
                ));
            });
            prompt.append("\n");
        }

        // 7. 分析要求
        prompt.append("---\n\n");
        prompt.append("## 文档生成要求\n\n");
        prompt.append("请根据以上信息生成完整的项目架构文档，包括：\n\n");
        prompt.append("### 1. 项目概述\n");
        prompt.append("- 项目的整体功能和定位\n");
        prompt.append("- 技术栈分析（基于识别的框架和组件）\n");
        prompt.append("- 架构风格（单体/微服务等）\n\n");

        prompt.append("### 2. 模块结构\n");
        prompt.append("- 项目的模块划分\n");
        prompt.append("- 各模块的职责和功能\n\n");

        prompt.append("### 3. 服务功能清单\n");
        prompt.append("- 按功能域对HTTP接口进行分组\n");
        prompt.append("- 列出每个功能域的主要接口和功能\n\n");

        prompt.append("### 4. 服务入口汇总\n");
        prompt.append("- HTTP接口的功能分类\n");
        prompt.append("- Dubbo服务的对外能力\n");
        prompt.append("- 定时任务的调度说明\n");
        prompt.append("- MQ消费的业务场景\n\n");

        prompt.append("### 5. 核心业务链路\n");
        prompt.append("- 识别3-5个核心业务流程\n");
        prompt.append("- 描述每个流程的调用链路\n");
        prompt.append("- 标注关键的业务节点\n\n");

        prompt.append("### 6. 架构特点和建议\n");
        prompt.append("- 当前架构的优点\n");
        prompt.append("- 潜在的改进点\n");
        prompt.append("- 技术债务提示\n\n");

        prompt.append("**格式要求**：\n");
        prompt.append("- 使用Markdown格式\n");
        prompt.append("- 使用表格和列表增强可读性\n");
        prompt.append("- 使用中文编写\n");
        prompt.append("- 重点突出业务价值和架构设计\n");

        return prompt.toString();
    }

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

    /**
     * 获取简单类名
     */
    private String getSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return "";
        }
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
}
