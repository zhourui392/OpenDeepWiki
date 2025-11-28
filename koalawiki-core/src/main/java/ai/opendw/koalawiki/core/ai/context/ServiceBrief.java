package ai.opendw.koalawiki.core.ai.context;

import ai.opendw.koalawiki.domain.cluster.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务简要信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBrief {

    /**
     * 服务ID（仓库ID）
     */
    private String id;

    /**
     * 服务名称
     */
    private String name;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 所属领域ID
     */
    private String domainId;

    /**
     * 所属领域名称
     */
    private String domainName;

    /**
     * 服务类型
     */
    private ServiceType serviceType;

    /**
     * 提供的接口数量
     */
    private int interfaceCount;

    /**
     * 依赖的服务数量
     */
    private int dependencyCount;

    /**
     * 被依赖的服务数量
     */
    private int dependentCount;
}
