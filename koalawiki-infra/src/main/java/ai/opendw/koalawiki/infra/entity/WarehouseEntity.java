package ai.opendw.koalawiki.infra.entity;

import ai.opendw.koalawiki.domain.ClassifyType;
import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;

import javax.persistence.*;
import java.util.Date;

/**
 * 仓库JPA实体
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Entity
@Table(name = "warehouses",
    indexes = {
        @Index(name = "idx_name", columnList = "name"),
        @Index(name = "idx_organization", columnList = "organization_name"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_classify", columnList = "classify"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_is_recommended", columnList = "is_recommended")
    })
public class WarehouseEntity extends BaseJpaEntity {

    /**
     * 组织名称
     */
    @Column(name = "organization_name", length = 100)
    private String organizationName;

    /**
     * 仓库名称
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 仓库描述
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * 仓库地址（Git URL）
     */
    @Column(name = "address", nullable = false, length = 500)
    private String address;

    /**
     * 私有化Git账号
     */
    @Column(name = "git_user_name", length = 100)
    private String gitUserName;

    /**
     * 私有化Git密码
     */
    @Column(name = "git_password", length = 200)
    private String gitPassword;

    /**
     * 私有化Git邮箱
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 仓库类型
     */
    @Column(name = "type", length = 50)
    private String type;

    /**
     * 仓库分支
     */
    @Column(name = "branch", length = 100)
    private String branch = "master";

    /**
     * 仓库状态
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WarehouseStatus status = WarehouseStatus.PENDING;

    /**
     * 错误信息
     */
    @Column(name = "error", length = 2000)
    private String error;

    /**
     * 仓库版本
     */
    @Column(name = "version", length = 50)
    private String version;

    /**
     * 是否嵌入完成
     */
    @Column(name = "is_embedded", nullable = false)
    private Boolean isEmbedded = false;

    /**
     * 是否推荐
     */
    @Column(name = "is_recommended", nullable = false)
    private Boolean isRecommended = false;

    /**
     * 仓库类别
     */
    @Column(name = "classify", length = 50)
    @Enumerated(EnumType.STRING)
    private ClassifyType classify;

    /**
     * Star数量
     */
    @Column(name = "stars", nullable = false)
    private Integer stars = 0;

    /**
     * Fork数量
     */
    @Column(name = "forks", nullable = false)
    private Integer forks = 0;

    /**
     * 创建用户ID
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 是否启用同步
     */
    @Column(name = "enable_sync", nullable = false)
    private Boolean enableSync = true;

    /**
     * 删除时间（软删除）
     */
    @Column(name = "deleted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deletedAt;

    // Getters and Setters

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGitUserName() {
        return gitUserName;
    }

    public void setGitUserName(String gitUserName) {
        this.gitUserName = gitUserName;
    }

    public String getGitPassword() {
        return gitPassword;
    }

    public void setGitPassword(String gitPassword) {
        this.gitPassword = gitPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseStatus status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getIsEmbedded() {
        return isEmbedded;
    }

    public void setIsEmbedded(Boolean isEmbedded) {
        this.isEmbedded = isEmbedded;
    }

    public Boolean getIsRecommended() {
        return isRecommended;
    }

    public void setIsRecommended(Boolean isRecommended) {
        this.isRecommended = isRecommended;
    }

    public ClassifyType getClassify() {
        return classify;
    }

    public void setClassify(ClassifyType classify) {
        this.classify = classify;
    }

    public Integer getStars() {
        return stars;
    }

    public void setStars(Integer stars) {
        this.stars = stars;
    }

    public Integer getForks() {
        return forks;
    }

    public void setForks(Integer forks) {
        this.forks = forks;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getEnableSync() {
        return enableSync;
    }

    public void setEnableSync(Boolean enableSync) {
        this.enableSync = enableSync;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }
}
