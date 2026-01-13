package ai.opendw.koalawiki.domain.ai;

import lombok.Data;
import java.util.Date;

/**
 * 文档生成任务 - 领域实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
public class GenerationTask {

    /**
     * 任务ID
     */
    private String id;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 服务ID
     */
    private String serviceId;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 状态: PENDING, RUNNING, COMPLETED, FAILED
     */
    private String status;

    /**
     * 总文件数
     */
    private Integer totalFiles;

    /**
     * 已完成文件数
     */
    private Integer completedFiles;

    /**
     * 失败文件数
     */
    private Integer failedFiles;

    /**
     * 使用的Agent类型
     */
    private String agentType;

    /**
     * 开始时间
     */
    private Date startedAt;

    /**
     * 完成时间
     */
    private Date completedAt;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 获取进度百分比
     */
    public double getProgress() {
        if (totalFiles == null || totalFiles == 0) {
            return 0.0;
        }
        int completed = completedFiles != null ? completedFiles : 0;
        return (double) completed / totalFiles * 100;
    }

    /**
     * 状态枚举
     */
    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
