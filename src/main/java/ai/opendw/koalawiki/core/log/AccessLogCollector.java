package ai.opendw.koalawiki.core.log;

import ai.opendw.koalawiki.domain.log.AccessLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 访问日志收集器
 * 使用异步队列收集访问日志，避免影响主流程性能
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Component
public class AccessLogCollector {

    /**
     * 日志队列，容量10000
     */
    private final BlockingQueue<AccessLog> logQueue = new LinkedBlockingQueue<>(10000);

    /**
     * 是否正在运行
     */
    private volatile boolean running = true;

    /**
     * 队列满丢弃计数
     */
    private long droppedCount = 0;

    @PostConstruct
    public void init() {
        log.info("AccessLogCollector initialized, queue capacity: {}", logQueue.remainingCapacity() + logQueue.size());
    }

    @PreDestroy
    public void destroy() {
        running = false;
        log.info("AccessLogCollector destroyed, remaining logs: {}, dropped: {}",
                logQueue.size(), droppedCount);
    }

    /**
     * 收集访问日志（异步）
     *
     * @param log 访问日志
     * @return 是否成功入队
     */
    public boolean collect(AccessLog log) {
        if (log == null) {
            return false;
        }

        // 非阻塞方式入队，如果队列满则丢弃
        boolean success = logQueue.offer(log);
        if (!success) {
            droppedCount++;
            if (droppedCount % 100 == 0) {
                this.log.warn("Access log queue is full, dropped count: {}", droppedCount);
            }
        }
        return success;
    }

    /**
     * 批量获取日志（阻塞方式）
     *
     * @param maxSize 最大数量
     * @param timeout 超时时间（毫秒）
     * @return 日志列表
     * @throws InterruptedException 中断异常
     */
    public List<AccessLog> pollBatch(int maxSize, long timeout) throws InterruptedException {
        List<AccessLog> logs = new ArrayList<>(maxSize);

        // 阻塞获取第一条（带超时）
        AccessLog first = logQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (first != null) {
            logs.add(first);

            // 非阻塞获取剩余的（最多maxSize-1条）
            logQueue.drainTo(logs, maxSize - 1);
        }

        return logs;
    }

    /**
     * 批量获取日志（非阻塞方式）
     *
     * @param maxSize 最大数量
     * @return 日志列表
     */
    public List<AccessLog> pollBatchNonBlocking(int maxSize) {
        List<AccessLog> logs = new ArrayList<>(maxSize);
        logQueue.drainTo(logs, maxSize);
        return logs;
    }

    /**
     * 获取队列当前大小
     *
     * @return 队列大小
     */
    public int getQueueSize() {
        return logQueue.size();
    }

    /**
     * 获取队列剩余容量
     *
     * @return 剩余容量
     */
    public int getRemainingCapacity() {
        return logQueue.remainingCapacity();
    }

    /**
     * 获取丢弃计数
     *
     * @return 丢弃的日志数量
     */
    public long getDroppedCount() {
        return droppedCount;
    }

    /**
     * 清空队列
     */
    public void clear() {
        logQueue.clear();
        log.info("Access log queue cleared");
    }

    /**
     * 获取队列使用率
     *
     * @return 使用率（0-1）
     */
    public double getUsageRate() {
        int capacity = logQueue.remainingCapacity() + logQueue.size();
        return (double) logQueue.size() / capacity;
    }
}
