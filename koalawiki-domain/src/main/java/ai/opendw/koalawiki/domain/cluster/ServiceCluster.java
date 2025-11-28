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
 * 服务集群聚合根
 * 管理多个仓库形成的微服务集群
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceCluster extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 集群名称
     */
    @NotBlank(message = "集群名称不能为空")
    @Size(max = 128, message = "集群名称长度不能超过128字符")
    private String name;

    /**
     * 集群编码（唯一标识）
     */
    @NotBlank(message = "集群编码不能为空")
    @Size(max = 64, message = "集群编码长度不能超过64字符")
    private String code;

    /**
     * 集群描述
     */
    @Size(max = 2000, message = "集群描述长度不能超过2000字符")
    private String description;

    /**
     * 技术栈描述
     */
    @Size(max = 255, message = "技术栈描述长度不能超过255字符")
    private String techStack;

    /**
     * 负责人
     */
    @Size(max = 64, message = "负责人长度不能超过64字符")
    private String owner;

    /**
     * 集群状态
     */
    private ClusterStatus status = ClusterStatus.ACTIVE;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 关联的仓库ID列表
     */
    private List<String> warehouseIds = new ArrayList<>();

    /**
     * 领域列表
     */
    private List<BusinessDomain> domains = new ArrayList<>();

    /**
     * 添加仓库到集群
     *
     * @param warehouseId 仓库ID
     */
    public void addWarehouse(String warehouseId) {
        if (warehouseId != null && !warehouseIds.contains(warehouseId)) {
            warehouseIds.add(warehouseId);
        }
    }

    /**
     * 从集群移除仓库
     *
     * @param warehouseId 仓库ID
     */
    public void removeWarehouse(String warehouseId) {
        warehouseIds.remove(warehouseId);
    }

    /**
     * 添加领域
     *
     * @param domain 领域
     */
    public void addDomain(BusinessDomain domain) {
        if (domain != null && !domains.contains(domain)) {
            domain.setClusterId(this.getId());
            domains.add(domain);
        }
    }

    /**
     * 获取仓库数量
     *
     * @return 仓库数量
     */
    public int getWarehouseCount() {
        return warehouseIds.size();
    }

    /**
     * 获取领域数量
     *
     * @return 领域数量
     */
    public int getDomainCount() {
        return domains.size();
    }

    /**
     * 判断集群是否活跃
     *
     * @return 是否活跃
     */
    public boolean isActive() {
        return ClusterStatus.ACTIVE.equals(status);
    }

    /**
     * 归档集群
     */
    public void archive() {
        this.status = ClusterStatus.ARCHIVED;
        this.updatedAt = new Date();
    }

    /**
     * 激活集群
     */
    public void activate() {
        this.status = ClusterStatus.ACTIVE;
        this.updatedAt = new Date();
    }
}
