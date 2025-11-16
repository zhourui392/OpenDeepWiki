package ai.opendw.koalawiki.core.service;

import ai.opendw.koalawiki.core.event.WarehouseSyncCompletedEvent;
import ai.opendw.koalawiki.core.util.IdGenerator;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncRecord;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncTrigger;
import ai.opendw.koalawiki.infra.entity.WarehouseSyncRecordEntity;
import ai.opendw.koalawiki.infra.repository.WarehouseSyncRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 仓库同步服务实现
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseSyncServiceImpl implements IWarehouseSyncService {

    private final WarehouseSyncRecordRepository syncRecordRepository;
    private final IWarehouseSyncExecutor syncExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 正在进行的同步任务缓存
     */
    private final Map<String, String> syncingTasks = new ConcurrentHashMap<>();

    @Override
    @Async
    public CompletableFuture<String> triggerSync(String warehouseId, WarehouseSyncTrigger trigger) {
        log.info("Triggering async sync for warehouse: {}, trigger: {}", warehouseId, trigger);

        try {
            String recordId = triggerSyncSync(warehouseId, trigger);
            return CompletableFuture.completedFuture(recordId);
        } catch (Exception e) {
            log.error("Failed to trigger sync for warehouse: {}", warehouseId, e);
            // Java 8 doesn't have failedFuture, need to handle differently
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    @Transactional
    public String triggerSyncSync(String warehouseId, WarehouseSyncTrigger trigger) {
        log.info("Triggering sync for warehouse: {}, trigger: {}", warehouseId, trigger);

        // 检查是否已在同步中
        if (isSyncing(warehouseId)) {
            log.warn("Warehouse {} is already syncing", warehouseId);
            throw new IllegalStateException("Warehouse is already syncing");
        }

        // 验证仓库
        IWarehouseSyncExecutor.ValidationResult validation = syncExecutor.validate(warehouseId);
        if (!validation.isValid()) {
            log.error("Warehouse validation failed: {}", validation.getErrorMessage());
            throw new IllegalArgumentException("Warehouse validation failed: " + validation.getErrorMessage());
        }

        // 创建同步记录
        WarehouseSyncRecordEntity syncRecord = new WarehouseSyncRecordEntity();
        syncRecord.setId(IdGenerator.generateId());
        syncRecord.setCreatedAt(new Date());
        syncRecord.setWarehouseId(warehouseId);
        syncRecord.setStatus(WarehouseSyncStatus.IN_PROGRESS);
        syncRecord.setTrigger(trigger);
        syncRecord.setStartTime(new Date());

        // 获取上次成功的同步记录
        Optional<WarehouseSyncRecordEntity> lastSuccess = syncRecordRepository
                .findFirstByWarehouseIdAndStatusOrderByStartTimeDesc(warehouseId, WarehouseSyncStatus.SUCCESS);
        if (lastSuccess.isPresent()) {
            syncRecord.setFromVersion(lastSuccess.get().getToVersion());
        }

        // 保存同步记录
        syncRecord = syncRecordRepository.save(syncRecord);

        // 添加到正在同步的任务缓存
        syncingTasks.put(warehouseId, syncRecord.getId());

        // 执行同步
        executeSyncAsync(warehouseId, syncRecord);

        return syncRecord.getId();
    }

    /**
     * 异步执行同步任务
     */
    @Async
    protected void executeSyncAsync(String warehouseId, WarehouseSyncRecordEntity syncRecord) {
        log.info("Executing sync for warehouse: {}, recordId: {}", warehouseId, syncRecord.getId());

        try {
            // 转换为领域对象
            WarehouseSyncRecord domainRecord = convertToDomain(syncRecord);

            // 执行同步
            IWarehouseSyncExecutor.SyncExecutionResult result = syncExecutor.execute(warehouseId, domainRecord);

            // 更新同步记录
            updateSyncRecord(syncRecord, result);

        } catch (Exception e) {
            log.error("Sync execution failed for warehouse: {}", warehouseId, e);

            // 更新为失败状态
            syncRecord.setStatus(WarehouseSyncStatus.FAILED);
            syncRecord.setEndTime(new Date());
            syncRecord.setErrorMessage(e.getMessage());
            syncRecord.setDurationMs(syncRecord.getEndTime().getTime() - syncRecord.getStartTime().getTime());
            syncRecordRepository.save(syncRecord);
        } finally {
            // 从正在同步的任务缓存中移除
            syncingTasks.remove(warehouseId);
        }
    }

    /**
     * 更新同步记录
     */
    @Transactional
    protected void updateSyncRecord(WarehouseSyncRecordEntity syncRecord, IWarehouseSyncExecutor.SyncExecutionResult result) {
        syncRecord.setEndTime(new Date());
        syncRecord.setDurationMs(result.getDuration());

        if (result.isSuccess()) {
            syncRecord.setStatus(WarehouseSyncStatus.SUCCESS);
            syncRecord.setToVersion(result.getToVersion());
            syncRecord.setFileCount(result.getFileCount());
            syncRecord.setAddedFileCount(result.getAddedCount());
            syncRecord.setUpdatedFileCount(result.getUpdatedCount());
            syncRecord.setDeletedFileCount(result.getDeletedCount());
            syncRecord.setDetails(result.getDetails());

            // 保存记录
            syncRecordRepository.save(syncRecord);
            log.info("Sync record updated: {}, status: {}", syncRecord.getId(), syncRecord.getStatus());

            // 发布同步完成事件
            try {
                // 从details JSON中提取localPath
                String localPath = extractLocalPath(result.getDetails());
                if (localPath != null && !localPath.isEmpty()) {
                    WarehouseSyncCompletedEvent event = new WarehouseSyncCompletedEvent(
                            this,
                            syncRecord.getWarehouseId(),
                            syncRecord.getId(),
                            localPath,
                            result.getToVersion(),
                            result.getFileCount()
                    );
                    eventPublisher.publishEvent(event);
                    log.info("Published WarehouseSyncCompletedEvent for warehouse: {}", syncRecord.getWarehouseId());
                } else {
                    log.warn("Cannot publish event: localPath not found in sync result details");
                }
            } catch (Exception e) {
                log.error("Failed to publish sync completed event", e);
                // 不影响主流程,仅记录日志
            }
        } else {
            syncRecord.setStatus(WarehouseSyncStatus.FAILED);
            syncRecord.setErrorMessage(result.getErrorMessage());
            syncRecordRepository.save(syncRecord);
            log.info("Sync record updated: {}, status: {}", syncRecord.getId(), syncRecord.getStatus());
        }
    }

    /**
     * 从details JSON中提取localPath
     */
    private String extractLocalPath(String details) {
        try {
            if (details == null || details.trim().isEmpty()) {
                return null;
            }
            Map<String, Object> detailsMap = objectMapper.readValue(details, Map.class);
            return (String) detailsMap.get("localPath");
        } catch (Exception e) {
            log.error("Failed to extract localPath from details", e);
            return null;
        }
    }

    @Override
    public SyncStatusInfo getSyncStatus(String warehouseId) {
        SyncStatusInfo statusInfo = new SyncStatusInfo();

        // 检查是否正在同步
        statusInfo.setSyncing(isSyncing(warehouseId));
        if (statusInfo.isSyncing()) {
            statusInfo.setCurrentSyncRecordId(syncingTasks.get(warehouseId));
        }

        // 获取最新的同步记录
        Optional<WarehouseSyncRecordEntity> latestRecord = syncRecordRepository
                .findFirstByWarehouseIdOrderByStartTimeDesc(warehouseId);

        if (latestRecord.isPresent()) {
            WarehouseSyncRecordEntity record = latestRecord.get();
            statusInfo.setLastSyncTime(record.getEndTime() != null ? record.getEndTime() : record.getStartTime());
            statusInfo.setLastSyncStatus(record.getStatus());
            statusInfo.setLastSyncVersion(record.getToVersion());
        }

        // 统计信息
        statusInfo.setSyncCount(syncRecordRepository.countByWarehouseId(warehouseId));

        // 统计成功和失败次数
        List<WarehouseSyncRecordEntity> successRecords = syncRecordRepository
                .findByWarehouseIdAndStatus(warehouseId, WarehouseSyncStatus.SUCCESS);
        statusInfo.setSuccessCount(successRecords.size());

        List<WarehouseSyncRecordEntity> failedRecords = syncRecordRepository
                .findByWarehouseIdAndStatus(warehouseId, WarehouseSyncStatus.FAILED);
        statusInfo.setFailureCount(failedRecords.size());

        return statusInfo;
    }

    @Override
    public WarehouseSyncRecordEntity getSyncRecord(String recordId) {
        return syncRecordRepository.findById(recordId).orElse(null);
    }

    @Override
    public Page<WarehouseSyncRecordEntity> getSyncRecords(String warehouseId, Pageable pageable) {
        return syncRecordRepository.findByWarehouseIdOrderByStartTimeDesc(warehouseId, pageable);
    }

    @Override
    public boolean cancelSync(String warehouseId) {
        if (!isSyncing(warehouseId)) {
            return false;
        }

        String recordId = syncingTasks.get(warehouseId);
        if (recordId == null) {
            return false;
        }

        // 调用执行器取消同步
        boolean cancelled = syncExecutor.cancel(warehouseId, recordId);

        if (cancelled) {
            // 更新同步记录状态
            Optional<WarehouseSyncRecordEntity> record = syncRecordRepository.findById(recordId);
            if (record.isPresent()) {
                WarehouseSyncRecordEntity entity = record.get();
                entity.setStatus(WarehouseSyncStatus.CANCELLED);
                entity.setEndTime(new Date());
                entity.setErrorMessage("Sync cancelled by user");
                entity.setDurationMs(entity.getEndTime().getTime() - entity.getStartTime().getTime());
                syncRecordRepository.save(entity);
            }

            // 从缓存中移除
            syncingTasks.remove(warehouseId);
        }

        return cancelled;
    }

    @Override
    @Transactional
    public CompletableFuture<String> retrySync(String recordId) {
        Optional<WarehouseSyncRecordEntity> optionalRecord = syncRecordRepository.findById(recordId);
        if (!optionalRecord.isPresent()) {
            throw new IllegalArgumentException("Sync record not found: " + recordId);
        }

        WarehouseSyncRecordEntity oldRecord = optionalRecord.get();
        if (!WarehouseSyncStatus.FAILED.equals(oldRecord.getStatus())) {
            throw new IllegalStateException("Can only retry failed sync records");
        }

        // 触发新的同步
        String newRecordId = triggerSyncSync(oldRecord.getWarehouseId(), WarehouseSyncTrigger.MANUAL);
        return CompletableFuture.completedFuture(newRecordId);
    }

    @Override
    public boolean isSyncing(String warehouseId) {
        // 先检查缓存
        if (syncingTasks.containsKey(warehouseId)) {
            return true;
        }

        // 再检查数据库（以防服务重启后缓存丢失）
        return syncRecordRepository.existsByWarehouseIdAndStatus(warehouseId, WarehouseSyncStatus.IN_PROGRESS);
    }

    @Override
    @Transactional
    public int cleanupSyncRecords(String warehouseId, int keepDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -keepDays);
        Date beforeDate = calendar.getTime();

        // 查询要删除的记录数
        long countBefore = syncRecordRepository.countByWarehouseIdAndTimeRange(
                warehouseId, new Date(0), beforeDate);

        // 删除记录
        int deletedCount = syncRecordRepository.deleteRecordsBeforeTime(beforeDate);

        log.info("Cleaned up {} sync records for warehouse: {}", deletedCount, warehouseId);
        return deletedCount;
    }

    @Override
    @Async
    public CompletableFuture<Void> batchTriggerSync(String[] warehouseIds, WarehouseSyncTrigger trigger) {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (String warehouseId : warehouseIds) {
            CompletableFuture<String> future = triggerSync(warehouseId, trigger);
            futures.add(future);
        }

        // 等待所有同步完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * 将实体转换为领域对象
     */
    private WarehouseSyncRecord convertToDomain(WarehouseSyncRecordEntity entity) {
        WarehouseSyncRecord record = new WarehouseSyncRecord();
        BeanUtils.copyProperties(entity, record);
        return record;
    }

    /**
     * 将领域对象转换为实体
     */
    private WarehouseSyncRecordEntity convertToEntity(WarehouseSyncRecord record) {
        WarehouseSyncRecordEntity entity = new WarehouseSyncRecordEntity();
        BeanUtils.copyProperties(record, entity);
        return entity;
    }
}