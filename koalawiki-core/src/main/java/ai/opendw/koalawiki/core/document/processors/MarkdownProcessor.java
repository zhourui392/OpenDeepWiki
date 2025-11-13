package ai.opendw.koalawiki.core.document.processors;

import ai.opendw.koalawiki.core.document.pipeline.AbstractDocumentProcessor;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingContext;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown文档处理器
 * 解析Markdown文档，提取元数据、标题、链接等信息
 */
@Slf4j
@Component
public class MarkdownProcessor extends AbstractDocumentProcessor {

    // 正则表达式模式
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^---\\s*\n(.*?)\n---\\s*\n", Pattern.DOTALL);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*?)\\]\\(([^)]+)\\)");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)?\\s*\n(.*?)\n```", Pattern.DOTALL);

    public MarkdownProcessor() {
        super("MarkdownProcessor", 20);
    }

    @Override
    public boolean canProcess(DocumentProcessingContext context) {
        // 处理Markdown和README文档
        DocumentProcessingContext.DocumentType type = context.getDocumentType();
        return type == DocumentProcessingContext.DocumentType.MARKDOWN ||
               type == DocumentProcessingContext.DocumentType.README;
    }

    @Override
    protected DocumentProcessingResult doProcess(DocumentProcessingContext context) {
        log.info("处理Markdown文档: {}", context.getDocumentPath());

        DocumentProcessingResult result = DocumentProcessingResult.success(getName());

        try {
            // 读取文档内容
            String content = readContent(context);
            if (content == null || content.trim().isEmpty()) {
                return DocumentProcessingResult.skip(getName(), "文档内容为空");
            }

            // 解析Front Matter
            Map<String, String> frontMatter = parseFrontMatter(content);
            if (!frontMatter.isEmpty()) {
                result.addOutput("frontMatter", frontMatter);
                context.getMetadata().putAll(frontMatter);
            }

            // 提取标题结构
            List<Heading> headings = extractHeadings(content);
            result.addOutput("headings", headings);
            result.addOutput("headingCount", headings.size());

            // 提取链接
            List<Link> links = extractLinks(content);
            result.addOutput("links", links);
            result.addOutput("linkCount", links.size());

            // 提取图片
            List<Image> images = extractImages(content);
            result.addOutput("images", images);
            result.addOutput("imageCount", images.size());

            // 提取代码块
            List<CodeBlock> codeBlocks = extractCodeBlocks(content);
            result.addOutput("codeBlocks", codeBlocks);
            result.addOutput("codeBlockCount", codeBlocks.size());

            // 计算文档统计信息
            DocumentStats stats = calculateStats(content);
            result.addOutput("stats", stats);

            // 将解析结果保存到共享状态
            context.putSharedState("markdownParsed", true);
            context.putSharedState("documentHeadings", headings);
            context.putSharedState("documentStats", stats);

            result.setMessage(String.format("解析完成: %d 个标题, %d 个链接, %d 个图片, %d 个代码块",
                headings.size(), links.size(), images.size(), codeBlocks.size()));

            // 更新处理指标
            result.getMetrics()
                .setDocumentsProcessed(1)
                .setBytesProcessed(content.getBytes(StandardCharsets.UTF_8).length)
                .setMetadataExtracted(frontMatter.size());

        } catch (Exception e) {
            log.error("Markdown文档处理失败: {}", context.getDocumentPath(), e);
            return DocumentProcessingResult.failure(getName(),
                "文档处理异常: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 读取文档内容
     */
    private String readContent(DocumentProcessingContext context) throws IOException {
        if (context.getContent() != null) {
            return context.getContent();
        }

        Path filePath = context.getLocalPath();
        if (filePath == null || !Files.exists(filePath)) {
            return null;
        }

        return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
    }

    /**
     * 解析Front Matter
     */
    private Map<String, String> parseFrontMatter(String content) {
        Map<String, String> frontMatter = new HashMap<>();

        Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            String yaml = matcher.group(1);
            String[] lines = yaml.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                int colonIndex = trimmed.indexOf(':');
                if (colonIndex > 0) {
                    String key = trimmed.substring(0, colonIndex).trim();
                    String value = trimmed.substring(colonIndex + 1).trim();
                    // 移除引号
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    } else if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    frontMatter.put(key, value);
                }
            }
        }

        return frontMatter;
    }

    /**
     * 提取标题
     */
    private List<Heading> extractHeadings(String content) {
        List<Heading> headings = new ArrayList<>();

        Matcher matcher = HEADING_PATTERN.matcher(content);
        while (matcher.find()) {
            int level = matcher.group(1).length();
            String text = matcher.group(2).trim();
            headings.add(new Heading(level, text));
        }

        return headings;
    }

    /**
     * 提取链接
     */
    private List<Link> extractLinks(String content) {
        List<Link> links = new ArrayList<>();

        Matcher matcher = LINK_PATTERN.matcher(content);
        while (matcher.find()) {
            String text = matcher.group(1);
            String url = matcher.group(2);
            links.add(new Link(text, url));
        }

        return links;
    }

    /**
     * 提取图片
     */
    private List<Image> extractImages(String content) {
        List<Image> images = new ArrayList<>();

        Matcher matcher = IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);
            images.add(new Image(alt, url));
        }

        return images;
    }

    /**
     * 提取代码块
     */
    private List<CodeBlock> extractCodeBlocks(String content) {
        List<CodeBlock> codeBlocks = new ArrayList<>();

        Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            codeBlocks.add(new CodeBlock(language, code));
        }

        return codeBlocks;
    }

    /**
     * 计算文档统计信息
     */
    private DocumentStats calculateStats(String content) {
        DocumentStats stats = new DocumentStats();

        // 计算行数
        stats.lineCount = content.split("\n").length;

        // 计算字符数
        stats.characterCount = content.length();

        // 计算单词数（简单估算）
        String textContent = content.replaceAll("```.*?```", "")  // 移除代码块
                                    .replaceAll("!?\\[[^\\]]*?\\]\\([^)]+\\)", "")  // 移除链接和图片
                                    .replaceAll("#+ ", "")  // 移除标题标记
                                    .replaceAll("[*_~`]", "");  // 移除格式标记

        String[] words = textContent.split("\\s+");
        stats.wordCount = words.length;

        return stats;
    }

    /**
     * 标题类
     */
    public static class Heading {
        public final int level;
        public final String text;

        public Heading(int level, String text) {
            this.level = level;
            this.text = text;
        }
    }

    /**
     * 链接类
     */
    public static class Link {
        public final String text;
        public final String url;

        public Link(String text, String url) {
            this.text = text;
            this.url = url;
        }
    }

    /**
     * 图片类
     */
    public static class Image {
        public final String alt;
        public final String url;

        public Image(String alt, String url) {
            this.alt = alt;
            this.url = url;
        }
    }

    /**
     * 代码块类
     */
    public static class CodeBlock {
        public final String language;
        public final String code;

        public CodeBlock(String language, String code) {
            this.language = language;
            this.code = code;
        }
    }

    /**
     * 文档统计信息
     */
    public static class DocumentStats {
        public int lineCount;
        public int characterCount;
        public int wordCount;
    }
}