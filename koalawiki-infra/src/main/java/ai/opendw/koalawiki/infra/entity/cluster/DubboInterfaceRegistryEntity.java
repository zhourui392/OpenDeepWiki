package ai.opendw.koalawiki.infra.entity.cluster;

import ai.opendw.koalawiki.infra.entity.BaseJpaEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Dubbo接口注册JPA实体
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Entity
@Table(name = "dubbo_interface_registry",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_interface", columnNames = {"cluster_id", "interface_name", "version", "group_name"})
    },
    indexes = {
        @Index(name = "idx_cluster_id", columnList = "cluster_id"),
        @Index(name = "idx_provider_warehouse", columnList = "provider_warehouse_id"),
        @Index(name = "idx_interface_name", columnList = "interface_name"),
        @Index(name = "idx_deprecated", columnList = "deprecated"),
        @Index(name = "idx_created_at", columnList = "created_at")
    })
public class DubboInterfaceRegistryEntity extends BaseJpaEntity {

    /**
     * 所属集群ID
     */
    @Column(name = "cluster_id", nullable = false, length = 36)
    private String clusterId;

    /**
     * 接口全限定名
     */
    @Column(name = "interface_name", nullable = false, length = 255)
    private String interfaceName;

    /**
     * 接口版本
     */
    @Column(name = "version", nullable = false, length = 32)
    private String version = "";

    /**
     * 接口分组
     */
    @Column(name = "group_name", nullable = false, length = 64)
    private String groupName = "";

    /**
     * 提供者仓库ID
     */
    @Column(name = "provider_warehouse_id", length = 36)
    private String providerWarehouseId;

    /**
     * 提供者服务名
     */
    @Column(name = "provider_service_name", length = 128)
    private String providerServiceName;

    /**
     * 接口描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 方法列表（JSON格式）
     */
    @Column(name = "methods", columnDefinition = "JSON")
    private String methods;

    /**
     * 是否废弃
     */
    @Column(name = "deprecated", nullable = false)
    private Boolean deprecated = false;

    /**
     * 废弃原因
     */
    @Column(name = "deprecated_reason", length = 500)
    private String deprecatedReason;

    /**
     * 源文件路径
     */
    @Column(name = "source_file", length = 500)
    private String sourceFile;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getProviderWarehouseId() {
        return providerWarehouseId;
    }

    public void setProviderWarehouseId(String providerWarehouseId) {
        this.providerWarehouseId = providerWarehouseId;
    }

    public String getProviderServiceName() {
        return providerServiceName;
    }

    public void setProviderServiceName(String providerServiceName) {
        this.providerServiceName = providerServiceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMethods() {
        return methods;
    }

    public void setMethods(String methods) {
        this.methods = methods;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDeprecatedReason() {
        return deprecatedReason;
    }

    public void setDeprecatedReason(String deprecatedReason) {
        this.deprecatedReason = deprecatedReason;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
