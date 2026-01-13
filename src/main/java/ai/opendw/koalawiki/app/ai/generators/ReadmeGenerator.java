package ai.opendw.koalawiki.app.ai.generators;

import ai.opendw.koalawiki.app.ai.IAIService;
import ai.opendw.koalawiki.app.ai.ReadmeContext;
import ai.opendw.koalawiki.domain.warehouse.Warehouse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * README生成器
 * 为代码仓库自动生成README文档
 */
@Slf4j
@Component
public class ReadmeGenerator {

    @Autowired
    private IAIService aiService;

    /**
     * 为仓库生成README
     *
     * @param warehouse 仓库信息
     * @param directoryStructure 目录结构
     * @param existingReadme 现有README（可选）
     * @return 生成的README内容
     */
    public String generate(Warehouse warehouse, String directoryStructure, String existingReadme) {
        log.info("开始生成README: {}/{}", warehouse.getOrganizationName(), warehouse.getName());

        // 构建上下文
        ReadmeContext context = buildContext(warehouse, directoryStructure, existingReadme);

        // 调用AI生成
        String readme = aiService.generateReadme(context);

        // 后处理
        readme = postProcess(readme);

        log.info("README生成完成，长度: {}", readme.length());
        return readme;
    }

    /**
     * 为仓库生成简化版README
     */
    public String generateSimple(String repositoryName, String owner, String description) {
        log.info("生成简化版README: {}/{}", owner, repositoryName);

        ReadmeContext context = ReadmeContext.builder()
                .repositoryName(repositoryName)
                .owner(owner)
                .description(description)
                .language("Chinese")
                .build();

        return aiService.generateReadme(context);
    }

    /**
     * 构建README生成上下文
     */
    private ReadmeContext buildContext(Warehouse warehouse, String directoryStructure, String existingReadme) {
        ReadmeContext.ReadmeContextBuilder builder = ReadmeContext.builder()
                .repositoryName(warehouse.getName())
                .owner(warehouse.getOrganizationName())
                .description(warehouse.getDescription())
                .directoryStructure(directoryStructure)
                .existingReadme(existingReadme)
                .language("Chinese"); // 默认中文

        // 分析技术栈
        List<String> techStack = analyzeTechStack(warehouse, directoryStructure);
        builder.techStack(techStack);

        // 确定主要语言
        String primaryLanguage = inferPrimaryLanguage(directoryStructure);
        builder.primaryLanguage(primaryLanguage);

        // 提取主要文件
        List<String> files = extractMainFiles(directoryStructure);
        builder.files(files);

        // 额外元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("stars", warehouse.getStars());
        metadata.put("forks", warehouse.getForks());
        builder.metadata(metadata);

        return builder.build();
    }

    /**
     * 分析技术栈
     */
    private List<String> analyzeTechStack(Warehouse warehouse, String directoryStructure) {
        // 从目录结构中提取文件列表
        List<String> fileList = extractFileList(directoryStructure);

        // 使用AI分析技术栈
        try {
            return aiService.analyzeTechStack(fileList, null);
        } catch (Exception e) {
            log.warn("技术栈分析失败，使用默认推断", e);
            return inferTechStackSimple(directoryStructure);
        }
    }

    /**
     * 推断主要编程语言
     */
    private String inferPrimaryLanguage(String directoryStructure) {
        if (directoryStructure.contains(".java")) {
            return "Java";
        } else if (directoryStructure.contains(".js") || directoryStructure.contains(".ts")) {
            return "JavaScript/TypeScript";
        } else if (directoryStructure.contains(".py")) {
            return "Python";
        } else if (directoryStructure.contains(".go")) {
            return "Go";
        } else if (directoryStructure.contains(".rs")) {
            return "Rust";
        } else if (directoryStructure.contains(".cs")) {
            return "C#";
        }
        return "Unknown";
    }

    /**
     * 提取主要文件
     */
    private List<String> extractMainFiles(String directoryStructure) {
        List<String> files = new ArrayList<>();

        // 分割成行
        String[] lines = directoryStructure.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            // 提取重要文件
            if (trimmed.contains("README") ||
                trimmed.contains("LICENSE") ||
                trimmed.contains("CHANGELOG") ||
                trimmed.contains("package.json") ||
                trimmed.contains("pom.xml") ||
                trimmed.contains("build.gradle") ||
                trimmed.contains("Dockerfile")) {

                files.add(trimmed);
            }
        }

        return files;
    }

    /**
     * 提取文件列表
     */
    private List<String> extractFileList(String directoryStructure) {
        List<String> files = new ArrayList<>();

        String[] lines = directoryStructure.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.endsWith("/")) {
                files.add(trimmed);
            }
        }

        return files;
    }

    /**
     * 简单推断技术栈
     */
    private List<String> inferTechStackSimple(String directoryStructure) {
        List<String> techStack = new ArrayList<>();

        if (directoryStructure.contains(".java")) {
            techStack.add("Java");
            if (directoryStructure.contains("pom.xml")) {
                techStack.add("Maven");
            }
            if (directoryStructure.contains("build.gradle")) {
                techStack.add("Gradle");
            }
            if (directoryStructure.contains("spring")) {
                techStack.add("Spring Boot");
            }
        }

        if (directoryStructure.contains("package.json")) {
            techStack.add("Node.js");
            if (directoryStructure.contains("react")) {
                techStack.add("React");
            }
            if (directoryStructure.contains("vue")) {
                techStack.add("Vue.js");
            }
        }

        if (directoryStructure.contains("requirements.txt")) {
            techStack.add("Python");
        }

        if (directoryStructure.contains("Dockerfile")) {
            techStack.add("Docker");
        }

        return techStack;
    }

    /**
     * README后处理
     */
    private String postProcess(String readme) {
        // 去除可能的代码块标记
        readme = readme.replace("```markdown", "").replace("```", "");

        // 确保没有多余的空行
        readme = readme.replaceAll("\n{3,}", "\n\n");

        // 去除首尾空白
        readme = readme.trim();

        return readme;
    }
}
