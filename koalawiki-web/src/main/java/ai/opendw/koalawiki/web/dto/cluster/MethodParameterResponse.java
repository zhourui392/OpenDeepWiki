package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

/**
 * 方法参数响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class MethodParameterResponse {

    /**
     * 参数名
     */
    private String name;

    /**
     * 参数类型
     */
    private String type;

    /**
     * 参数描述
     */
    private String description;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 参数索引
     */
    private Integer index;
}
