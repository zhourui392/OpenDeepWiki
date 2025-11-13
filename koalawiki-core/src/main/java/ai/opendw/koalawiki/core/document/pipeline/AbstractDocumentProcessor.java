package ai.opendw.koalawiki.core.document.pipeline;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象文档处理器基类
 * 提供责任链模式的基础实现
 */
@Slf4j
@Getter
@Setter
public abstract class AbstractDocumentProcessor implements IDocumentProcessor {

    /**
     * 处理器名称
     */
    protected final String name;

    /**
     * 处理优先级
     */
    protected final int priority;

    /**
     * 链中的下一个处理器
     */
    protected IDocumentProcessor next;

    /**
     * 构造函数
     *
     * @param name 处理器名称
     * @param priority 处理优先级
     */
    protected AbstractDocumentProcessor(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    /**
     * 构造函数（使用默认优先级）
     *
     * @param name 处理器名称
     */
    protected AbstractDocumentProcessor(String name) {
        this(name, 100);
    }

    @Override
    public DocumentProcessingResult process(DocumentProcessingContext context) {
        log.debug("处理器 {} 开始处理: {}", name, context.getDocumentPath());

        try {
            // 执行前置检查
            if (!preProcess(context)) {
                return DocumentProcessingResult.skip(name, "前置检查未通过");
            }

            // 执行实际处理
            DocumentProcessingResult result = doProcess(context);

            // 执行后置处理
            postProcess(context, result);

            return result;

        } catch (Exception e) {
            log.error("处理器 {} 处理失败: {}", name, context.getDocumentPath(), e);
            return DocumentProcessingResult.failure(name,
                "处理异常: " + e.getMessage(), e);
        }
    }

    /**
     * 前置处理
     * 子类可以重写此方法进行处理前的准备工作
     *
     * @param context 处理上下文
     * @return 如果应该继续处理返回true
     */
    protected boolean preProcess(DocumentProcessingContext context) {
        return true;
    }

    /**
     * 实际的处理逻辑
     * 子类必须实现此方法
     *
     * @param context 处理上下文
     * @return 处理结果
     */
    protected abstract DocumentProcessingResult doProcess(DocumentProcessingContext context);

    /**
     * 后置处理
     * 子类可以重写此方法进行处理后的清理工作
     *
     * @param context 处理上下文
     * @param result 处理结果
     */
    protected void postProcess(DocumentProcessingContext context, DocumentProcessingResult result) {
        // 默认不做任何后置处理
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setNext(IDocumentProcessor next) {
        this.next = next;
    }

    @Override
    public IDocumentProcessor getNext() {
        return next;
    }
}