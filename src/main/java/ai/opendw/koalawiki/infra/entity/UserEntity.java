package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户JPA实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_created_at", columnList = "created_at")
})
public class UserEntity extends BaseJpaEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名称
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 用户邮箱
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 用户密码(加密后)
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /**
     * 用户头像
     */
    @Column(name = "avatar", length = 500)
    private String avatar;

    /**
     * 用户更新时间
     */
    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * 用户最后登录时间
     */
    @Column(name = "last_login_at")
    private Date lastLoginAt;

    /**
     * 用户最后登录IP
     */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    /**
     * 用户简介
     */
    @Column(name = "bio", length = 500)
    private String bio;

    /**
     * 用户位置
     */
    @Column(name = "location", length = 100)
    private String location;

    /**
     * 用户个人网站
     */
    @Column(name = "website", length = 200)
    private String website;

    /**
     * 用户公司
     */
    @Column(name = "company", length = 100)
    private String company;
}
