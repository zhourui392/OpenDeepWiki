package ai.opendw.koalawiki.infra.entity;

import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncTrigger;

import javax.persistence.*;
import java.util.Date;

/**
 * 仓库同步记录JPA实体
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Entity
@Table(name = "warehouse_sync_records",
    indexes = {
        @Index(name = "idx_sync_records_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_sync_records_status", columnList = "status"),
        @Index(name = "idx_sync_records_start_time", columnList = "start_time"),
        @Index(name = "idx_sync_records_warehouse_status", columnList = "warehouse_id,status")
    })
public class WarehouseSyncRecordEntity extends BaseJpaEntity {

    /**
     * 仓库ID
     */
    @Column(name = "warehouse_id", nullable = false, length = 50)
    private String warehouseId;

    /**
     * 同步状态
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WarehouseSyncStatus status;

    /**
     * 同步开始时间
     */
    @Column(name = "start_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    /**
     * 同步结束时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    /**
     * 起始版本（Git commit ID）
     */
    @Column(length = 40)
    private String fromVersion;

    /**
     * 目标版本（Git commit ID）
     */
    @Column(length = 40)
    private String toVersion;

    /**
     * 错误消息（同步失败时）
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 错误堆栈（同步失败时）
     */
    @Column(columnDefinition = "TEXT")
    private String errorStack;

    /**
     * 同步进度（0-100）
     */
    private Integer progress;

    /**
     * 总文件数
     */
    private Integer fileCount;

    /**
     * 更新的文件数
     */
    private Integer updatedFileCount;

    /**
     * 新增的文件数
     */
    private Integer addedFileCount;

    /**
     * 删除的文件数
     */
    private Integer deletedFileCount;

    /**
     * 触发方式
     */
    @Column(name = "`trigger`", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WarehouseSyncTrigger trigger;

    /**
     * 同步耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 同步详情（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    // Getters and Setters

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public WarehouseSyncStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseSyncStatus status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(String fromVersion) {
        this.fromVersion = fromVersion;
    }

    public String getToVersion() {
        return toVersion;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStack() {
        return errorStack;
    }

    public void setErrorStack(String errorStack) {
        this.errorStack = errorStack;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public Integer getUpdatedFileCount() {
        return updatedFileCount;
    }

    public void setUpdatedFileCount(Integer updatedFileCount) {
        this.updatedFileCount = updatedFileCount;
    }

    public Integer getAddedFileCount() {
        return addedFileCount;
    }

    public void setAddedFileCount(Integer addedFileCount) {
        this.addedFileCount = addedFileCount;
    }

    public Integer getDeletedFileCount() {
        return deletedFileCount;
    }

    public void setDeletedFileCount(Integer deletedFileCount) {
        this.deletedFileCount = deletedFileCount;
    }

    public WarehouseSyncTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(WarehouseSyncTrigger trigger) {
        this.trigger = trigger;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}