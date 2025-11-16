package ai.opendw.koalawiki.core.scheduler;

import ai.opendw.koalawiki.core.service.IWarehouseSyncService;
import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncTrigger;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.entity.WarehouseSyncRecordEntity;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import ai.opendw.koalawiki.infra.repository.WarehouseSyncRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 仓库同步定时任务调度器
 * 负责定期执行仓库同步任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseSyncScheduler {

    private final IWarehouseSyncService warehouseSyncService;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseSyncRecordRepository syncRecordRepository;

    /**
     * 同步超时时间（分钟）
     */
    @Value("${koalawiki.sync.timeout-minutes:30}")
    private int syncTimeoutMinutes;

    /**
     * 是否启用自动同步
     */
    @Value("${koalawiki.sync.auto-enabled:true}")
    private boolean autoSyncEnabled;

    /**
     * 定期执行仓库自动同步
     * 默认每30分钟执行一次
     */
    @Scheduled(fixedDelayString = "${koalawiki.sync.interval-ms:1800000}",
               initialDelayString = "${koalawiki.sync.initial-delay-ms:60000}")
    public void executeAutoSync() {
        if (!autoSyncEnabled) {
            log.debug("自动同步已禁用");
            return;
        }

        log.info("开始执行定时仓库同步任务");

        try {
            // 查找需要同步的仓库（启用同步且状态为COMPLETED）
            List<WarehouseEntity> warehouses = warehouseRepository.findByEnableSyncAndStatus(
                true, WarehouseStatus.COMPLETED);

            if (warehouses.isEmpty()) {
                log.debug("没有需要自动同步的仓库");
                return;
            }

            log.info("找到 {} 个需要同步的仓库", warehouses.size());

            // 执行同步
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (WarehouseEntity warehouse : warehouses) {
                try {
                    // 检查是否已经在同步中
                    if (syncRecordRepository.isWarehouseSyncing(warehouse.getId())) {
                        log.debug("仓库 {} 已经在同步中，跳过", warehouse.getName());
                        skipCount++;
                        continue;
                    }

                    // 触发异步同步
                    log.info("触发仓库 {} 的自动同步", warehouse.getName());
                    warehouseSyncService.triggerSync(warehouse.getId(), WarehouseSyncTrigger.AUTO);
                    successCount++;

                    // 避免同时触发太多同步任务
                    Thread.sleep(1000);

                } catch (Exception e) {
                    log.error("仓库 {} 同步失败", warehouse.getName(), e);
                    failCount++;
                }
            }

            log.info("定时同步任务完成: 成功={}, 跳过={}, 失败={}",
                successCount, skipCount, failCount);

        } catch (Exception e) {
            log.error("定时同步任务执行异常", e);
        }
    }

    /**
     * 清理超时的同步记录
     * 每10分钟执行一次
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 120000)
    public void cleanupTimeoutSyncRecords() {
        log.debug("开始清理超时的同步记录");

        try {
            Date timeoutThreshold = new Date(System.currentTimeMillis() -
                TimeUnit.MINUTES.toMillis(syncTimeoutMinutes));

            // 查找超时的同步记录
            List<WarehouseSyncRecordEntity> timeoutRecords =
                syncRecordRepository.findTimeoutRecords(
                    WarehouseSyncStatus.IN_PROGRESS, timeoutThreshold);

            if (timeoutRecords.isEmpty()) {
                log.debug("没有超时的同步记录");
                return;
            }

            log.info("找到 {} 个超时的同步记录", timeoutRecords.size());

            // 标记为失败
            for (WarehouseSyncRecordEntity record : timeoutRecords) {
                record.setStatus(WarehouseSyncStatus.FAILED);
                record.setEndTime(new Date());
                record.setErrorMessage("同步超时（超过" + syncTimeoutMinutes + "分钟）");
                syncRecordRepository.save(record);

                log.warn("同步记录 {} 已超时，标记为失败", record.getId());
            }

        } catch (Exception e) {
            log.error("清理超时同步记录失败", e);
        }
    }

    /**
     * 统计并报告同步状态
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void reportSyncStatistics() {
        log.info("生成同步统计报告");

        try {
            // 获取最近24小时的统计
            Date since = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24));

            long totalCount = syncRecordRepository.countByStartTimeAfter(since);
            long successCount = syncRecordRepository.countByStatusAndStartTimeAfter(
                WarehouseSyncStatus.SUCCESS, since);
            long failedCount = syncRecordRepository.countByStatusAndStartTimeAfter(
                WarehouseSyncStatus.FAILED, since);
            long inProgressCount = syncRecordRepository.countByStatus(
                WarehouseSyncStatus.IN_PROGRESS);

            log.info("过去24小时同步统计: 总数={}, 成功={}, 失败={}, 进行中={}",
                totalCount, successCount, failedCount, inProgressCount);

            // 如果有异常情况，发出警告
            if (failedCount > successCount) {
                log.warn("警告: 过去24小时失败的同步任务数量超过成功数量");
            }

            if (inProgressCount > 5) {
                log.warn("警告: 当前有 {} 个同步任务仍在进行中，可能存在问题", inProgressCount);
            }

        } catch (Exception e) {
            log.error("生成同步统计报告失败", e);
        }
    }

    /**
     * 健康检查任务
     * 每5分钟执行一次
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void healthCheck() {
        log.debug("执行仓库同步健康检查");

        try {
            // 检查是否有长时间运行的同步任务
            Date longRunningThreshold = new Date(System.currentTimeMillis() -
                TimeUnit.MINUTES.toMillis(syncTimeoutMinutes / 2));

            List<WarehouseSyncRecordEntity> longRunningRecords =
                syncRecordRepository.findByStatusAndStartTimeBefore(
                    WarehouseSyncStatus.IN_PROGRESS, longRunningThreshold);

            if (!longRunningRecords.isEmpty()) {
                log.warn("发现 {} 个长时间运行的同步任务（超过{}分钟）",
                    longRunningRecords.size(), syncTimeoutMinutes / 2);

                for (WarehouseSyncRecordEntity record : longRunningRecords) {
                    long runningMinutes = TimeUnit.MILLISECONDS.toMinutes(
                        System.currentTimeMillis() - record.getStartTime().getTime());
                    log.warn("同步任务 {} 已运行 {} 分钟", record.getId(), runningMinutes);
                }
            }

        } catch (Exception e) {
            log.error("健康检查失败", e);
        }
    }
}