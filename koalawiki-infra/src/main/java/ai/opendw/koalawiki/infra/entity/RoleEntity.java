package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;

/**
 * 角色JPA实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name")
})
public class RoleEntity extends BaseJpaEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * 角色描述
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 是否启用
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * 是否为系统角色
     */
    @Column(name = "is_system_role", nullable = false)
    private Boolean isSystemRole = false;
}
