package ai.opendw.koalawiki.infra.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * JPA基础实体类
 * 所有JPA实体都应该继承这个类
 */
@MappedSuperclass
public abstract class BaseJpaEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID (String类型)
     */
    @Id
    @Column(length = 50)
    private String id;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
