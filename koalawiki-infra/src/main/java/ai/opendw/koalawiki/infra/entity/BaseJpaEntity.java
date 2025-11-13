package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Date;

/**
 * JPA基础实体类
 * 所有JPA实体都应该继承这个类
 */
@Data
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
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;
}
