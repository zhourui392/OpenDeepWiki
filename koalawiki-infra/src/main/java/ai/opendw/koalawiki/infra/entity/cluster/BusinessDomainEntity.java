package ai.opendw.koalawiki.infra.entity.cluster;

import ai.opendw.koalawiki.infra.entity.BaseJpaEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * 业务领域JPA实体
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Entity
@Table(name = "business_domain",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cluster_code", columnNames = {"cluster_id", "code"})
    },
    indexes = {
        @Index(name = "idx_cluster_id", columnList = "cluster_id"),
        @Index(name = "idx_code", columnList = "code"),
        @Index(name = "idx_sort_order", columnList = "sort_order"),
        @Index(name = "idx_deleted_at", columnList = "deleted_at")
    })
public class BusinessDomainEntity extends BaseJpaEntity {

    /**
     * 所属集群ID
     */
    @Column(name = "cluster_id", nullable = false, length = 36)
    private String clusterId;

    /**
     * 领域名称
     */
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    /**
     * 领域编码
     */
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    /**
     * 领域描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 领域负责人
     */
    @Column(name = "owner", length = 64)
    private String owner;

    /**
     * 显示颜色
     */
    @Column(name = "color", length = 20)
    private String color = "#1890ff";

    /**
     * 排序权重
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    /**
     * 删除时间（软删除）
     */
    @Column(name = "deleted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deletedAt;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }
}
