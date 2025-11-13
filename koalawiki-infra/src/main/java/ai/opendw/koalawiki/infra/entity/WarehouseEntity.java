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
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_created_time", columnList = "createdTime")
    })
public class WarehouseEntity extends BaseJpaEntity {

    /**
     * 仓库名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 仓库URL
     */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * 仓库描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 仓库状态
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WarehouseStatus status;

    /**
     * 分类类型
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ClassifyType classifyType;

    /**
     * 创建用户ID
     */
    @Column(nullable = false, length = 50)
    private String userId;

    /**
     * 是否启用自动同步
     */
    @Column(nullable = false)
    private Boolean autoSync = false;

    /**
     * 同步间隔（小时）
     */
    private Integer syncInterval = 24;

    /**
     * 最后同步时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSyncTime;

    /**
     * 访问次数
     */
    private Long viewCount = 0L;

    /**
     * 星标数
     */
    private Long starCount = 0L;

    /**
     * 是否公开
     */
    @Column(nullable = false)
    private Boolean isPublic = true;

    /**
     * 认证用户名（私有仓库）
     */
    @Column(length = 100)
    private String authUsername;

    /**
     * 认证密码/令牌（私有仓库）
     */
    @Column(length = 500)
    private String authPassword;

    /**
     * 默认分支
     */
    @Column(length = 100)
    private String defaultBranch = "main";

    /**
     * 仓库语言
     */
    @Column(length = 50)
    private String language;

    /**
     * 标签（JSON数组）
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseStatus status) {
        this.status = status;
    }

    public ClassifyType getClassifyType() {
        return classifyType;
    }

    public void setClassifyType(ClassifyType classifyType) {
        this.classifyType = classifyType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getAutoSync() {
        return autoSync;
    }

    public void setAutoSync(Boolean autoSync) {
        this.autoSync = autoSync;
    }

    public Integer getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(Integer syncInterval) {
        this.syncInterval = syncInterval;
    }

    public Date getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(Date lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getStarCount() {
        return starCount;
    }

    public void setStarCount(Long starCount) {
        this.starCount = starCount;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}