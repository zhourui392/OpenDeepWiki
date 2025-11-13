package ai.opendw.koalawiki.web.dto.warehouse;

import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncTrigger;
import lombok.Data;

import java.util.Date;

/**
 * 仓库同步记录DTO
 */
@Data
public class SyncRecordDto {

    /**
     * 记录ID
     */
    private String id;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 同步状态
     */
    private WarehouseSyncStatus status;

    /**
     * 触发方式
     */
    private WarehouseSyncTrigger trigger;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 同步前版本
     */
    private String fromVersion;

    /**
     * 同步后版本
     */
    private String toVersion;

    /**
     * 新增文件数
     */
    private int addedFileCount;

    /**
     * 修改文件数
     */
    private int modifiedFileCount;

    /**
     * 删除文件数
     */
    private int deletedFileCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 同步耗时（毫秒）
     */
    private Long duration;

    /**
     * 同步进度（0-100）
     */
    private Integer progress;

    /**
     * 当前处理的文件
     */
    private String currentFile;

    /**
     * 获取同步耗时（毫秒）
     */
    public Long getDuration() {
        if (startTime != null && endTime != null) {
            return endTime.getTime() - startTime.getTime();
        }
        if (startTime != null && status == WarehouseSyncStatus.IN_PROGRESS) {
            return System.currentTimeMillis() - startTime.getTime();
        }
        return null;
    }
}