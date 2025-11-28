package ai.opendw.koalawiki.web.dto.cluster;

import ai.opendw.koalawiki.domain.cluster.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 领域响应
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class DomainResponse {

    /**
     * 领域ID
     */
    private String id;

    /**
     * 所属集群ID
     */
    private String clusterId;

    /**
     * 领域名称
     */
    private String name;

    /**
     * 领域编码
     */
    private String code;

    /**
     * 领域描述
     */
    private String description;

    /**
     * 领域负责人
     */
    private String owner;

    /**
     * 显示颜色
     */
    private String color;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 服务数量
     */
    private Integer serviceCount;

    /**
     * 服务列表
     */
    private List<ServiceInfoResponse> services;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 服务信息响应
     */
    @Data
    @Builder
    public static class ServiceInfoResponse {

        /**
         * 映射ID
         */
        private String id;

        /**
         * 仓库ID
         */
        private String warehouseId;

        /**
         * 服务名称
         */
        private String serviceName;

        /**
         * 服务类型
         */
        private ServiceType type;

        /**
         * 服务描述
         */
        private String description;

        /**
         * 是否主领域
         */
        private Boolean isPrimary;

        /**
         * 排序权重
         */
        private Integer sortOrder;
    }
}
