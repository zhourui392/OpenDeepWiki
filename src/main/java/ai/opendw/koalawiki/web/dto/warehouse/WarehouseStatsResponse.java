package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仓库统计信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStatsResponse {

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 总文件数
     */
    private Integer totalFiles;

    /**
     * 文档文件数（md文件）
     */
    private Integer documentFiles;

    /**
     * 总文件大小（字节）
     */
    private Long totalSize;

    /**
     * 目录数
     */
    private Integer catalogCount;

    /**
     * 文档项数
     */
    private Integer documentItemCount;

    /**
     * 最后同步时间
     */
    private Long lastSyncTime;

    /**
     * 版本号（最新commit ID）
     */
    private String version;

    /**
     * 仓库状态
     */
    private String status;

    /**
     * 浏览次数
     */
    private Long viewCount;

    /**
     * 星标数
     */
    private Integer starCount;
}
