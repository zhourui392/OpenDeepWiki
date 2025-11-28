package ai.opendw.koalawiki.domain.cluster;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 业务领域聚合
 * 按O2O业务划分（如：交易域、商品域、履约域）
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessDomain extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 所属集群ID
     */
    @NotBlank(message = "集群ID不能为空")
    private String clusterId;

    /**
     * 领域名称
     */
    @NotBlank(message = "领域名称不能为空")
    @Size(max = 64, message = "领域名称长度不能超过64字符")
    private String name;

    /**
     * 领域编码
     */
    @NotBlank(message = "领域编码不能为空")
    @Size(max = 32, message = "领域编码长度不能超过32字符")
    private String code;

    /**
     * 领域描述
     */
    @Size(max = 500, message = "领域描述长度不能超过500字符")
    private String description;

    /**
     * 领域负责人
     */
    @Size(max = 64, message = "负责人长度不能超过64字符")
    private String owner;

    /**
     * 显示颜色
     */
    @Size(max = 20, message = "颜色长度不能超过20字符")
    private String color = "#1890ff";

    /**
     * 排序权重
     */
    private Integer sortOrder = 0;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 领域内服务列表
     */
    private List<ServiceInfo> services = new ArrayList<>();

    /**
     * 添加服务到领域
     *
     * @param service 服务信息
     */
    public void addService(ServiceInfo service) {
        if (service != null && !services.contains(service)) {
            service.setDomainId(this.getId());
            services.add(service);
        }
    }

    /**
     * 移除服务
     *
     * @param warehouseId 仓库ID
     */
    public void removeService(String warehouseId) {
        services.removeIf(s -> warehouseId.equals(s.getWarehouseId()));
    }

    /**
     * 获取服务数量
     *
     * @return 服务数量
     */
    public int getServiceCount() {
        return services.size();
    }

    /**
     * 判断是否为未分类领域
     *
     * @return 是否未分类
     */
    public boolean isUnclassified() {
        return "unclassified".equals(code);
    }
}
