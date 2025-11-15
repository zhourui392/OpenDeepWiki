package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 自定义提交仓库请求
 */
@Data
public class CustomSubmitWarehouseRequest {

    /**
     * 组织名称
     */
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称长度不能超过100字符")
    private String organization;

    /**
     * 仓库名称
     */
    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 200, message = "仓库名称长度不能超过200字符")
    private String repositoryName;

    /**
     * 仓库地址
     */
    @NotBlank(message = "仓库地址不能为空")
    @Size(max = 500, message = "仓库地址长度不能超过500字符")
    private String address;

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
}
