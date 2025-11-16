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
     * 使用的Agent类型: claude, codex
     */
    private String agentType;

    /**
     * 错误信息
     */
    private String errorMessage;

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
        CLAUDE,
        CODEX
    }
}
