package ai.opendw.koalawiki.app.task;

import ai.opendw.koalawiki.core.log.AccessLogCollector;
import ai.opendw.koalawiki.domain.log.AccessLog;
import ai.opendw.koalawiki.infra.entity.AccessLogEntity;
import ai.opendw.koalawiki.infra.repository.AccessLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 访问日志处理定时任务
 * 定期从队列中批量获取日志并保存到数据库
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Component
@EnableScheduling
public class AccessLogTask {

    @Autowired
    private AccessLogCollector accessLogCollector;

    @Autowired
    private AccessLogProcessor accessLogProcessor;

    @Autowired
    private AccessLogRepository accessLogRepository;

    /**
     * 批量大小
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 日志保留天数
     */
    private static final int RETENTION_DAYS = 90;

    /**
     * 处理访问日志
     * 每30秒执行一次
     */
    @Scheduled(fixedDelay = 30000)
    public void processLogs() {
        try {
            // 从队列获取日志
            List<AccessLog> logs = accessLogCollector.pollBatchNonBlocking(BATCH_SIZE);

            if (logs.isEmpty()) {
                return;
            }

            log.info("Processing {} access logs from queue", logs.size());

            // 批量处理
            accessLogProcessor.processBatch(logs);

            log.info("Successfully processed {} access logs, queue size: {}",
                    logs.size(), accessLogCollector.getQueueSize());

        } catch (Exception e) {
            log.error("Failed to process access logs", e);
        }
    }

    /**
     * 清理过期日志
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldLogs() {
        try {
            log.info("Starting to cleanup old access logs, retention days: {}", RETENTION_DAYS);

            // 计算过期日期
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -RETENTION_DAYS);
            Date expireDate = calendar.getTime();

            // 删除过期日志
            accessLogRepository.deleteByAccessTimeBefore(expireDate);

            log.info("Successfully cleaned up access logs before {}", expireDate);

        } catch (Exception e) {
            log.error("Failed to cleanup old access logs", e);
        }
    }

    /**
     * 监控队列状态
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void monitorQueue() {
        int queueSize = accessLogCollector.getQueueSize();
        double usageRate = accessLogCollector.getUsageRate();
        long droppedCount = accessLogCollector.getDroppedCount();

        if (log.isDebugEnabled()) {
            log.debug("Access log queue status - size: {}, usage: {:.2f}%, dropped: {}",
                    queueSize, usageRate * 100, droppedCount);
        }

        // 队列使用率超过80%时发出警告
        if (usageRate > 0.8) {
            log.warn("Access log queue usage is high: {:.2f}%, size: {}",
                    usageRate * 100, queueSize);
        }

        // 有丢弃日志时发出警告
        if (droppedCount > 0 && droppedCount % 1000 == 0) {
            log.warn("Access log queue has dropped {} logs", droppedCount);
        }
    }
}
