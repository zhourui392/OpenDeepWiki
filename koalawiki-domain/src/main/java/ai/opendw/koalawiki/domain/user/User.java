package ai.opendw.koalawiki.domain.user;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    private String name;

    /**
     * 用户邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 用户密码(加密后)
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 用户更新时间
     */
    private Date updatedAt;

    /**
     * 用户最后登录时间
     */
    private Date lastLoginAt;

    /**
     * 用户最后登录IP
     */
    private String lastLoginIp;

    /**
     * 用户简介
     */
    @Size(max = 500, message = "简介长度不能超过500字符")
    private String bio;

    /**
     * 用户位置
     */
    @Size(max = 100, message = "位置长度不能超过100字符")
    private String location;

    /**
     * 用户个人网站
     */
    @Size(max = 200, message = "网站地址长度不能超过200字符")
    private String website;

    /**
     * 用户公司
     */
    @Size(max = 100, message = "公司名称长度不能超过100字符")
    private String company;
}
