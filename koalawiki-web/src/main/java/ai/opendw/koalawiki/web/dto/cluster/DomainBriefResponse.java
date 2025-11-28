package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

/**
 * 领域简要响应
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class DomainBriefResponse {

    /**
     * 领域ID
     */
    private String id;

    /**
     * 领域名称
     */
    private String name;

    /**
     * 领域编码
     */
    private String code;

    /**
     * 显示颜色
     */
    private String color;

    /**
     * 服务数量
     */
    private Integer serviceCount;
}
