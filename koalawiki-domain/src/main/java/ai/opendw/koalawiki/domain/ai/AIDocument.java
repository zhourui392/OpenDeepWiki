package ai.opendw.koalawiki.domain.ai;

import lombok.Data;
import java.util.Date;

/**
 * AI生成的文档 - 领域实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
public class AIDocument {

    /**
     * 文档ID
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
     * 服务名称
     */
    private String serviceName;

    /**
     * 文档类别
     */
    private String docType;

    /**
     * 关联的提示词模板ID
     */
    private String promptTemplateId;

    /**
     * 源代码文件路径
     */
    private String sourceFile;

    /**
     * 文档标题
     */
    private String title;

    /**
     * Markdown格式文档内容
     */
    private String content;

    /**
     * 状态: GENERATING, COMPLETED, FAILED
     */
    private String status;

    /**
     * 使用的Agent类型: claude
     */
    private String agentType;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 扩展信息(JSON)
     */
    private String metadata;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 状态枚举
     */
    public enum Status {
        GENERATING,
        COMPLETED,
        FAILED
    }

    /**
     * Agent类型枚举
     */
    public enum AgentType {
        CLAUDE
    }
}
