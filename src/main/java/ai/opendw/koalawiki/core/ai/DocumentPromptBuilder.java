package ai.opendw.koalawiki.core.ai;

import ai.opendw.koalawiki.core.analysis.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
@RequiredArgsConstructor
public class DocumentPromptBuilder {

    private final AIPromptTemplateService promptTemplateLoader;

    /**
     * 构建简单的架构分析提示词
     * 让Claude在工作空间下自己读取代码进行分析
     *
     * @return 提示词文本
     */
    public String buildSimpleArchitecturePrompt() {
        return promptTemplateLoader.loadTemplate("project_analysis", "claude");
    }

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

        // 7. 从资源文件加载输出要求模板
        prompt.append("---\n\n");
        String outputTemplate = promptTemplateLoader.loadTemplate("project_analysis", "claude");
        prompt.append(outputTemplate);

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
        Map<String, String> variables = new HashMap<>();
        variables.put("className", (String) context.getOrDefault("className", "Unknown"));
        variables.put("packageName", (String) context.getOrDefault("packageName", ""));
        variables.put("language", (String) context.getOrDefault("language", "java"));
        variables.put("code", code != null ? code : "");

        return promptTemplateLoader.loadAndRender("class_chinese", "claude", variables);
    }

    /**
     * 构建服务文档提示词
     *
     * @param templateId 模板ID
     * @param agentType Agent类型
     * @param context 上下文信息
     * @return 提示词文本
     */
    public String buildServicePrompt(String templateId, String agentType, Map<String, Object> context) {
        Map<String, String> variables = new HashMap<>();
        variables.put("serviceName", (String) context.getOrDefault("serviceName", ""));
        variables.put("serviceId", (String) context.getOrDefault("serviceId", ""));
        variables.put("docType", (String) context.getOrDefault("docType", ""));
        variables.put("filePath", (String) context.getOrDefault("filePath", ""));
        variables.put("code", (String) context.getOrDefault("code", ""));
        variables.put("className", (String) context.getOrDefault("className", ""));
        variables.put("packageName", (String) context.getOrDefault("packageName", ""));
        variables.put("summary", (String) context.getOrDefault("summary", ""));
        variables.put("dependencies", (String) context.getOrDefault("dependencies", ""));

        return promptTemplateLoader.loadAndRender(templateId, agentType, variables);
    }


    /**
     * 构建系统说明文档提示词
     *
     * @param context ReadmeContext上下文
     * @param serviceName 服务名称
     * @return 提示词文本
     */
    public String buildReadmePrompt(ai.opendw.koalawiki.core.analysis.model.ReadmeContext context, String serviceName) {
        Map<String, String> variables = new HashMap<>();
        variables.put("projectInfo", formatProjectInfo(context, serviceName));
        variables.put("modules", formatModules(context.getModules()));
        variables.put("packages", formatPackages(context.getPackages()));
        variables.put("features", formatFeatures(context.getFeatures()));
        variables.put("startupGuide", context.getStartupGuide() != null ? context.getStartupGuide() : "");
        variables.put("testGuide", context.getTestGuide() != null ? context.getTestGuide() : "");
        variables.put("dataModels", formatDataModels(context.getDataModels()));

        return promptTemplateLoader.loadAndRender("SYSTEM_DOC", "claude", variables);
    }

    private String formatProjectInfo(ai.opendw.koalawiki.core.analysis.model.ReadmeContext context, String serviceName) {
        return "- 项目名称: " + (context.getProjectName() != null ? context.getProjectName() : serviceName) + "\n" +
               "- 服务名称: " + serviceName;
    }

    private String formatModules(List<ai.opendw.koalawiki.core.analysis.model.ReadmeContext.MavenModule> modules) {
        if (modules == null || modules.isEmpty()) {
            return "无模块信息";
        }

        StringBuilder sb = new StringBuilder();
        for (ai.opendw.koalawiki.core.analysis.model.ReadmeContext.MavenModule module : modules) {
            sb.append("- ").append(module.getName());
            if (module.getDescription() != null) {
                sb.append(": ").append(module.getDescription());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatPackages(List<ai.opendw.koalawiki.core.analysis.model.ReadmeContext.PackageInfo> packages) {
        if (packages == null || packages.isEmpty()) {
            return "无包信息";
        }

        StringBuilder sb = new StringBuilder();
        for (ai.opendw.koalawiki.core.analysis.model.ReadmeContext.PackageInfo pkg : packages) {
            sb.append("- ").append(pkg.getPackageName())
              .append(" (").append(pkg.getClassCount()).append("个类): ")
              .append(pkg.getPurpose()).append("\n");
        }
        return sb.toString();
    }

    private String formatFeatures(List<ai.opendw.koalawiki.core.analysis.model.ReadmeContext.Feature> features) {
        if (features == null || features.isEmpty()) {
            return "无功能信息";
        }

        StringBuilder sb = new StringBuilder();
        for (ai.opendw.koalawiki.core.analysis.model.ReadmeContext.Feature feature : features) {
            sb.append("- ").append(feature.getName());
            if (feature.getDescription() != null) {
                sb.append(": ").append(feature.getDescription());
            }
            if (feature.getEndpoint() != null) {
                sb.append(" (").append(feature.getEndpoint()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatDataModels(List<ai.opendw.koalawiki.core.analysis.model.ReadmeContext.DataModel> models) {
        if (models == null || models.isEmpty()) {
            return "无数据模型";
        }

        StringBuilder sb = new StringBuilder();
        for (ai.opendw.koalawiki.core.analysis.model.ReadmeContext.DataModel model : models) {
            sb.append("- ").append(model.getEntityName())
              .append(" → ").append(model.getTableName()).append("\n");
        }
        return sb.toString();
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
