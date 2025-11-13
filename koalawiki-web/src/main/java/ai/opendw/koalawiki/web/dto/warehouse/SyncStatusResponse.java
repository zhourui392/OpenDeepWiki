package ai.opendw.koalawiki.web.dto.warehouse;

import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 同步状态响应DTO
 */
@Data
@Builder
public class SyncStatusResponse {

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 当前同步状态
     */
    private WarehouseSyncStatus currentStatus;

    /**
     * 是否正在同步
     */
    private boolean isSyncing;

    /**
     * 当前同步记录ID（如果正在同步）
     */
    private String currentSyncRecordId;

    /**
     * 当前同步进度（0-100）
     */
    private Integer currentProgress;

    /**
     * 当前处理的文件
     */
    private String currentFile;

    /**
     * 当前同步开始时间
     */
    private Date currentSyncStartTime;

    /**
     * 最后成功同步时间
     */
    private Date lastSuccessSyncTime;

    /**
     * 最后成功同步的版本
     */
    private String lastSuccessVersion;

    /**
     * 总同步次数
     */
    private long totalSyncCount;

    /**
     * 成功次数
     */
    private long successCount;

    /**
     * 失败次数
     */
    private long failedCount;

    /**
     * 平均同步耗时（毫秒）
     */
    private Long averageDuration;

    /**
     * 最近的同步记录列表
     */
    private List<SyncRecordDto> recentRecords;

    /**
     * 下次自动同步时间（如果启用）
     */
    private Date nextAutoSyncTime;

    /**
     * 是否启用自动同步
     */
    private boolean autoSyncEnabled;

    /**
     * 自动同步间隔（分钟）
     */
    private Integer autoSyncInterval;
}