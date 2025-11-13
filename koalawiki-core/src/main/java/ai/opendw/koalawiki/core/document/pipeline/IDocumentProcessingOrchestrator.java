package ai.opendw.koalawiki.core.document.pipeline;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 文档处理编排器接口
 * 负责管理和协调文档处理器链
 */
public interface IDocumentProcessingOrchestrator {

    /**
     * 注册文档处理器
     *
     * @param processor 要注册的处理器
     */
    void registerProcessor(IDocumentProcessor processor);

    /**
     * 批量注册文档处理器
     *
     * @param processors 处理器列表
     */
    void registerProcessors(List<IDocumentProcessor> processors);

    /**
     * 移除文档处理器
     *
     * @param processorName 处理器名称
     */
    void removeProcessor(String processorName);

    /**
     * 获取所有已注册的处理器
     *
     * @return 处理器列表
     */
    List<IDocumentProcessor> getProcessors();

    /**
     * 同步处理文档
     *
     * @param context 文档处理上下文
     * @return 处理结果列表
     */
    List<DocumentProcessingResult> process(DocumentProcessingContext context);

    /**
     * 异步处理文档
     *
     * @param context 文档处理上下文
     * @return 处理结果的Future
     */
    CompletableFuture<List<DocumentProcessingResult>> processAsync(DocumentProcessingContext context);

    /**
     * 批量处理文档
     *
     * @param contexts 文档处理上下文列表
     * @return 每个文档的处理结果
     */
    List<List<DocumentProcessingResult>> processBatch(List<DocumentProcessingContext> contexts);

    /**
     * 异步批量处理文档
     *
     * @param contexts 文档处理上下文列表
     * @return 处理结果的Future
     */
    CompletableFuture<List<List<DocumentProcessingResult>>> processBatchAsync(List<DocumentProcessingContext> contexts);

    /**
     * 构建处理器链
     * 根据优先级排序并链接处理器
     */
    void buildProcessorChain();

    /**
     * 清空所有处理器
     */
    void clearProcessors();

    /**
     * 获取处理器数量
     *
     * @return 已注册的处理器数量
     */
    int getProcessorCount();

    /**
     * 检查是否有特定名称的处理器
     *
     * @param processorName 处理器名称
     * @return 如果存在返回true
     */
    boolean hasProcessor(String processorName);

    /**
     * 获取特定名称的处理器
     *
     * @param processorName 处理器名称
     * @return 处理器，如果不存在返回null
     */
    IDocumentProcessor getProcessor(String processorName);
}