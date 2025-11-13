package ai.opendw.koalawiki.core.document.processors;

import ai.opendw.koalawiki.core.document.pipeline.AbstractDocumentProcessor;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingContext;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 目录优化处理器
 * 对大型仓库进行智能过滤，使用AI模型优化目录结构以减少token消耗
 *
 * 参考C#实现: DocumentsService.GetCatalogueSmartFilterAsync
 */
@Slf4j
@Component
public class CatalogOptimizationProcessor extends AbstractDocumentProcessor {

    private static final int FILE_COUNT_THRESHOLD = 800;
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 5000;

    // 正则表达式用于提取AI返回的优化内容
    private static final Pattern RESPONSE_FILE_PATTERN =
        Pattern.compile("<response_file>(.*?)</response_file>", Pattern.DOTALL);
    private static final Pattern JSON_PATTERN =
        Pattern.compile("```json(.*?)```", Pattern.DOTALL);

    public CatalogOptimizationProcessor() {
        super("CatalogOptimizationProcessor", 25);
    }

    @Override
    public boolean canProcess(DocumentProcessingContext context) {
        // 只在需要目录优化时处理
        if (context.getDocumentType() != DocumentProcessingContext.DocumentType.DIRECTORY) {
            return false;
        }

        // 检查是否启用智能过滤
        Boolean enableSmartFilter = context.getParameter("enableSmartFilter", Boolean.class)
            .orElse(true);

        if (!enableSmartFilter) {
            return false;
        }

        // 检查文件数量是否超过阈值
        Integer fileCount = context.getParameter("fileCount", Integer.class)
            .orElse(0);

        return fileCount >= FILE_COUNT_THRESHOLD;
    }

    @Override
    protected DocumentProcessingResult doProcess(DocumentProcessingContext context) {
        log.info("开始优化目录结构: {}", context.getDocumentPath());

        DocumentProcessingResult result = DocumentProcessingResult.success(getName());

        try {
            // 获取原始目录结构
            String originalCatalogue = context.getParameter("catalogue", String.class)
                .orElse(null);

            if (originalCatalogue == null || originalCatalogue.isEmpty()) {
                return DocumentProcessingResult.failure(getName(),
                    "目录结构为空，无法进行优化");
            }

            // 获取README内容（用于AI理解项目结构）
            String readme = context.getParameter("readme", String.class)
                .orElse("");

            // 调用AI优化目录结构
            String optimizedCatalogue = optimizeCatalogueWithAI(
                originalCatalogue,
                readme,
                context
            );

            if (optimizedCatalogue == null || optimizedCatalogue.isEmpty()) {
                log.warn("目录优化失败，使用原始目录结构");
                optimizedCatalogue = originalCatalogue;
            }

            // 将优化后的目录结构保存到共享状态
            context.putSharedState("optimizedCatalogue", optimizedCatalogue);
            context.putSharedState("catalogueOptimized", true);

            // 更新指标
            int originalSize = originalCatalogue.length();
            int optimizedSize = optimizedCatalogue.length();
            int reduction = originalSize - optimizedSize;
            double reductionPercent = (double) reduction / originalSize * 100;

            result.addOutput("originalSize", originalSize);
            result.addOutput("optimizedSize", optimizedSize);
            result.addOutput("reduction", reduction);
            result.addOutput("reductionPercent", String.format("%.2f%%", reductionPercent));

            result.setMessage(String.format("目录优化完成，减少 %d 字符 (%.2f%%)",
                reduction, reductionPercent));

            result.getMetrics()
                .setDocumentsProcessed(1)
                .setBytesProcessed(optimizedSize);

            log.info("目录优化完成: 原始大小={}, 优化后大小={}, 减少={} ({}%)",
                originalSize, optimizedSize, reduction, String.format("%.2f", reductionPercent));

        } catch (Exception e) {
            log.error("目录优化失败: {}", context.getDocumentPath(), e);
            return DocumentProcessingResult.failure(getName(),
                "目录优化异常: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 使用AI模型优化目录结构
     *
     * @param catalogue 原始目录结构
     * @param readme README内容
     * @param context 处理上下文
     * @return 优化后的目录结构
     */
    private String optimizeCatalogueWithAI(String catalogue, String readme,
                                           DocumentProcessingContext context) {
        // TODO: 集成AI服务（Phase 3任务）
        // 这里需要等待Phase 3的OpenAI集成完成
        // 目前返回null，表示不进行优化

        log.debug("AI目录优化功能将在Phase 3实现（OpenAI集成）");
        log.debug("catalogue length: {}, readme length: {}",
            catalogue.length(), readme.length());

        // 临时实现：简单的基于规则的优化
        return optimizeCatalogueByRules(catalogue);
    }

    /**
     * 基于规则的简单目录优化（临时实现）
     * 等待AI集成后将被替换
     */
    private String optimizeCatalogueByRules(String catalogue) {
        String[] lines = catalogue.split("\n");
        StringBuilder optimized = new StringBuilder();

        // 简单过滤：去除一些常见的不重要文件
        String[] ignoredExtensions = {".png", ".jpg", ".jpeg", ".gif", ".svg",
            ".ico", ".woff", ".ttf", ".eot", ".map"};
        String[] ignoredDirs = {"node_modules/", "target/", "build/", "dist/",
            ".git/", ".idea/", ".vscode/", "__pycache__/"};

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            boolean shouldIgnore = false;

            // 检查忽略的扩展名
            for (String ext : ignoredExtensions) {
                if (line.toLowerCase().endsWith(ext)) {
                    shouldIgnore = true;
                    break;
                }
            }

            // 检查忽略的目录
            if (!shouldIgnore) {
                for (String dir : ignoredDirs) {
                    if (line.contains(dir)) {
                        shouldIgnore = true;
                        break;
                    }
                }
            }

            if (!shouldIgnore) {
                optimized.append(line).append("\n");
            }
        }

        return optimized.toString();
    }

    /**
     * 从AI响应中提取优化后的目录结构
     * 支持多种格式：<response_file>、```json、纯文本
     */
    private String extractOptimizedContent(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return null;
        }

        // 尝试提取 <response_file> 标签内容
        Matcher responseMatcher = RESPONSE_FILE_PATTERN.matcher(aiResponse);
        if (responseMatcher.find()) {
            return responseMatcher.group(1).trim();
        }

        // 尝试提取 ```json 代码块
        Matcher jsonMatcher = JSON_PATTERN.matcher(aiResponse);
        if (jsonMatcher.find()) {
            return jsonMatcher.group(1).trim();
        }

        // 如果没有特殊标签，返回原始内容
        return aiResponse.trim();
    }

    /**
     * 带重试机制的AI调用
     */
    private String callAIWithRetry(String catalogue, String readme,
                                   DocumentProcessingContext context) throws Exception {
        Exception lastException = null;

        for (int retryCount = 0; retryCount < MAX_RETRIES; retryCount++) {
            try {
                // TODO: 实际的AI调用将在这里实现
                // 目前返回基于规则的优化
                return optimizeCatalogueByRules(catalogue);

            } catch (Exception e) {
                lastException = e;
                log.error("优化目录结构失败，重试第 {} 次", retryCount + 1, e);

                if (retryCount < MAX_RETRIES - 1) {
                    Thread.sleep(RETRY_DELAY_MS * (retryCount + 1));
                }
            }
        }

        throw new Exception("优化目录结构失败，已重试 " + MAX_RETRIES + " 次", lastException);
    }
}
