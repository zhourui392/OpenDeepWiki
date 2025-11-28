package ai.opendw.koalawiki.web.dto.ai.context;

import ai.opendw.koalawiki.core.ai.context.AIContext;
import ai.opendw.koalawiki.domain.cluster.ContextLevel;
import lombok.Data;

import java.util.Date;

/**
 * AI上下文响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class AIContextResponse {

    /**
     * 集群ID
     */
    private String clusterId;

    /**
     * 目标服务ID
     */
    private String targetServiceId;

    /**
     * 上下文级别
     */
    private ContextLevel contextLevel;

    /**
     * 上下文内容（JSON或Markdown）
     */
    private Object context;

    /**
     * Markdown格式内容
     */
    private String markdown;

    /**
     * 预估大小（字符数）
     */
    private int estimatedSize;

    /**
     * 预估大小（KB）
     */
    private int estimatedSizeKb;

    /**
     * 是否已压缩
     */
    private boolean compressed;

    /**
     * 生成时间
     */
    private Date generatedAt;

    /**
     * 从AIContext构建响应
     *
     * @param context       上下文
     * @param includeMarkdown 是否包含Markdown
     * @param compressed    是否压缩
     * @return 响应DTO
     */
    public static AIContextResponse from(AIContext context, boolean includeMarkdown, boolean compressed) {
        AIContextResponse response = new AIContextResponse();
        response.setClusterId(context.getClusterId());
        response.setTargetServiceId(context.getTargetServiceId());
        response.setContextLevel(context.getContextLevel());
        response.setContext(context);
        response.setEstimatedSize(context.getEstimatedSize());
        response.setEstimatedSizeKb(context.getEstimatedSize() / 1024);
        response.setCompressed(compressed);
        response.setGeneratedAt(context.getGeneratedAt());

        if (includeMarkdown) {
            response.setMarkdown(context.toMarkdown());
        }

        return response;
    }
}
