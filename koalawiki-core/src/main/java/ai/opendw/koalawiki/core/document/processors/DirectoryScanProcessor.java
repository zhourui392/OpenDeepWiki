package ai.opendw.koalawiki.core.document.processors;

import ai.opendw.koalawiki.core.document.pipeline.AbstractDocumentProcessor;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingContext;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * 目录扫描处理器
 * 扫描目录结构并收集文件信息
 */
@Slf4j
@Component
public class DirectoryScanProcessor extends AbstractDocumentProcessor {

    public DirectoryScanProcessor() {
        super("DirectoryScanProcessor", 10);  // 高优先级
    }

    @Override
    public boolean canProcess(DocumentProcessingContext context) {
        // 只处理目录类型的文档
        return context.getDocumentType() == DocumentProcessingContext.DocumentType.DIRECTORY;
    }

    @Override
    protected DocumentProcessingResult doProcess(DocumentProcessingContext context) {
        log.info("扫描目录: {}", context.getDocumentPath());

        DocumentProcessingResult result = DocumentProcessingResult.success(getName());

        try {
            Path dirPath = context.getLocalPath();
            if (dirPath == null || !Files.exists(dirPath)) {
                return DocumentProcessingResult.failure(getName(),
                    "目录不存在: " + context.getDocumentPath());
            }

            if (!Files.isDirectory(dirPath)) {
                return DocumentProcessingResult.failure(getName(),
                    "路径不是目录: " + context.getDocumentPath());
            }

            // 扫描目录
            DirectoryScanResult scanResult = scanDirectory(dirPath);

            // 将扫描结果添加到输出
            result.addOutput("fileCount", scanResult.fileCount);
            result.addOutput("directoryCount", scanResult.directoryCount);
            result.addOutput("totalSize", scanResult.totalSize);
            result.addOutput("markdownFiles", scanResult.markdownFileCount);
            result.addOutput("imageFiles", scanResult.imageFileCount);

            // 将扫描结果保存到共享状态，供后续处理器使用
            context.putSharedState("directoryScanResult", scanResult);

            result.setMessage(String.format("扫描完成: %d 个文件, %d 个目录, 总大小: %d 字节",
                scanResult.fileCount, scanResult.directoryCount, scanResult.totalSize));

            // 更新处理指标
            result.getMetrics()
                .setDocumentsProcessed(scanResult.fileCount)
                .setBytesProcessed(scanResult.totalSize);

        } catch (Exception e) {
            log.error("目录扫描失败: {}", context.getDocumentPath(), e);
            return DocumentProcessingResult.failure(getName(),
                "目录扫描异常: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 扫描目录并收集统计信息
     */
    private DirectoryScanResult scanDirectory(Path dirPath) throws IOException {
        DirectoryScanResult result = new DirectoryScanResult();

        try (Stream<Path> pathStream = Files.walk(dirPath)) {
            pathStream.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        result.directoryCount++;
                    } else {
                        result.fileCount++;
                        result.totalSize += Files.size(path);

                        String fileName = path.getFileName().toString().toLowerCase();
                        if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
                            result.markdownFileCount++;
                        } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
                                   fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
                                   fileName.endsWith(".svg")) {
                            result.imageFileCount++;
                        }
                    }
                } catch (IOException e) {
                    log.warn("无法处理路径: {}", path, e);
                }
            });
        }

        return result;
    }

    /**
     * 目录扫描结果
     */
    public static class DirectoryScanResult {
        public int fileCount = 0;
        public int directoryCount = 0;
        public long totalSize = 0;
        public int markdownFileCount = 0;
        public int imageFileCount = 0;
    }
}