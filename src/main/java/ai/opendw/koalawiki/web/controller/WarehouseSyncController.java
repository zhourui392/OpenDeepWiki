package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.core.service.IWarehouseSyncService;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import ai.opendw.koalawiki.infra.entity.WarehouseSyncRecordEntity;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.warehouse.SyncRecordDto;
import ai.opendw.koalawiki.web.dto.warehouse.SyncStatusResponse;
import ai.opendw.koalawiki.web.dto.warehouse.TriggerSyncRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 仓库同步控制器
 * 提供仓库同步相关的REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/warehouse/sync")
@RequiredArgsConstructor
@Validated
public class WarehouseSyncController {

    private final IWarehouseSyncService warehouseSyncService;

    /**
     * 触发仓库同步
     *
     * @param request 同步请求
     * @return 同步记录ID
     */
    @PostMapping("/trigger")
    public ResponseEntity<Result<String>> triggerSync(@Valid @RequestBody TriggerSyncRequest request) {
        log.info("触发仓库同步: warehouseId={}, forceSync={}",
            request.getWarehouseId(), request.isForceSync());

        try {
            CompletableFuture<String> future = warehouseSyncService.triggerSync(
                request.getWarehouseId(), request.isForceSync());

            // 等待任务提交完成（不等待同步完成）
            String syncRecordId = future.get();

            return ResponseEntity.ok(Result.success(syncRecordId,
                "同步任务已触发，记录ID: " + syncRecordId));

        } catch (Exception e) {
            log.error("触发同步失败: {}", request.getWarehouseId(), e);
            return ResponseEntity.ok(Result.error("触发同步失败: " + e.getMessage()));
        }
    }

    /**
     * 获取仓库同步状态
     *
     * @param warehouseId 仓库ID
     * @return 同步状态信息
     */
    @GetMapping("/status/{warehouseId}")
    public ResponseEntity<Result<SyncStatusResponse>> getSyncStatus(
            @PathVariable @NotBlank String warehouseId) {

        log.debug("查询仓库同步状态: {}", warehouseId);

        try {
            IWarehouseSyncService.SyncStatusInfo statusInfo =
                warehouseSyncService.getSyncStatus(warehouseId);

            // 转换为DTO
            SyncStatusResponse response = buildSyncStatusResponse(warehouseId, statusInfo);

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("查询同步状态失败: {}", warehouseId, e);
            return ResponseEntity.ok(Result.error("查询同步状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取同步记录列表
     *
     * @param warehouseId 仓库ID
     * @param page        页码（从0开始）
     * @param size        每页大小
     * @return 同步记录分页
     */
    @GetMapping("/records/{warehouseId}")
    public ResponseEntity<Result<Page<SyncRecordDto>>> getSyncRecords(
            @PathVariable @NotBlank String warehouseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("查询同步记录: warehouseId={}, page={}, size={}", warehouseId, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<WarehouseSyncRecordEntity> records =
                warehouseSyncService.getSyncRecords(warehouseId, pageable);

            // 转换为DTO
            Page<SyncRecordDto> dtoPage = records.map(this::convertToDto);

            return ResponseEntity.ok(Result.success(dtoPage));

        } catch (Exception e) {
            log.error("查询同步记录失败: {}", warehouseId, e);
            return ResponseEntity.ok(Result.error("查询同步记录失败: " + e.getMessage()));
        }
    }

    /**
     * 取消正在进行的同步
     *
     * @param warehouseId 仓库ID
     * @return 操作结果
     */
    @PostMapping("/cancel/{warehouseId}")
    public ResponseEntity<Result<Boolean>> cancelSync(
            @PathVariable @NotBlank String warehouseId) {

        log.info("取消仓库同步: {}", warehouseId);

        try {
            boolean cancelled = warehouseSyncService.cancelSync(warehouseId);

            if (cancelled) {
                return ResponseEntity.ok(Result.success(true, "同步已取消"));
            } else {
                return ResponseEntity.ok(Result.success(false, "没有正在进行的同步任务"));
            }

        } catch (Exception e) {
            log.error("取消同步失败: {}", warehouseId, e);
            return ResponseEntity.ok(Result.error("取消同步失败: " + e.getMessage()));
        }
    }

    /**
     * 重试失败的同步
     *
     * @param syncRecordId 同步记录ID
     * @return 新的同步记录ID
     */
    @PostMapping("/retry/{syncRecordId}")
    public ResponseEntity<Result<String>> retrySync(
            @PathVariable @NotBlank String syncRecordId) {

        log.info("重试同步: recordId={}", syncRecordId);

        try {
            CompletableFuture<String> future = warehouseSyncService.retrySync(syncRecordId);
            String newSyncRecordId = future.get();

            return ResponseEntity.ok(Result.success(newSyncRecordId,
                "重试已触发，新记录ID: " + newSyncRecordId));

        } catch (Exception e) {
            log.error("重试同步失败: {}", syncRecordId, e);
            return ResponseEntity.ok(Result.error("重试同步失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有正在进行的同步任务
     *
     * @return 正在同步的仓库列表
     */
    @GetMapping("/running")
    public ResponseEntity<Result<List<SyncRecordDto>>> getRunningSyncs() {
        log.debug("查询正在进行的同步任务");

        try {
            List<WarehouseSyncRecordEntity> runningRecords =
                warehouseSyncService.getSyncRecords(null, null).stream()
                    .filter(r -> r.getStatus() == WarehouseSyncStatus.IN_PROGRESS)
                    .collect(Collectors.toList());

            List<SyncRecordDto> dtos = runningRecords.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(dtos));

        } catch (Exception e) {
            log.error("查询运行中同步任务失败", e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 构建同步状态响应
     */
    private SyncStatusResponse buildSyncStatusResponse(String warehouseId,
                                                       IWarehouseSyncService.SyncStatusInfo statusInfo) {
        return SyncStatusResponse.builder()
            .warehouseId(warehouseId)
            .currentStatus(statusInfo.getCurrentStatus())
            .isSyncing(statusInfo.isSyncing())
            .currentSyncRecordId(statusInfo.getCurrentSyncRecordId())
            .currentProgress(statusInfo.getCurrentProgress())
            .lastSuccessSyncTime(statusInfo.getLastSuccessSyncTime())
            .lastSuccessVersion(statusInfo.getLastSuccessVersion())
            .totalSyncCount(statusInfo.getTotalSyncCount())
            .successCount(statusInfo.getSuccessCount())
            .failedCount(statusInfo.getFailedCount())
            .averageDuration(statusInfo.getAverageDuration())
            .build();
    }

    /**
     * 转换实体为DTO
     */
    private SyncRecordDto convertToDto(WarehouseSyncRecordEntity entity) {
        SyncRecordDto dto = new SyncRecordDto();
        dto.setId(entity.getId());
        dto.setWarehouseId(entity.getWarehouseId());
        dto.setStatus(entity.getStatus());
        dto.setTrigger(entity.getTrigger());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setFromVersion(entity.getFromVersion());
        dto.setToVersion(entity.getToVersion());
        dto.setAddedFileCount(entity.getAddedFileCount() != null ? entity.getAddedFileCount() : 0);
        dto.setModifiedFileCount(entity.getUpdatedFileCount() != null ? entity.getUpdatedFileCount() : 0);
        dto.setDeletedFileCount(entity.getDeletedFileCount() != null ? entity.getDeletedFileCount() : 0);
        dto.setErrorMessage(entity.getErrorMessage());
        return dto;
    }
}