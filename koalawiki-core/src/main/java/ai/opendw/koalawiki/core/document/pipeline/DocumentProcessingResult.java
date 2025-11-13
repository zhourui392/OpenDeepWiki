package ai.opendw.koalawiki.core.document.pipeline;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

/**
 * 文档处理结果
 * 包含处理状态、输出和错误信息
 */
@Data
@Builder
@Accessors(chain = true)
public class DocumentProcessingResult {

    /**
     * 处理是否成功
     */
    private boolean success;

    /**
     * 处理器名称
     */
    private String processorName;

    /**
     * 处理消息
     */
    private String message;

    /**
     * 错误信息（如果失败）
     */
    private String error;

    /**
     * 异常信息（如果有异常）
     */
    private Exception exception;

    /**
     * 处理输出数据
     */
    @Builder.Default
    private Map<String, Object> output = new HashMap<>();

    /**
     * 处理产生的文件路径列表
     */
    @Builder.Default
    private List<String> generatedFiles = new ArrayList<>();

    /**
     * 处理修改的文件路径列表
     */
    @Builder.Default
    private List<String> modifiedFiles = new ArrayList<>();

    /**
     * 处理删除的文件路径列表
     */
    @Builder.Default
    private List<String> deletedFiles = new ArrayList<>();

    /**
     * 处理指标
     */
    @Builder.Default
    private ProcessingMetrics metrics = ProcessingMetrics.builder().build();

    /**
     * 是否应该继续处理链
     */
    @Builder.Default
    private boolean continueChain = true;

    /**
     * 处理开始时间
     */
    private Date startTime;

    /**
     * 处理结束时间
     */
    private Date endTime;

    /**
     * 创建成功结果
     */
    public static DocumentProcessingResult success(String processorName) {
        return DocumentProcessingResult.builder()
                .success(true)
                .processorName(processorName)
                .startTime(new Date())
                .endTime(new Date())
                .build();
    }

    /**
     * 创建成功结果（带消息）
     */
    public static DocumentProcessingResult success(String processorName, String message) {
        return DocumentProcessingResult.builder()
                .success(true)
                .processorName(processorName)
                .message(message)
                .startTime(new Date())
                .endTime(new Date())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static DocumentProcessingResult failure(String processorName, String error) {
        return DocumentProcessingResult.builder()
                .success(false)
                .processorName(processorName)
                .error(error)
                .continueChain(false)
                .startTime(new Date())
                .endTime(new Date())
                .build();
    }

    /**
     * 创建失败结果（带异常）
     */
    public static DocumentProcessingResult failure(String processorName, String error, Exception exception) {
        return DocumentProcessingResult.builder()
                .success(false)
                .processorName(processorName)
                .error(error)
                .exception(exception)
                .continueChain(false)
                .startTime(new Date())
                .endTime(new Date())
                .build();
    }

    /**
     * 创建跳过结果
     */
    public static DocumentProcessingResult skip(String processorName, String reason) {
        return DocumentProcessingResult.builder()
                .success(true)
                .processorName(processorName)
                .message("跳过: " + reason)
                .continueChain(true)
                .startTime(new Date())
                .endTime(new Date())
                .build();
    }

    /**
     * 添加输出数据
     */
    public DocumentProcessingResult addOutput(String key, Object value) {
        output.put(key, value);
        return this;
    }

    /**
     * 获取输出数据
     */
    public <T> Optional<T> getOutput(String key, Class<T> type) {
        Object value = output.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * 计算处理耗时（毫秒）
     */
    public long getProcessingTime() {
        if (startTime != null && endTime != null) {
            return endTime.getTime() - startTime.getTime();
        }
        return 0;
    }

    /**
     * 处理指标内部类
     */
    @Data
    @Builder
    @Accessors(chain = true)
    public static class ProcessingMetrics {
        /**
         * 处理的文档数量
         */
        @Builder.Default
        private int documentsProcessed = 0;

        /**
         * 处理的字节数
         */
        @Builder.Default
        private long bytesProcessed = 0;

        /**
         * 提取的元数据项数
         */
        @Builder.Default
        private int metadataExtracted = 0;

        /**
         * 生成的索引条目数
         */
        @Builder.Default
        private int indexEntriesGenerated = 0;

        /**
         * 自定义指标
         */
        @Builder.Default
        private Map<String, Object> customMetrics = new HashMap<>();

        /**
         * 添加自定义指标
         */
        public ProcessingMetrics addCustomMetric(String key, Object value) {
            customMetrics.put(key, value);
            return this;
        }
    }
}