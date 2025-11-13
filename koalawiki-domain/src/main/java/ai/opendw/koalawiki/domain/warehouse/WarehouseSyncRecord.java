package ai.opendw.koalawiki.domain.warehouse;

import ai.opendw.koalawiki.domain.BaseEntity;

import java.util.Date;

/**
 * 仓库同步记录实体
 * 记录每次仓库同步的详细信息
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
public class WarehouseSyncRecord extends BaseEntity {

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 同步状态
     */
    private WarehouseSyncStatus status;

    /**
     * 同步开始时间
     */
    private Date startTime;

    /**
     * 同步结束时间
     */
    private Date endTime;

    /**
     * 起始版本（Git commit ID）
     */
    private String fromVersion;

    /**
     * 目标版本（Git commit ID）
     */
    private String toVersion;

    /**
     * 错误消息（同步失败时）
     */
    private String errorMessage;

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
    private WarehouseSyncTrigger trigger;

    /**
     * 同步耗时（毫秒）
     */
    private Long duration;

    /**
     * 同步详情（JSON格式）
     */
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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * 计算同步耗时
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            this.duration = endTime.getTime() - startTime.getTime();
        }
    }

    /**
     * 判断是否同步成功
     */
    public boolean isSuccess() {
        return WarehouseSyncStatus.SUCCESS.equals(status);
    }

    /**
     * 判断是否同步失败
     */
    public boolean isFailed() {
        return WarehouseSyncStatus.FAILED.equals(status);
    }

    /**
     * 判断是否正在同步
     */
    public boolean isInProgress() {
        return WarehouseSyncStatus.IN_PROGRESS.equals(status);
    }
}