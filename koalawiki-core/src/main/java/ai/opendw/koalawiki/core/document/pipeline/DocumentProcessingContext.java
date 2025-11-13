package ai.opendw.koalawiki.core.document.pipeline;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 文档处理上下文
 * 包含处理文档所需的所有信息和状态
 */
@Data
@Builder
@Accessors(chain = true)
public class DocumentProcessingContext {

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 文档路径（相对于仓库根目录）
     */
    private String documentPath;

    /**
     * 文档的本地文件系统路径
     */
    private Path localPath;

    /**
     * 文档类型
     */
    private DocumentType documentType;

    /**
     * 文档内容（可选，某些处理器可能需要）
     */
    private String content;

    /**
     * 文档元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 处理参数
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * 处理状态（在处理过程中共享）
     */
    @Builder.Default
    private Map<String, Object> sharedState = new HashMap<>();

    /**
     * 是否是增量处理
     */
    @Builder.Default
    private boolean incremental = false;

    /**
     * 处理模式
     */
    @Builder.Default
    private ProcessingMode mode = ProcessingMode.FULL;

    /**
     * 文档的Git提交版本
     */
    private String commitId;

    /**
     * 上次处理的提交版本（用于增量处理）
     */
    private String lastProcessedCommitId;

    /**
     * 获取元数据值
     */
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * 获取参数值
     */
    public <T> Optional<T> getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * 获取共享状态值
     */
    public <T> Optional<T> getSharedState(String key, Class<T> type) {
        Object value = sharedState.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * 设置共享状态值
     */
    public DocumentProcessingContext putSharedState(String key, Object value) {
        sharedState.put(key, value);
        return this;
    }

    /**
     * 文档类型枚举
     */
    public enum DocumentType {
        MARKDOWN("markdown", "md"),
        README("readme", "md"),
        DIRECTORY("directory", null),
        CONFIGURATION("configuration", "yaml,yml,json,toml"),
        SOURCE_CODE("source_code", "java,py,js,ts,go,rust"),
        IMAGE("image", "png,jpg,jpeg,gif,svg"),
        OTHER("other", null);

        private final String name;
        private final String extensions;

        DocumentType(String name, String extensions) {
            this.name = name;
            this.extensions = extensions;
        }

        /**
         * 根据文件扩展名判断文档类型
         */
        public static DocumentType fromFileName(String fileName) {
            if (fileName == null) {
                return OTHER;
            }

            String lowerName = fileName.toLowerCase();

            // 特殊处理README文件
            if (lowerName.startsWith("readme")) {
                return README;
            }

            // 获取文件扩展名
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot < 0) {
                return OTHER;
            }

            String extension = fileName.substring(lastDot + 1).toLowerCase();

            // 匹配文档类型
            for (DocumentType type : values()) {
                if (type.extensions != null) {
                    String[] exts = type.extensions.split(",");
                    for (String ext : exts) {
                        if (ext.equals(extension)) {
                            return type;
                        }
                    }
                }
            }

            return OTHER;
        }
    }

    /**
     * 处理模式枚举
     */
    public enum ProcessingMode {
        FULL("full"),           // 完整处理
        INCREMENTAL("incr"),    // 增量处理
        QUICK("quick"),         // 快速处理
        PREVIEW("preview");     // 预览模式

        private final String code;

        ProcessingMode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}