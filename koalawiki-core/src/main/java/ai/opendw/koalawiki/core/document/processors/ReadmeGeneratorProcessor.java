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
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * README生成器处理器
 * 为目录自动生成README.md文件
 */
@Slf4j
@Component
public class ReadmeGeneratorProcessor extends AbstractDocumentProcessor {

    private static final String README_FILENAME = "README.md";
    private static final Set<String> IGNORED_DIRS = new HashSet<>(Arrays.asList(
        ".git", ".svn", ".hg", "node_modules", "target", "build", "dist", ".idea", ".vscode"
    ));

    public ReadmeGeneratorProcessor() {
        super("ReadmeGeneratorProcessor", 30);
    }

    @Override
    public boolean canProcess(DocumentProcessingContext context) {
        // 只处理目录类型且需要生成README的情况
        if (context.getDocumentType() != DocumentProcessingContext.DocumentType.DIRECTORY) {
            return false;
        }

        // 检查参数是否要求生成README
        Boolean generateReadme = context.getParameter("generateReadme", Boolean.class)
            .orElse(false);

        return generateReadme;
    }

    @Override
    protected DocumentProcessingResult doProcess(DocumentProcessingContext context) {
        log.info("生成README文档: {}", context.getDocumentPath());

        DocumentProcessingResult result = DocumentProcessingResult.success(getName());

        try {
            Path dirPath = context.getLocalPath();
            if (dirPath == null || !Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return DocumentProcessingResult.failure(getName(),
                    "目录不存在或无效: " + context.getDocumentPath());
            }

            // 检查是否已有README文件
            Path readmePath = dirPath.resolve(README_FILENAME);
            if (Files.exists(readmePath)) {
                Boolean overwrite = context.getParameter("overwriteReadme", Boolean.class)
                    .orElse(false);
                if (!overwrite) {
                    return DocumentProcessingResult.skip(getName(), "README文件已存在");
                }
            }

            // 获取目录信息
            DirectoryInfo dirInfo = analyzeDirectory(dirPath);

            // 生成README内容
            String readmeContent = generateReadmeContent(dirPath, dirInfo, context);

            // 写入README文件
            Files.write(readmePath, readmeContent.getBytes(StandardCharsets.UTF_8));

            result.getGeneratedFiles().add(readmePath.toString());
            result.setMessage("成功生成README文件: " + readmePath.getFileName());

            // 添加输出信息
            result.addOutput("readmePath", readmePath.toString());
            result.addOutput("fileCount", dirInfo.fileCount);
            result.addOutput("dirCount", dirInfo.dirCount);
            result.addOutput("totalSize", dirInfo.totalSize);

            // 更新指标
            result.getMetrics()
                .setDocumentsProcessed(1)
                .setBytesProcessed(readmeContent.getBytes(StandardCharsets.UTF_8).length);

        } catch (Exception e) {
            log.error("README生成失败: {}", context.getDocumentPath(), e);
            return DocumentProcessingResult.failure(getName(),
                "README生成异常: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 分析目录结构
     */
    private DirectoryInfo analyzeDirectory(Path dirPath) throws IOException {
        DirectoryInfo info = new DirectoryInfo();

        // 获取直接子项
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.forEach(path -> {
                String fileName = path.getFileName().toString();

                // 跳过隐藏文件和忽略的目录
                if (fileName.startsWith(".") || IGNORED_DIRS.contains(fileName)) {
                    return;
                }

                try {
                    if (Files.isDirectory(path)) {
                        info.directories.add(new DirectoryEntry(fileName, countFiles(path)));
                        info.dirCount++;
                    } else {
                        long size = Files.size(path);
                        info.files.add(new FileEntry(fileName, size));
                        info.fileCount++;
                        info.totalSize += size;

                        // 分类文件
                        String ext = getFileExtension(fileName);
                        info.filesByType.computeIfAbsent(ext, k -> new ArrayList<>()).add(fileName);
                    }
                } catch (IOException e) {
                    log.warn("无法处理路径: {}", path, e);
                }
            });
        }

        // 排序
        info.directories.sort(Comparator.comparing(d -> d.name.toLowerCase()));
        info.files.sort(Comparator.comparing(f -> f.name.toLowerCase()));

        return info;
    }

    /**
     * 统计目录中的文件数量
     */
    private int countFiles(Path dirPath) {
        try (Stream<Path> stream = Files.walk(dirPath)) {
            return (int) stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "other";
    }

    /**
     * 生成README内容
     */
    private String generateReadmeContent(Path dirPath, DirectoryInfo dirInfo,
                                          DocumentProcessingContext context) {
        StringBuilder content = new StringBuilder();

        // 标题
        String title = context.getParameter("readmeTitle", String.class)
            .orElse(dirPath.getFileName().toString());
        content.append("# ").append(title).append("\n\n");

        // 描述
        String description = context.getParameter("readmeDescription", String.class)
            .orElse(null);
        if (description != null) {
            content.append(description).append("\n\n");
        }

        // 目录结构
        content.append("## 目录结构\n\n");

        if (!dirInfo.directories.isEmpty()) {
            content.append("### 子目录\n\n");
            for (DirectoryEntry dir : dirInfo.directories) {
                content.append("- **").append(dir.name).append("/** - ")
                       .append(dir.fileCount).append(" 个文件\n");
            }
            content.append("\n");
        }

        if (!dirInfo.files.isEmpty()) {
            content.append("### 文件\n\n");

            // 按类型分组显示
            Map<String, List<String>> grouped = groupFilesByCategory(dirInfo.filesByType);
            for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
                content.append("#### ").append(entry.getKey()).append("\n\n");
                for (String file : entry.getValue()) {
                    content.append("- ").append(file).append("\n");
                }
                content.append("\n");
            }
        }

        // 统计信息
        content.append("## 统计信息\n\n");
        content.append("- 总文件数: ").append(dirInfo.fileCount).append("\n");
        content.append("- 子目录数: ").append(dirInfo.dirCount).append("\n");
        content.append("- 总大小: ").append(formatSize(dirInfo.totalSize)).append("\n");

        // 生成时间
        content.append("\n---\n");
        content.append("*生成时间: ").append(new Date()).append("*\n");

        return content.toString();
    }

    /**
     * 按类别分组文件
     */
    private Map<String, List<String>> groupFilesByCategory(Map<String, List<String>> filesByType) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();

        // 定义类别映射
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("md", "文档文件");
        categoryMap.put("markdown", "文档文件");
        categoryMap.put("java", "源代码");
        categoryMap.put("py", "源代码");
        categoryMap.put("js", "源代码");
        categoryMap.put("ts", "源代码");
        categoryMap.put("go", "源代码");
        categoryMap.put("yaml", "配置文件");
        categoryMap.put("yml", "配置文件");
        categoryMap.put("json", "配置文件");
        categoryMap.put("xml", "配置文件");
        categoryMap.put("properties", "配置文件");

        // 分组文件
        for (Map.Entry<String, List<String>> entry : filesByType.entrySet()) {
            String category = categoryMap.getOrDefault(entry.getKey(), "其他文件");
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).addAll(entry.getValue());
        }

        return grouped;
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 目录信息
     */
    private static class DirectoryInfo {
        List<DirectoryEntry> directories = new ArrayList<>();
        List<FileEntry> files = new ArrayList<>();
        Map<String, List<String>> filesByType = new HashMap<>();
        int fileCount = 0;
        int dirCount = 0;
        long totalSize = 0;
    }

    /**
     * 目录条目
     */
    private static class DirectoryEntry {
        final String name;
        final int fileCount;

        DirectoryEntry(String name, int fileCount) {
            this.name = name;
            this.fileCount = fileCount;
        }
    }

    /**
     * 文件条目
     */
    private static class FileEntry {
        final String name;
        final long size;

        FileEntry(String name, long size) {
            this.name = name;
            this.size = size;
        }
    }
}