package ai.opendw.koalawiki.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 仓库同步完成事件
 * 当仓库同步成功完成时发布此事件
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Getter
public class WarehouseSyncCompletedEvent extends ApplicationEvent {

    /**
     * 仓库ID
     */
    private final String warehouseId;

    /**
     * 同步记录ID
     */
    private final String syncRecordId;

    /**
     * 本地仓库路径
     */
    private final String localPath;

    /**
     * 同步到的版本号(commit hash)
     */
    private final String toVersion;

    /**
     * 同步的文件数量
     */
    private final int fileCount;

    /**
     * 构造函数
     *
     * @param source        事件源
     * @param warehouseId   仓库ID
     * @param syncRecordId  同步记录ID
     * @param localPath     本地仓库路径
     * @param toVersion     同步到的版本号
     * @param fileCount     同步的文件数量
     */
    public WarehouseSyncCompletedEvent(Object source, String warehouseId, String syncRecordId,
                                       String localPath, String toVersion, int fileCount) {
        super(source);
        this.warehouseId = warehouseId;
        this.syncRecordId = syncRecordId;
        this.localPath = localPath;
        this.toVersion = toVersion;
        this.fileCount = fileCount;
    }
}
