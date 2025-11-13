package ai.opendw.koalawiki.core.document.processors;

import ai.opendw.koalawiki.core.document.pipeline.AbstractDocumentProcessor;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingContext;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

/**
 * 文档索引处理器
 * 创建文档索引，提取关键词和元数据
 */
@Slf4j
@Component
public class DocumentIndexProcessor extends AbstractDocumentProcessor {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the", "is", "at", "which", "on", "and", "a", "an", "as", "are", "was", "were",
        "的", "是", "在", "和", "与", "了", "吗", "吧", "啊", "呢", "嗯"
    ));

    public DocumentIndexProcessor() {
        super("DocumentIndexProcessor", 40);
    }

    @Override
    public boolean canProcess(DocumentProcessingContext context) {
        // 处理Markdown、README和源代码文件
        DocumentProcessingContext.DocumentType type = context.getDocumentType();
        return type == DocumentProcessingContext.DocumentType.MARKDOWN ||
               type == DocumentProcessingContext.DocumentType.README ||
               type == DocumentProcessingContext.DocumentType.SOURCE_CODE;
    }

    @Override
    protected DocumentProcessingResult doProcess(DocumentProcessingContext context) {
        log.info("创建文档索引: {}", context.getDocumentPath());

        DocumentProcessingResult result = DocumentProcessingResult.success(getName());

        try {
            // 创建索引条目
            IndexEntry indexEntry = new IndexEntry();
            indexEntry.setDocumentPath(context.getDocumentPath());
            indexEntry.setDocumentType(context.getDocumentType().name());
            indexEntry.setWarehouseId(context.getWarehouseId());
            indexEntry.setCommitId(context.getCommitId());
            indexEntry.setIndexTime(new Date());

            // 从共享状态获取已解析的信息
            if (context.getSharedState("markdownParsed", Boolean.class).orElse(false)) {
                // 使用已解析的Markdown信息
                extractFromParsedMarkdown(context, indexEntry);
            } else {
                // 直接从内容提取
                extractFromContent(context, indexEntry);
            }

            // 提取文件路径信息
            extractPathInfo(context, indexEntry);

            // 计算相关性评分
            calculateRelevanceScore(indexEntry);

            // 保存索引条目
            result.addOutput("indexEntry", indexEntry);
            result.addOutput("keywords", indexEntry.getKeywords());
            result.addOutput("tags", indexEntry.getTags());

            // 将索引信息添加到共享状态
            context.putSharedState("documentIndexed", true);
            context.putSharedState("indexEntry", indexEntry);

            result.setMessage(String.format("索引创建完成: %d 个关键词, %d 个标签",
                indexEntry.getKeywords().size(), indexEntry.getTags().size()));

            // 更新指标
            result.getMetrics()
                .setIndexEntriesGenerated(1)
                .setMetadataExtracted(indexEntry.getMetadata().size())
                .addCustomMetric("keywordCount", indexEntry.getKeywords().size())
                .addCustomMetric("tagCount", indexEntry.getTags().size());

        } catch (Exception e) {
            log.error("文档索引创建失败: {}", context.getDocumentPath(), e);
            return DocumentProcessingResult.failure(getName(),
                "索引创建异常: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 从已解析的Markdown提取索引信息
     */
    @SuppressWarnings("unchecked")
    private void extractFromParsedMarkdown(DocumentProcessingContext context, IndexEntry indexEntry) {
        // 获取标题
        List<MarkdownProcessor.Heading> headings =
            (List<MarkdownProcessor.Heading>) context.getSharedState("documentHeadings", List.class)
                .orElse(new ArrayList<>());

        if (!headings.isEmpty()) {
            // 使用第一个H1作为标题
            for (MarkdownProcessor.Heading heading : headings) {
                if (heading.level == 1) {
                    indexEntry.setTitle(heading.text);
                    break;
                }
            }

            // 提取所有标题作为关键词
            for (MarkdownProcessor.Heading heading : headings) {
                extractKeywordsFromText(heading.text, indexEntry.getKeywords());
            }
        }

        // 从元数据中提取标签
        Map<String, Object> metadata = context.getMetadata();
        if (metadata.containsKey("tags")) {
            Object tags = metadata.get("tags");
            if (tags instanceof String) {
                String[] tagArray = ((String) tags).split("[,;\\s]+");
                indexEntry.getTags().addAll(Arrays.asList(tagArray));
            }
        }

        // 复制其他元数据
        indexEntry.getMetadata().putAll(metadata);
    }

    /**
     * 从内容直接提取索引信息
     */
    private void extractFromContent(DocumentProcessingContext context, IndexEntry indexEntry) {
        String content = context.getContent();
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        // 提取标题（假设第一行是标题）
        String[] lines = content.split("\n");
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            // 移除Markdown标题标记
            firstLine = firstLine.replaceAll("^#+\\s*", "");
            indexEntry.setTitle(firstLine);
        }

        // 提取关键词
        extractKeywordsFromText(content, indexEntry.getKeywords());

        // 提取摘要（前200个字符）
        String summary = content.replaceAll("\\s+", " ").trim();
        if (summary.length() > 200) {
            summary = summary.substring(0, 197) + "...";
        }
        indexEntry.setSummary(summary);
    }

    /**
     * 从文本提取关键词
     */
    private void extractKeywordsFromText(String text, Set<String> keywords) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // 简单的关键词提取：分词并过滤
        String[] words = text.toLowerCase()
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5\\s]", " ")  // 保留字母、数字、中文
            .split("\\s+");

        for (String word : words) {
            // 过滤停用词和短词
            if (word.length() > 2 && !STOP_WORDS.contains(word)) {
                keywords.add(word);

                // 限制关键词数量
                if (keywords.size() >= 50) {
                    break;
                }
            }
        }
    }

    /**
     * 提取路径信息
     */
    private void extractPathInfo(DocumentProcessingContext context, IndexEntry indexEntry) {
        String path = context.getDocumentPath();
        if (path == null) {
            return;
        }

        // 提取文件名
        int lastSlash = path.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        indexEntry.setFileName(fileName);

        // 提取路径层级
        String[] parts = path.split("/");
        indexEntry.setPathDepth(parts.length);

        // 提取父目录
        if (lastSlash > 0) {
            indexEntry.setParentPath(path.substring(0, lastSlash));
        }

        // 将路径组件作为标签
        for (String part : parts) {
            if (!part.isEmpty() && !part.equals(fileName)) {
                indexEntry.getTags().add("path:" + part);
            }
        }
    }

    /**
     * 计算相关性评分
     */
    private void calculateRelevanceScore(IndexEntry indexEntry) {
        double score = 0.0;

        // 基于文档类型的基础分
        switch (indexEntry.getDocumentType()) {
            case "README":
                score += 30;
                break;
            case "MARKDOWN":
                score += 20;
                break;
            case "SOURCE_CODE":
                score += 10;
                break;
            default:
                score += 5;
        }

        // 基于路径深度（越浅越重要）
        score += Math.max(0, 20 - indexEntry.getPathDepth() * 2);

        // 基于关键词数量
        score += Math.min(indexEntry.getKeywords().size(), 20);

        // 基于标签数量
        score += Math.min(indexEntry.getTags().size() * 2, 10);

        // 基于是否有标题
        if (indexEntry.getTitle() != null && !indexEntry.getTitle().isEmpty()) {
            score += 10;
        }

        indexEntry.setRelevanceScore(score);
    }

    /**
     * 索引条目
     */
    @Data
    public static class IndexEntry {
        private String documentPath;
        private String fileName;
        private String parentPath;
        private String documentType;
        private String warehouseId;
        private String commitId;
        private String title;
        private String summary;
        private Set<String> keywords = new HashSet<>();
        private Set<String> tags = new HashSet<>();
        private Map<String, Object> metadata = new HashMap<>();
        private int pathDepth;
        private double relevanceScore;
        private Date indexTime;
    }
}