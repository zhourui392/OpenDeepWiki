package ai.opendw.koalawiki.web.dto.cluster;

import ai.opendw.koalawiki.domain.cluster.ClusterStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 集群响应
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class ClusterResponse {

    /**
     * 集群ID
     */
    private String id;

    /**
     * 集群名称
     */
    private String name;

    /**
     * 集群编码
     */
    private String code;

    /**
     * 集群描述
     */
    private String description;

    /**
     * 技术栈描述
     */
    private String techStack;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 集群状态
     */
    private ClusterStatus status;

    /**
     * 仓库数量
     */
    private Integer warehouseCount;

    /**
     * 领域数量
     */
    private Integer domainCount;

    /**
     * 领域列表
     */
    private List<DomainBriefResponse> domains;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
