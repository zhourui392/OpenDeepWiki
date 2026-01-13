package ai.opendw.koalawiki.core.service;

import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncTrigger;
import ai.opendw.koalawiki.infra.entity.WarehouseSyncRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.concurrent.CompletableFuture;

/**
 * 仓库同步服务接口
 * 负责管理仓库的同步流程
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
public interface IWarehouseSyncService {

    /**
     * 触发仓库同步（异步）
     *
     * @param warehouseId 仓库ID
     * @param trigger     触发方式
     * @return 异步任务，返回同步记录ID
     */
    CompletableFuture<String> triggerSync(String warehouseId, WarehouseSyncTrigger trigger);

    /**
     * 触发仓库同步（异步） - 重载方法
     *
     * @param warehouseId 仓库ID
     * @param forceSync   是否强制同步
     * @return 异步任务，返回同步记录ID
     */
    default CompletableFuture<String> triggerSync(String warehouseId, boolean forceSync) {
        return triggerSync(warehouseId, forceSync ? WarehouseSyncTrigger.MANUAL : WarehouseSyncTrigger.AUTO);
    }

    /**
     * 触发仓库同步（同步）
     *
     * @param warehouseId 仓库ID
     * @param trigger     触发方式
     * @return 同步记录ID
     */
    String triggerSyncSync(String warehouseId, WarehouseSyncTrigger trigger);

    /**
     * 获取仓库同步状态
     *
     * @param warehouseId 仓库ID
     * @return 同步状态信息
     */
    SyncStatusInfo getSyncStatus(String warehouseId);

    /**
     * 获取同步记录
     *
     * @param recordId 记录ID
     * @return 同步记录
     */
    WarehouseSyncRecordEntity getSyncRecord(String recordId);

    /**
     * 获取仓库的同步记录列表
     *
     * @param warehouseId 仓库ID
     * @param pageable    分页参数
     * @return 同步记录分页
     */
    Page<WarehouseSyncRecordEntity> getSyncRecords(String warehouseId, Pageable pageable);

    /**
     * 取消正在进行的同步
     *
     * @param warehouseId 仓库ID
     * @return 是否成功取消
     */
    boolean cancelSync(String warehouseId);

    /**
     * 重试失败的同步
     *
     * @param recordId 同步记录ID
     * @return 新的同步记录ID（异步）
     */
    CompletableFuture<String> retrySync(String recordId);

    /**
     * 检查仓库是否正在同步
     *
     * @param warehouseId 仓库ID
     * @return 是否正在同步
     */
    boolean isSyncing(String warehouseId);

    /**
     * 清理历史同步记录
     *
     * @param warehouseId 仓库ID
     * @param keepDays    保留天数
     * @return 清理的记录数
     */
    int cleanupSyncRecords(String warehouseId, int keepDays);

    /**
     * 批量触发仓库同步
     *
     * @param warehouseIds 仓库ID列表
     * @param trigger      触发方式
     * @return 异步任务列表
     */
    CompletableFuture<Void> batchTriggerSync(String[] warehouseIds, WarehouseSyncTrigger trigger);

    /**
     * 同步状态信息
     */
    class SyncStatusInfo {
        /**
         * 是否正在同步
         */
        private boolean syncing;

        /**
         * 最后同步时间
         */
        private java.util.Date lastSyncTime;

        /**
         * 最后同步状态
         */
        private WarehouseSyncStatus lastSyncStatus;

        /**
         * 最后同步版本
         */
        private String lastSyncVersion;

        /**
         * 同步次数
         */
        private long syncCount;

        /**
         * 成功次数
         */
        private long successCount;

        /**
         * 失败次数
         */
        private long failureCount;

        /**
         * 当前同步记录ID（如果正在同步）
         */
        private String currentSyncRecordId;

        /**
         * 当前同步进度（百分比）
         */
        private Integer currentProgress;

        /**
         * 当前状态
         */
        private WarehouseSyncStatus currentStatus;

        /**
         * 总同步次数
         */
        private long totalSyncCount;

        /**
         * 失败次数
         */
        private long failedCount;

        /**
         * 最后成功同步时间
         */
        private java.util.Date lastSuccessSyncTime;

        /**
         * 最后成功同步版本
         */
        private String lastSuccessVersion;

        /**
         * 平均同步时长（毫秒）
         */
        private Long averageDuration;

        // Getters and Setters

        public boolean isSyncing() {
            return syncing;
        }

        public void setSyncing(boolean syncing) {
            this.syncing = syncing;
        }

        public java.util.Date getLastSyncTime() {
            return lastSyncTime;
        }

        public void setLastSyncTime(java.util.Date lastSyncTime) {
            this.lastSyncTime = lastSyncTime;
        }

        public WarehouseSyncStatus getLastSyncStatus() {
            return lastSyncStatus;
        }

        public void setLastSyncStatus(WarehouseSyncStatus lastSyncStatus) {
            this.lastSyncStatus = lastSyncStatus;
        }

        public String getLastSyncVersion() {
            return lastSyncVersion;
        }

        public void setLastSyncVersion(String lastSyncVersion) {
            this.lastSyncVersion = lastSyncVersion;
        }

        public long getSyncCount() {
            return syncCount;
        }

        public void setSyncCount(long syncCount) {
            this.syncCount = syncCount;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(long successCount) {
            this.successCount = successCount;
        }

        public long getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(long failureCount) {
            this.failureCount = failureCount;
        }

        public String getCurrentSyncRecordId() {
            return currentSyncRecordId;
        }

        public void setCurrentSyncRecordId(String currentSyncRecordId) {
            this.currentSyncRecordId = currentSyncRecordId;
        }

        public Integer getCurrentProgress() {
            return currentProgress;
        }

        public void setCurrentProgress(Integer currentProgress) {
            this.currentProgress = currentProgress;
        }

        public WarehouseSyncStatus getCurrentStatus() {
            return currentStatus;
        }

        public void setCurrentStatus(WarehouseSyncStatus currentStatus) {
            this.currentStatus = currentStatus;
        }

        public long getTotalSyncCount() {
            return totalSyncCount;
        }

        public void setTotalSyncCount(long totalSyncCount) {
            this.totalSyncCount = totalSyncCount;
        }

        public long getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(long failedCount) {
            this.failedCount = failedCount;
        }

        public java.util.Date getLastSuccessSyncTime() {
            return lastSuccessSyncTime;
        }

        public void setLastSuccessSyncTime(java.util.Date lastSuccessSyncTime) {
            this.lastSuccessSyncTime = lastSuccessSyncTime;
        }

        public String getLastSuccessVersion() {
            return lastSuccessVersion;
        }

        public void setLastSuccessVersion(String lastSuccessVersion) {
            this.lastSuccessVersion = lastSuccessVersion;
        }

        public Long getAverageDuration() {
            return averageDuration;
        }

        public void setAverageDuration(Long averageDuration) {
            this.averageDuration = averageDuration;
        }
    }
}