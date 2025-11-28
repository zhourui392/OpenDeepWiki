package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Dubbo方法响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class DubboMethodResponse {

    /**
     * 方法名
     */
    private String name;

    /**
     * 返回类型
     */
    private String returnType;

    /**
     * 参数列表
     */
    private List<MethodParameterResponse> parameters;

    /**
     * 方法描述
     */
    private String description;

    /**
     * 方法签名
     */
    private String signature;

    /**
     * 是否废弃
     */
    private Boolean deprecated;

    /**
     * 异常列表
     */
    private List<String> exceptions;
}
