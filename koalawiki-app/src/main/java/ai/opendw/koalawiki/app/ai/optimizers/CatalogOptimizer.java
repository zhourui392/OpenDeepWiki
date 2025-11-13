package ai.opendw.koalawiki.app.ai.optimizers;

import ai.opendw.koalawiki.app.ai.IAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 目录优化器
 * 智能过滤大型仓库的文件目录，保留核心文档结构
 */
@Slf4j
@Component
public class CatalogOptimizer {

    @Autowired
    private IAIService aiService;

    /**
     * 文件数量阈值，超过此数量触发优化
     */
    private static final int FILE_COUNT_THRESHOLD = 800;

    /**
     * 目标文件数量
     */
    private static final int TARGET_FILE_COUNT = 100;

    /**
     * 优化目录
     *
     * @param catalogData 原始目录数据
     * @return 优化后的目录数据
     */
    public String optimize(String catalogData) {
        // 计算文件数量
        int fileCount = countFiles(catalogData);
        log.info("原始文件数量: {}", fileCount);

        // 判断是否需要优化
        if (fileCount <= FILE_COUNT_THRESHOLD) {
            log.info("文件数量未超过阈值，无需优化");
            return catalogData;
        }

        log.info("文件数量超过阈值，开始智能优化");

        // 先使用规则过滤
        String ruleFiltered = applyRuleBasedFilter(catalogData);
        int afterRuleCount = countFiles(ruleFiltered);
        log.info("规则过滤后文件数量: {}", afterRuleCount);

        // 如果规则过滤后仍然很多，使用AI优化
        if (afterRuleCount > TARGET_FILE_COUNT) {
            log.info("使用AI进行进一步优化");
            return aiService.optimizeCatalog(ruleFiltered, TARGET_FILE_COUNT);
        }

        return ruleFiltered;
    }

    /**
     * 基于规则的过滤
     */
    private String applyRuleBasedFilter(String catalogData) {
        List<String> lines = Arrays.asList(catalogData.split("\n"));
        List<String> filteredLines = new ArrayList<>();

        for (String line : lines) {
            if (shouldKeepLine(line)) {
                filteredLines.add(line);
            }
        }

        return String.join("\n", filteredLines);
    }

    /**
     * 判断是否保留某一行
     */
    private boolean shouldKeepLine(String line) {
        String lower = line.toLowerCase();

        // 排除构建产物
        if (lower.contains("/target/") ||
            lower.contains("/build/") ||
            lower.contains("/dist/") ||
            lower.contains("/out/") ||
            lower.contains("/.next/") ||
            lower.contains("/coverage/")) {
            return false;
        }

        // 排除依赖目录
        if (lower.contains("/node_modules/") ||
            lower.contains("/vendor/") ||
            lower.contains("/.gradle/") ||
            lower.contains("/.m2/")) {
            return false;
        }

        // 排除IDE文件
        if (lower.contains("/.idea/") ||
            lower.contains("/.vscode/") ||
            lower.contains("/.eclipse/") ||
            lower.contains("/.settings/")) {
            return false;
        }

        // 排除临时文件和缓存
        if (lower.contains("/.cache/") ||
            lower.contains("/tmp/") ||
            lower.contains("/temp/") ||
            lower.contains("/__pycache__/") ||
            lower.contains("*.log") ||
            lower.contains("*.tmp")) {
            return false;
        }

        // 排除版本控制文件（除了重要的）
        if (lower.contains("/.git/") && !lower.contains(".gitignore") && !lower.contains(".github")) {
            return false;
        }

        // 保留重要文件
        if (lower.contains("readme") ||
            lower.contains("license") ||
            lower.contains("changelog") ||
            lower.contains("contributing") ||
            lower.contains("package.json") ||
            lower.contains("pom.xml") ||
            lower.contains("build.gradle") ||
            lower.contains("dockerfile") ||
            lower.contains("docker-compose") ||
            lower.contains("makefile")) {
            return true;
        }

        // 保留源代码目录
        if (lower.contains("/src/") ||
            lower.contains("/lib/") ||
            lower.contains("/api/") ||
            lower.contains("/core/") ||
            lower.contains("/service/")) {
            return true;
        }

        // 保留配置文件
        if (lower.endsWith(".yml") ||
            lower.endsWith(".yaml") ||
            lower.endsWith(".json") ||
            lower.endsWith(".xml") ||
            lower.endsWith(".properties") ||
            lower.endsWith(".conf") ||
            lower.endsWith(".config")) {
            return true;
        }

        // 保留文档文件
        if (lower.endsWith(".md") ||
            lower.endsWith(".txt") ||
            lower.endsWith(".rst")) {
            return true;
        }

        // 默认保留
        return true;
    }

    /**
     * 计算文件数量
     */
    private int countFiles(String catalogData) {
        if (catalogData == null || catalogData.isEmpty()) {
            return 0;
        }

        return (int) Arrays.stream(catalogData.split("\n"))
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().endsWith("/")) // 排除目录
                .count();
    }

    /**
     * 获取优化统计信息
     */
    public OptimizationResult optimizeWithStats(String catalogData) {
        int originalCount = countFiles(catalogData);
        String optimized = optimize(catalogData);
        int optimizedCount = countFiles(optimized);

        return OptimizationResult.builder()
                .originalCatalog(catalogData)
                .optimizedCatalog(optimized)
                .originalFileCount(originalCount)
                .optimizedFileCount(optimizedCount)
                .reductionPercentage((originalCount - optimizedCount) * 100.0 / originalCount)
                .build();
    }

    /**
     * 优化结果
     */
    @lombok.Data
    @lombok.Builder
    public static class OptimizationResult {
        private String originalCatalog;
        private String optimizedCatalog;
        private int originalFileCount;
        private int optimizedFileCount;
        private double reductionPercentage;
    }
}
