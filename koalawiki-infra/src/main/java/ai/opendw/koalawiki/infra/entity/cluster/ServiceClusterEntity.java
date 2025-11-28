package ai.opendw.koalawiki.infra.entity.cluster;

import ai.opendw.koalawiki.domain.cluster.ClusterStatus;
import ai.opendw.koalawiki.infra.entity.BaseJpaEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * 服务集群JPA实体
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Entity
@Table(name = "service_cluster",
    indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_owner", columnList = "owner"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_deleted_at", columnList = "deleted_at")
    })
public class ServiceClusterEntity extends BaseJpaEntity {

    /**
     * 集群名称
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * 集群编码
     */
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    /**
     * 集群描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 技术栈描述
     */
    @Column(name = "tech_stack", length = 255)
    private String techStack;

    /**
     * 负责人
     */
    @Column(name = "owner", length = 64)
    private String owner;

    /**
     * 状态
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ClusterStatus status = ClusterStatus.ACTIVE;

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

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ClusterStatus getStatus() {
        return status;
    }

    public void setStatus(ClusterStatus status) {
        this.status = status;
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
