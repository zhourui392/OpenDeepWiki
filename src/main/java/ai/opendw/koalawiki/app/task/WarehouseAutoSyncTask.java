package ai.opendw.koalawiki.app.task;

import ai.opendw.koalawiki.core.service.IWarehouseSyncService;
import ai.opendw.koalawiki.domain.warehouse.Warehouse;
import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 仓库自动同步任务
 * 定期扫描并同步需要更新的仓库
 *
 * 参考C#实现: WarehouseProcessingTask.cs
 */
@Slf4j
@Component
public class WarehouseAutoSyncTask {

    private final IWarehouseSyncService warehouseSyncService;
    private final WarehouseRepository warehouseRepository;

    @Value("${koalawiki.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${koalawiki.sync.batch-size:10}")
    private int batchSize;

    @Value("${koalawiki.sync.max-concurrent:3}")
    private int maxConcurrent;

    // 记录当前正在执行的任务数
    private final AtomicInteger runningTasks = new AtomicInteger(0);

    // 记录任务是否正在执行，防止重复执行
    private volatile boolean isRunning = false;

    @Autowired
    public WarehouseAutoSyncTask(IWarehouseSyncService warehouseSyncService,
                                  WarehouseRepository warehouseRepository) {
        this.warehouseSyncService = warehouseSyncService;
        this.warehouseRepository = warehouseRepository;
    }

    /**
     * 定时执行仓库同步
     * 默认每6小时执行一次
     */
    @Scheduled(cron = "${koalawiki.sync.cron:0 0 */6 * * ?}")
    public void executeAutoSync() {
        if (!syncEnabled) {
            log.debug("仓库自动同步已禁用");
            return;
        }

        if (isRunning) {
            log.info("仓库自动同步任务正在执行，跳过本次调度");
            return;
        }

        log.info("开始执行仓库自动同步任务");
        isRunning = true;
        long startTime = System.currentTimeMillis();

        try {
            // 查询需要同步的仓库
            List<Warehouse> warehouses = findWarehousesToSync();

            if (warehouses.isEmpty()) {
                log.info("没有需要同步的仓库");
                return;
            }

            log.info("找到 {} 个需要同步的仓库", warehouses.size());

            // 批量处理仓库
            int processedCount = 0;
            int successCount = 0;
            int failedCount = 0;

            for (Warehouse warehouse : warehouses) {
                // 控制并发数
                while (runningTasks.get() >= maxConcurrent) {
                    Thread.sleep(1000);
                }

                // 异步触发同步
                runningTasks.incrementAndGet();
                CompletableFuture<String> future = warehouseSyncService.triggerSync(
                    warehouse.getId(), false);

                // 处理结果
                future.whenComplete((syncRecordId, throwable) -> {
                    runningTasks.decrementAndGet();

                    if (throwable != null) {
                        log.error("仓库同步失败: warehouseId={}", warehouse.getId(), throwable);
                    } else {
                        log.info("仓库同步任务已触发: warehouseId={}, syncRecordId={}",
                            warehouse.getId(), syncRecordId);
                    }
                });

                processedCount++;

                // 批量控制
                if (processedCount >= batchSize) {
                    log.info("已达到批次限制 {}，等待当前批次完成", batchSize);
                    // 等待当前批次完成
                    while (runningTasks.get() > 0) {
                        Thread.sleep(1000);
                    }
                    processedCount = 0;
                }
            }

            // 等待所有任务完成
            while (runningTasks.get() > 0) {
                Thread.sleep(1000);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("仓库自动同步任务完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                warehouses.size(), successCount, failedCount, duration);

        } catch (Exception e) {
            log.error("仓库自动同步任务执行失败", e);
        } finally {
            isRunning = false;
        }
    }

    /**
     * 查找需要同步的仓库
     */
    private List<Warehouse> findWarehousesToSync() {
        // 查询启用自动同步且状态正常的仓库（使用WarehouseStatus枚举）
        List<ai.opendw.koalawiki.infra.entity.WarehouseEntity> warehouseEntities =
                warehouseRepository.findByEnableSyncAndStatus(true, WarehouseStatus.COMPLETED);

        // 转换为domain对象并过滤出需要同步的仓库
        return warehouseEntities.stream()
            .map(this::convertToDomain)
            .filter(this::shouldSync)
            .limit(batchSize * 2) // 限制查询数量
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 将Entity转换为Domain对象
     */
    private Warehouse convertToDomain(ai.opendw.koalawiki.infra.entity.WarehouseEntity entity) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(entity.getId());
        warehouse.setName(entity.getName());
        warehouse.setDescription(entity.getDescription());
        warehouse.setAddress(entity.getAddress());
        warehouse.setStatus(entity.getStatus());
        warehouse.setClassify(entity.getClassify());
        warehouse.setEnableSync(entity.getEnableSync());
        warehouse.setCreatedAt(entity.getCreatedAt());
        return warehouse;
    }

    /**
     * 判断仓库是否需要同步
     */
    private boolean shouldSync(Warehouse warehouse) {
        // 检查是否正在同步
        IWarehouseSyncService.SyncStatusInfo statusInfo =
            warehouseSyncService.getSyncStatus(warehouse.getId());

        if (statusInfo.isSyncing()) {
            log.debug("仓库正在同步中，跳过: {}", warehouse.getId());
            return false;
        }

        // 检查上次同步时间
        Date lastSyncTime = statusInfo.getLastSuccessSyncTime();
        if (lastSyncTime == null) {
            // 从未同步过，需要同步
            return true;
        }

        // 计算时间间隔（毫秒）
        long timeSinceLastSync = System.currentTimeMillis() - lastSyncTime.getTime();
        long syncIntervalMillis = getSyncInterval(warehouse);

        // 如果超过同步间隔，需要同步
        return timeSinceLastSync >= syncIntervalMillis;
    }

    /**
     * 获取仓库的同步间隔
     */
    private long getSyncInterval(Warehouse warehouse) {
        // Warehouse实体中没有syncIntervalDays字段
        // 使用默认的同步间隔：1天
        int syncIntervalDays = 1;

        // 转换为毫秒
        return syncIntervalDays * 24L * 60L * 60L * 1000L;
    }

    /**
     * 手动触发自动同步（用于测试或管理）
     */
    public void triggerManual() {
        log.info("手动触发仓库自动同步");
        executeAutoSync();
    }

    /**
     * 获取任务状态
     */
    public TaskStatus getStatus() {
        TaskStatus status = new TaskStatus();
        status.setRunning(isRunning);
        status.setRunningTasks(runningTasks.get());
        status.setSyncEnabled(syncEnabled);
        status.setBatchSize(batchSize);
        status.setMaxConcurrent(maxConcurrent);
        return status;
    }

    /**
     * 任务状态
     */
    public static class TaskStatus {
        private boolean running;
        private int runningTasks;
        private boolean syncEnabled;
        private int batchSize;
        private int maxConcurrent;

        // Getters and setters
        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }

        public int getRunningTasks() { return runningTasks; }
        public void setRunningTasks(int runningTasks) { this.runningTasks = runningTasks; }

        public boolean isSyncEnabled() { return syncEnabled; }
        public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public int getMaxConcurrent() { return maxConcurrent; }
        public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
    }
}