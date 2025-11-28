package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

/**
 * 集群统计响应
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class ClusterStatisticsResponse {

    /**
     * 集群ID
     */
    private String clusterId;

    /**
     * 仓库数量
     */
    private long warehouseCount;

    /**
     * 领域数量
     */
    private long domainCount;

    /**
     * 接口数量
     */
    private long interfaceCount;

    /**
     * 文档数量
     */
    private long documentCount;
}
