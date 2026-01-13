package ai.opendw.koalawiki.core.service.impl;

import ai.opendw.koalawiki.core.git.GitRepositoryInfo;
import ai.opendw.koalawiki.core.git.GitRepositoryManager;
import ai.opendw.koalawiki.core.git.GitCredentials;
import ai.opendw.koalawiki.core.git.CommitInfo;
import ai.opendw.koalawiki.core.git.GitService;
import ai.opendw.koalawiki.core.service.IWarehouseSyncService;
import ai.opendw.koalawiki.domain.warehouse.Warehouse;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncTrigger;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.entity.WarehouseSyncRecordEntity;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import ai.opendw.koalawiki.infra.repository.WarehouseSyncRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仓库同步服务实现 (Alternate Implementation - Disabled)
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Slf4j
// @Service  // Disabled to avoid bean conflict
@RequiredArgsConstructor
public class WarehouseSyncServiceImpl implements IWarehouseSyncService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseSyncRecordRepository syncRecordRepository;
    private final GitRepositoryManager gitRepositoryManager;
    private final GitService gitService;

    /**
     * 正在同步的仓库（warehouseId -> syncRecordId）
     */
    private final Map<String, String> syncingWarehouses = new ConcurrentHashMap<>();

    @Async
    @Override
    public CompletableFuture<String> triggerSync(String warehouseId, WarehouseSyncTrigger trigger) {
        return CompletableFuture.supplyAsync(() -> triggerSyncSync(warehouseId, trigger));
    }

    @Override
    @Transactional
    public String triggerSyncSync(String warehouseId, WarehouseSyncTrigger trigger) {
        log.info("Triggering sync for warehouse: {}, trigger: {}", warehouseId, trigger);

        // 检查仓库是否存在
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseId));

        // 检查是否已经在同步中
        if (syncingWarehouses.containsKey(warehouseId)) {
            String existingRecordId = syncingWarehouses.get(warehouseId);
            log.warn("Warehouse {} is already syncing, record: {}", warehouseId, existingRecordId);
            return existingRecordId;
        }

        // 创建同步记录
        WarehouseSyncRecordEntity syncRecord = new WarehouseSyncRecordEntity();
        syncRecord.setId(UUID.randomUUID().toString());
        syncRecord.setWarehouseId(warehouseId);
        syncRecord.setTrigger(trigger);
        syncRecord.setStatus(WarehouseSyncStatus.PENDING);
        syncRecord.setFromVersion(warehouse.getVersion());
        syncRecord.setProgress(0);
        syncRecord.setCreatedAt(new Date());

        syncRecord = syncRecordRepository.save(syncRecord);
        String syncRecordId = syncRecord.getId();

        // 标记为同步中
        syncingWarehouses.put(warehouseId, syncRecordId);

        // 执行同步
        try {
            executeSyncInternal(warehouse, syncRecord);
        } catch (Exception e) {
            log.error("Failed to execute sync for warehouse: {}", warehouseId, e);
            handleSyncError(syncRecord, e);
        } finally {
            // 移除同步标记
            syncingWarehouses.remove(warehouseId);
        }

        return syncRecordId;
    }

    @Override
    public SyncStatusInfo getSyncStatus(String warehouseId) {
        log.debug("Getting sync status for warehouse: {}", warehouseId);

        SyncStatusInfo statusInfo = new SyncStatusInfo();
        statusInfo.setSyncing(syncingWarehouses.containsKey(warehouseId));

        // 获取最后一次同步记录
        syncRecordRepository.findFirstByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                .ifPresent(record -> {
                    statusInfo.setLastSyncTime(record.getCreatedAt());
                    statusInfo.setLastSyncStatus(record.getStatus());
                    statusInfo.setLastSyncVersion(record.getToVersion());

                    if (statusInfo.isSyncing()) {
                        statusInfo.setCurrentSyncRecordId(record.getId());
                        statusInfo.setCurrentProgress(record.getProgress());
                        statusInfo.setCurrentStatus(record.getStatus());
                    }
                });

        // 统计同步次数
        long totalCount = syncRecordRepository.countByWarehouseId(warehouseId);
        long successCount = syncRecordRepository.countByWarehouseIdAndStatus(warehouseId, WarehouseSyncStatus.SUCCESS);
        long failureCount = syncRecordRepository.countByWarehouseIdAndStatus(warehouseId, WarehouseSyncStatus.FAILED);

        statusInfo.setTotalSyncCount(totalCount);
        statusInfo.setSuccessCount(successCount);
        statusInfo.setFailedCount(failureCount);

        // 获取最后成功的同步
        syncRecordRepository.findFirstByWarehouseIdAndStatusOrderByStartTimeDesc(
                warehouseId, WarehouseSyncStatus.SUCCESS
        ).ifPresent(record -> {
            statusInfo.setLastSuccessSyncTime(record.getEndTime());
            statusInfo.setLastSuccessVersion(record.getToVersion());
        });

        // 计算平均同步时长
        List<WarehouseSyncRecordEntity> successRecords =
                syncRecordRepository.findTop10ByWarehouseIdAndStatusOrderByCreatedAtDesc(
                        warehouseId, WarehouseSyncStatus.SUCCESS
                );
        if (!successRecords.isEmpty()) {
            long avgDuration = successRecords.stream()
                    .filter(r -> r.getDurationMs() != null)
                    .mapToLong(WarehouseSyncRecordEntity::getDurationMs)
                    .sum() / successRecords.size();
            statusInfo.setAverageDuration(avgDuration);
        }

        return statusInfo;
    }

    @Override
    public WarehouseSyncRecordEntity getSyncRecord(String recordId) {
        return syncRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Sync record not found: " + recordId));
    }

    @Override
    public Page<WarehouseSyncRecordEntity> getSyncRecords(String warehouseId, Pageable pageable) {
        return syncRecordRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId, pageable);
    }

    @Override
    public boolean cancelSync(String warehouseId) {
        String syncRecordId = syncingWarehouses.get(warehouseId);
        if (syncRecordId == null) {
            log.warn("Warehouse {} is not syncing", warehouseId);
            return false;
        }

        // 更新同步记录状态
        syncRecordRepository.findById(syncRecordId).ifPresent(record -> {
            record.setStatus(WarehouseSyncStatus.CANCELLED);
            record.setEndTime(new Date());
            if (record.getStartTime() != null) {
                record.setDurationMs(record.getEndTime().getTime() - record.getStartTime().getTime());
            }
            syncRecordRepository.save(record);
        });

        // 移除同步标记
        syncingWarehouses.remove(warehouseId);

        log.info("Cancelled sync for warehouse: {}", warehouseId);
        return true;
    }

    @Async
    @Override
    public CompletableFuture<String> retrySync(String recordId) {
        log.info("Retrying sync for record: {}", recordId);

        WarehouseSyncRecordEntity oldRecord = getSyncRecord(recordId);
        return triggerSync(oldRecord.getWarehouseId(), WarehouseSyncTrigger.MANUAL);
    }

    @Override
    public boolean isSyncing(String warehouseId) {
        return syncingWarehouses.containsKey(warehouseId);
    }

    @Override
    @Transactional
    public int cleanupSyncRecords(String warehouseId, int keepDays) {
        log.info("Cleaning up sync records for warehouse: {}, keeping {} days", warehouseId, keepDays);

        Date cutoffDate = new Date(System.currentTimeMillis() - (keepDays * 24L * 60 * 60 * 1000));
        int deletedCount = syncRecordRepository.deleteByWarehouseIdAndCreatedAtBefore(warehouseId, cutoffDate);

        log.info("Deleted {} sync records for warehouse: {}", deletedCount, warehouseId);
        return deletedCount;
    }

    @Override
    public CompletableFuture<Void> batchTriggerSync(String[] warehouseIds, WarehouseSyncTrigger trigger) {
        log.info("Batch triggering sync for {} warehouses", warehouseIds.length);

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (String warehouseId : warehouseIds) {
            futures.add(triggerSync(warehouseId, trigger));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // ===== 私有方法 =====

    /**
     * 执行同步的内部方法
     */
    private void executeSyncInternal(WarehouseEntity warehouse, WarehouseSyncRecordEntity syncRecord) {
        log.info("Executing sync for warehouse: {}", warehouse.getId());

        // 更新同步记录为进行中
        syncRecord.setStatus(WarehouseSyncStatus.IN_PROGRESS);
        syncRecord.setStartTime(new Date());
        syncRecord.setProgress(10);
        syncRecordRepository.save(syncRecord);

        try {
            // 构建Git认证信息
            GitCredentials credentials = buildGitCredentials(warehouse);

            // 克隆或更新仓库
            syncRecord.setProgress(30);
            syncRecordRepository.save(syncRecord);

            GitRepositoryInfo repoInfo = gitRepositoryManager.getOrCloneRepository(
                    warehouse.getAddress(), credentials
            );

            syncRecord.setProgress(60);
            syncRecordRepository.save(syncRecord);

            // 获取提交历史
            List<CommitInfo> commits = gitService.getCommitHistory(repoInfo.getLocalPath(), 10);

            // 计算变更文件数
            if (commits.size() >= 2) {
                List<String> changedFiles = gitService.getCommitDiff(
                        repoInfo.getLocalPath(),
                        commits.get(1).getCommitId(),
                        commits.get(0).getCommitId()
                );
                syncRecord.setUpdatedFileCount(changedFiles.size());
            }

            syncRecord.setProgress(90);
            syncRecordRepository.save(syncRecord);

            // 更新仓库信息
            warehouse.setVersion(repoInfo.getLatestCommitId());
            warehouse.setStatus(ai.opendw.koalawiki.domain.warehouse.WarehouseStatus.READY);
            warehouse.setError(null);
            warehouseRepository.save(warehouse);

            // 完成同步
            syncRecord.setStatus(WarehouseSyncStatus.SUCCESS);
            syncRecord.setEndTime(new Date());
            syncRecord.setToVersion(repoInfo.getLatestCommitId());
            syncRecord.setProgress(100);
            syncRecord.setDurationMs(syncRecord.getEndTime().getTime() - syncRecord.getStartTime().getTime());
            syncRecordRepository.save(syncRecord);

            log.info("Sync completed successfully for warehouse: {}", warehouse.getId());

        } catch (Exception e) {
            log.error("Sync failed for warehouse: {}", warehouse.getId(), e);
            throw e;
        }
    }

    /**
     * 处理同步错误
     */
    private void handleSyncError(WarehouseSyncRecordEntity syncRecord, Exception e) {
        syncRecord.setStatus(WarehouseSyncStatus.FAILED);
        syncRecord.setEndTime(new Date());
        syncRecord.setErrorMessage(e.getMessage());
        syncRecord.setErrorStack(getStackTrace(e));

        if (syncRecord.getStartTime() != null) {
            syncRecord.setDurationMs(syncRecord.getEndTime().getTime() - syncRecord.getStartTime().getTime());
        }

        syncRecordRepository.save(syncRecord);

        // 更新仓库状态
        warehouseRepository.findById(syncRecord.getWarehouseId()).ifPresent(warehouse -> {
            warehouse.setStatus(ai.opendw.koalawiki.domain.warehouse.WarehouseStatus.ERROR);
            warehouse.setError(e.getMessage());
            warehouseRepository.save(warehouse);
        });
    }

    /**
     * 构建Git认证信息
     *
     * <p>如果仓库没有配置用户名密码，则使用默认值</p>
     *
     * @param warehouse 仓库实体
     * @return Git认证信息
     * @author zhourui(V33215020)
     * @since 2025/11/21
     */
    private GitCredentials buildGitCredentials(WarehouseEntity warehouse) {
        String username = warehouse.getGitUserName();
        String password = warehouse.getGitPassword();

        if (username == null || username.trim().isEmpty()) {
            username = "V33215020";
            log.debug("使用默认Git用户名: {}", username);
        }

        if (password == null || password.trim().isEmpty()) {
            password = "Zz135246";
            log.debug("使用默认Git密码");
        }

        GitCredentials credentials = new GitCredentials();
        credentials.setType(GitCredentials.CredentialType.HTTP_BASIC);
        credentials.setUsername(username);
        credentials.setPassword(password);
        return credentials;
    }

    /**
     * 获取异常堆栈跟踪
     */
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
