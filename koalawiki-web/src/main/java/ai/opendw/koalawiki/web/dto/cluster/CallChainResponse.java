package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

/**
 * 调用链响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class CallChainResponse {

    /**
     * 接口ID
     */
    private String interfaceId;

    /**
     * 接口名
     */
    private String interfaceName;

    /**
     * 提供者服务名
     */
    private String providerServiceName;

    /**
     * 上游调用者数量
     */
    private int upstreamCount;

    /**
     * 下游被调用数量
     */
    private int downstreamCount;

    /**
     * Mermaid图代码
     */
    private String mermaidCode;
}
