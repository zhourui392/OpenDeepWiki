package ai.opendw.koalawiki.core.document.pipeline;

/**
 * 文档处理器接口 - 责任链模式
 * 每个处理器负责文档处理的特定方面
 */
public interface IDocumentProcessor {

    /**
     * 处理文档
     *
     * @param context 文档处理上下文
     * @return 处理结果
     */
    DocumentProcessingResult process(DocumentProcessingContext context);

    /**
     * 检查该处理器是否能处理给定的文档
     *
     * @param context 文档处理上下文
     * @return 如果能处理返回true
     */
    boolean canProcess(DocumentProcessingContext context);

    /**
     * 获取处理器名称，用于日志和识别
     *
     * @return 处理器名称
     */
    String getName();

    /**
     * 获取处理优先级。数值越小优先级越高
     *
     * @return 优先级值
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 设置链中的下一个处理器
     *
     * @param next 下一个处理器
     */
    void setNext(IDocumentProcessor next);

    /**
     * 获取链中的下一个处理器
     *
     * @return 下一个处理器，如果是最后一个则返回null
     */
    IDocumentProcessor getNext();
}