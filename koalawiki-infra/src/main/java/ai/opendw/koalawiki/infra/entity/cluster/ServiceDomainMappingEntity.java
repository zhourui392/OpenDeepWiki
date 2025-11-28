package ai.opendw.koalawiki.infra.entity.cluster;

import ai.opendw.koalawiki.domain.cluster.ServiceType;
import ai.opendw.koalawiki.infra.entity.BaseJpaEntity;

import javax.persistence.*;

/**
 * 服务领域映射JPA实体
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Entity
@Table(name = "service_domain_mapping",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_domain_warehouse", columnNames = {"domain_id", "warehouse_id"})
    },
    indexes = {
        @Index(name = "idx_domain_id", columnList = "domain_id"),
        @Index(name = "idx_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_service_type", columnList = "service_type"),
        @Index(name = "idx_is_primary", columnList = "is_primary")
    })
public class ServiceDomainMappingEntity extends BaseJpaEntity {

    /**
     * 领域ID
     */
    @Column(name = "domain_id", nullable = false, length = 36)
    private String domainId;

    /**
     * 仓库ID
     */
    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    /**
     * 服务名称
     */
    @Column(name = "service_name", length = 128)
    private String serviceName;

    /**
     * 服务类型
     */
    @Column(name = "service_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType = ServiceType.PROVIDER;

    /**
     * 服务描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 排序权重
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * 是否主领域
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
