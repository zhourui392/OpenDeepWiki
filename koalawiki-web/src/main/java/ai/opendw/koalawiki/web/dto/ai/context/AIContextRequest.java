package ai.opendw.koalawiki.web.dto.ai.context;

import ai.opendw.koalawiki.domain.cluster.ContextLevel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * AI上下文请求DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class AIContextRequest {

    /**
     * 集群ID
     */
    @NotBlank(message = "集群ID不能为空")
    private String clusterId;

    /**
     * 目标服务ID（可选）
     */
    private String targetServiceId;

    /**
     * 上下文级别
     */
    private ContextLevel contextLevel = ContextLevel.L2;

    /**
     * 最大上下文大小（KB）
     */
    private Integer maxSizeKb = 50;

    /**
     * 是否压缩
     */
    private Boolean compress = true;

    /**
     * 输出格式
     */
    private OutputFormat format = OutputFormat.JSON;

    /**
     * 输出格式枚举
     */
    public enum OutputFormat {
        /**
         * JSON格式
         */
        JSON,

        /**
         * Markdown格式
         */
        MARKDOWN
    }
}
