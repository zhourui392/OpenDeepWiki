package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新仓库请求
 */
@Data
public class UpdateWarehouseRequest {

    /**
     * 仓库名称
     */
    @Size(max = 200, message = "仓库名称长度不能超过200字符")
    private String name;

    /**
     * 仓库描述
     */
    @Size(max = 1000, message = "仓库描述长度不能超过1000字符")
    private String description;

    /**
     * 分支名称
     */
    @Size(max = 100, message = "分支名称长度不能超过100字符")
    private String branch;

    /**
     * Git用户名
     */
    @Size(max = 100, message = "Git用户名长度不能超过100字符")
    private String gitUserName;

    /**
     * Git密码
     */
    @Size(max = 200, message = "Git密码长度不能超过200字符")
    private String gitPassword;

    /**
     * Git邮箱
     */
    @Size(max = 100, message = "Git邮箱长度不能超过100字符")
    private String email;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 是否启用同步
     */
    private Boolean enableSync;
}
