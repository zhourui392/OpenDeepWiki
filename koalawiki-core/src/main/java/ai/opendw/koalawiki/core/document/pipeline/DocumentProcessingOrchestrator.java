package ai.opendw.koalawiki.core.document.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文档处理编排器实现
 * 管理和协调文档处理器链
 */
@Slf4j
@Service
public class DocumentProcessingOrchestrator implements IDocumentProcessingOrchestrator {

    /**
     * 已注册的处理器映射（处理器名称 -> 处理器）
     */
    private final Map<String, IDocumentProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * 处理器链的头节点
     */
    private IDocumentProcessor chainHead;

    /**
     * 是否需要重建链
     */
    private volatile boolean needRebuildChain = true;

    @Override
    public void registerProcessor(IDocumentProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("处理器不能为空");
        }

        String name = processor.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("处理器名称不能为空");
        }

        log.info("注册文档处理器: {}", name);
        processorMap.put(name, processor);
        needRebuildChain = true;
    }

    @Override
    public void registerProcessors(List<IDocumentProcessor> processors) {
        if (processors == null || processors.isEmpty()) {
            return;
        }

        log.info("批量注册 {} 个文档处理器", processors.size());
        for (IDocumentProcessor processor : processors) {
            registerProcessor(processor);
        }
    }

    @Override
    public void removeProcessor(String processorName) {
        if (processorName == null || processorName.trim().isEmpty()) {
            return;
        }

        log.info("移除文档处理器: {}", processorName);
        processorMap.remove(processorName);
        needRebuildChain = true;
    }

    @Override
    public List<IDocumentProcessor> getProcessors() {
        return new ArrayList<>(processorMap.values());
    }

    @Override
    public List<DocumentProcessingResult> process(DocumentProcessingContext context) {
        if (context == null) {
            throw new IllegalArgumentException("处理上下文不能为空");
        }

        // 如果需要，重建处理器链
        if (needRebuildChain) {
            synchronized (this) {
                if (needRebuildChain) {
                    buildProcessorChain();
                }
            }
        }

        List<DocumentProcessingResult> results = new ArrayList<>();

        // 如果没有处理器，返回空结果
        if (chainHead == null) {
            log.warn("没有可用的文档处理器");
            return results;
        }

        log.debug("开始处理文档: {}", context.getDocumentPath());

        // 遍历处理器链
        IDocumentProcessor current = chainHead;
        while (current != null) {
            try {
                // 检查处理器是否能处理该文档
                if (!current.canProcess(context)) {
                    log.debug("处理器 {} 跳过文档: {}", current.getName(), context.getDocumentPath());
                    results.add(DocumentProcessingResult.skip(current.getName(), "不适用于此文档类型"));
                    current = current.getNext();
                    continue;
                }

                log.debug("处理器 {} 开始处理文档: {}", current.getName(), context.getDocumentPath());

                // 执行处理
                DocumentProcessingResult result = current.process(context);
                results.add(result);

                // 检查是否继续处理链
                if (!result.isContinueChain()) {
                    log.info("处理器 {} 终止了处理链", current.getName());
                    break;
                }

                // 如果处理失败，根据配置决定是否继续
                if (!result.isSuccess()) {
                    log.error("处理器 {} 处理失败: {}", current.getName(), result.getError());
                    // 可以根据配置决定是否继续
                    break;
                }

            } catch (Exception e) {
                log.error("处理器 {} 发生异常", current.getName(), e);
                results.add(DocumentProcessingResult.failure(current.getName(),
                    "处理器执行异常: " + e.getMessage(), e));
                break;
            }

            current = current.getNext();
        }

        log.debug("文档处理完成: {}, 执行了 {} 个处理器",
            context.getDocumentPath(), results.size());

        return results;
    }

    @Async
    @Override
    public CompletableFuture<List<DocumentProcessingResult>> processAsync(DocumentProcessingContext context) {
        return CompletableFuture.supplyAsync(() -> process(context));
    }

    @Override
    public List<List<DocumentProcessingResult>> processBatch(List<DocumentProcessingContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("批量处理 {} 个文档", contexts.size());

        List<List<DocumentProcessingResult>> batchResults = new ArrayList<>();
        for (DocumentProcessingContext context : contexts) {
            try {
                List<DocumentProcessingResult> results = process(context);
                batchResults.add(results);
            } catch (Exception e) {
                log.error("批量处理文档失败: {}", context.getDocumentPath(), e);
                // 创建失败结果
                List<DocumentProcessingResult> errorResult = new ArrayList<>();
                errorResult.add(DocumentProcessingResult.failure("BatchProcessor",
                    "批量处理失败: " + e.getMessage(), e));
                batchResults.add(errorResult);
            }
        }

        return batchResults;
    }

    @Async
    @Override
    public CompletableFuture<List<List<DocumentProcessingResult>>> processBatchAsync(
            List<DocumentProcessingContext> contexts) {
        return CompletableFuture.supplyAsync(() -> processBatch(contexts));
    }

    @Override
    public void buildProcessorChain() {
        log.info("构建处理器链，共 {} 个处理器", processorMap.size());

        if (processorMap.isEmpty()) {
            chainHead = null;
            needRebuildChain = false;
            return;
        }

        // 按优先级排序处理器
        List<IDocumentProcessor> sortedProcessors = processorMap.values().stream()
                .sorted(Comparator.comparingInt(IDocumentProcessor::getPriority))
                .collect(Collectors.toList());

        // 构建链
        chainHead = sortedProcessors.get(0);
        IDocumentProcessor current = chainHead;

        for (int i = 1; i < sortedProcessors.size(); i++) {
            IDocumentProcessor next = sortedProcessors.get(i);
            current.setNext(next);
            current = next;
        }

        // 确保最后一个处理器的next为null
        current.setNext(null);

        needRebuildChain = false;

        log.info("处理器链构建完成，链顺序: {}",
            sortedProcessors.stream()
                .map(IDocumentProcessor::getName)
                .collect(Collectors.joining(" -> ")));
    }

    @Override
    public void clearProcessors() {
        log.info("清空所有处理器");
        processorMap.clear();
        chainHead = null;
        needRebuildChain = true;
    }

    @Override
    public int getProcessorCount() {
        return processorMap.size();
    }

    @Override
    public boolean hasProcessor(String processorName) {
        return processorName != null && processorMap.containsKey(processorName);
    }

    @Override
    public IDocumentProcessor getProcessor(String processorName) {
        return processorName != null ? processorMap.get(processorName) : null;
    }
}