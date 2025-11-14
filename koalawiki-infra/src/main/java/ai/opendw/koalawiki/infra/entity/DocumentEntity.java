package ai.opendw.koalawiki.infra.entity;

import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;

import javax.persistence.*;
import java.util.Date;

/**
 * 文档JPA实体
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Entity
@Table(name = "documents",
    indexes = {
        @Index(name = "idx_document_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_document_status", columnList = "status"),
        @Index(name = "idx_document_last_update", columnList = "last_update")
    })
public class DocumentEntity extends BaseJpaEntity {

    /**
     * 关联仓库ID
     */
    @Column(name = "warehouse_id", nullable = false, length = 50)
    private String warehouseId;

    /**
     * 最后更新时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update")
    private Date lastUpdate;

    /**
     * 文档介绍
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 点赞数
     */
    @Column(name = "like_count")
    private Long likeCount = 0L;

    /**
     * 评论数
     */
    @Column(name = "comment_count")
    private Long commentCount = 0L;

    /**
     * 本地git仓库地址
     */
    @Column(name = "git_path", length = 500)
    private String gitPath;

    /**
     * 仓库状态
     */
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private WarehouseStatus status;

    // Getters and Setters

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    public String getGitPath() {
        return gitPath;
    }

    public void setGitPath(String gitPath) {
        this.gitPath = gitPath;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseStatus status) {
        this.status = status;
    }
}
