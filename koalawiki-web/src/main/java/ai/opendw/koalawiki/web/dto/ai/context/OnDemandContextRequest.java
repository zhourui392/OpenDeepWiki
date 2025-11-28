package ai.opendw.koalawiki.web.dto.ai.context;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 按需上下文请求DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class OnDemandContextRequest {

    /**
     * 集群ID
     */
    @NotBlank(message = "集群ID不能为空")
    private String clusterId;

    /**
     * 查询文本
     */
    @NotBlank(message = "查询文本不能为空")
    private String query;

    /**
     * 提示信息（已知相关的服务ID）
     */
    private List<String> hints;

    /**
     * 最大上下文大小（KB）
     */
    private Integer maxSizeKb = 30;
}
