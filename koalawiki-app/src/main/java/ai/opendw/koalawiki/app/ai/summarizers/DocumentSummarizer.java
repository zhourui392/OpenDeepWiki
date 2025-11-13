package ai.opendw.koalawiki.app.ai.summarizers;

import ai.opendw.koalawiki.app.ai.IAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档摘要生成器
 * 为文档生成简洁的摘要
 */
@Slf4j
@Component
public class DocumentSummarizer {

    @Autowired
    private IAIService aiService;

    /**
     * 默认摘要长度
     */
    private static final int DEFAULT_SUMMARY_LENGTH = 200;

    /**
     * 生成文档摘要
     *
     * @param content 文档内容
     * @return 摘要
     */
    public String summarize(String content) {
        return summarize(content, DEFAULT_SUMMARY_LENGTH);
    }

    /**
     * 生成指定长度的文档摘要
     *
     * @param content 文档内容
     * @param maxLength 最大长度
     * @return 摘要
     */
    public String summarize(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        log.info("生成文档摘要，内容长度: {}, 目标长度: {}", content.length(), maxLength);

        // 如果内容本身就很短，直接返回
        if (content.length() <= maxLength) {
            return content.trim();
        }

        try {
            // 使用AI生成摘要
            String summary = aiService.summarizeDocument(content, maxLength);

            // 确保摘要不超过指定长度
            if (summary.length() > maxLength) {
                summary = summary.substring(0, maxLength) + "...";
            }

            log.info("摘要生成成功，长度: {}", summary.length());
            return summary.trim();
        } catch (Exception e) {
            log.error("摘要生成失败，使用简单截取", e);
            return simpleExtract(content, maxLength);
        }
    }

    /**
     * 批量生成摘要
     *
     * @param documents 文档列表
     * @return 摘要列表
     */
    public List<String> summarizeBatch(List<String> documents) {
        return summarizeBatch(documents, DEFAULT_SUMMARY_LENGTH);
    }

    /**
     * 批量生成指定长度的摘要
     *
     * @param documents 文档列表
     * @param maxLength 最大长度
     * @return 摘要列表
     */
    public List<String> summarizeBatch(List<String> documents, int maxLength) {
        log.info("批量生成摘要，文档数: {}", documents.size());

        List<String> summaries = new java.util.ArrayList<>();
        for (String doc : documents) {
            try {
                String summary = summarize(doc, maxLength);
                summaries.add(summary);
            } catch (Exception e) {
                log.error("文档摘要生成失败", e);
                summaries.add(simpleExtract(doc, maxLength));
            }
        }

        return summaries;
    }

    /**
     * 生成文档摘要并提取关键词
     */
    public DocumentSummaryResult summarizeWithKeywords(String content) {
        String summary = summarize(content);
        List<String> keywords = extractKeywords(content);

        return DocumentSummaryResult.builder()
                .summary(summary)
                .keywords(keywords)
                .originalLength(content.length())
                .summaryLength(summary.length())
                .build();
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String content) {
        try {
            return aiService.generateTags(content, 5);
        } catch (Exception e) {
            log.error("关键词提取失败", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 简单的文本提取（降级方案）
     * 提取第一段或前N个字符
     */
    private String simpleExtract(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 去除Markdown标记
        String cleaned = content
                .replaceAll("^#{1,6}\\s+", "")  // 移除标题标记
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")  // 移除粗体
                .replaceAll("\\*(.+?)\\*", "$1")  // 移除斜体
                .replaceAll("```[\\s\\S]*?```", "")  // 移除代码块
                .replaceAll("`(.+?)`", "$1")  // 移除行内代码
                .replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1")  // 移除链接
                .trim();

        // 尝试提取第一段
        String[] paragraphs = cleaned.split("\n\n");
        if (paragraphs.length > 0 && !paragraphs[0].isEmpty()) {
            String firstParagraph = paragraphs[0].trim();
            if (firstParagraph.length() <= maxLength) {
                return firstParagraph;
            }
        }

        // 截取前N个字符
        if (cleaned.length() > maxLength) {
            // 尝试在句号、问号、感叹号处截断
            int lastPunctuation = Math.max(
                    Math.max(cleaned.lastIndexOf('。', maxLength),
                            cleaned.lastIndexOf('？', maxLength)),
                    cleaned.lastIndexOf('！', maxLength)
            );

            if (lastPunctuation > maxLength / 2) {
                return cleaned.substring(0, lastPunctuation + 1);
            }

            return cleaned.substring(0, maxLength) + "...";
        }

        return cleaned;
    }

    /**
     * 文档摘要结果
     */
    @lombok.Data
    @lombok.Builder
    public static class DocumentSummaryResult {
        private String summary;
        private List<String> keywords;
        private int originalLength;
        private int summaryLength;
        private double compressionRatio;

        public double getCompressionRatio() {
            if (originalLength == 0) {
                return 0;
            }
            return (double) summaryLength / originalLength;
        }
    }
}
