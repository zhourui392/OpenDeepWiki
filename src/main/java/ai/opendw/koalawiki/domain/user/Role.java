package ai.opendw.koalawiki.domain.user;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 50, message = "角色名称长度必须在2-50之间")
    private String name;

    /**
     * 角色描述
     */
    @Size(max = 200, message = "角色描述长度不能超过200字符")
    private String description;

    /**
     * 是否启用
     */
    private Boolean isActive = true;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 是否为系统角色
     */
    private Boolean isSystemRole = false;
}
