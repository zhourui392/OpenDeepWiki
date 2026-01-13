package ai.opendw.koalawiki.domain.warehouse;

import ai.opendw.koalawiki.domain.BaseEntity;
import ai.opendw.koalawiki.domain.ClassifyType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 仓库实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Warehouse extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 组织名称
     */
    @Size(max = 100, message = "组织名称长度不能超过100字符")
    private String organizationName;

    /**
     * 仓库名称
     */
    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 200, message = "仓库名称长度不能超过200字符")
    private String name;

    /**
     * 仓库描述
     */
    @Size(max = 1000, message = "仓库描述长度不能超过1000字符")
    private String description;

    /**
     * 仓库地址
     */
    @NotBlank(message = "仓库地址不能为空")
    @Size(max = 500, message = "仓库地址长度不能超过500字符")
    private String address;

    /**
     * 私有化git账号
     */
    @Size(max = 100, message = "Git账号长度不能超过100字符")
    private String gitUserName;

    /**
     * 私有化git密码
     */
    @Size(max = 200, message = "Git密码长度不能超过200字符")
    private String gitPassword;

    /**
     * 私有化git邮箱
     */
    @Size(max = 100, message = "Git邮箱长度不能超过100字符")
    private String email;

    /**
     * 仓库类型
     */
    @Size(max = 50, message = "仓库类型长度不能超过50字符")
    private String type;

    /**
     * 仓库分支
     */
    @Size(max = 100, message = "分支名称长度不能超过100字符")
    private String branch;

    /**
     * 仓库状态
     */
    private WarehouseStatus status;

    /**
     * 错误信息
     */
    @Size(max = 2000, message = "错误信息长度不能超过2000字符")
    private String error;

    /**
     * 仓库版本
     */
    @Size(max = 50, message = "版本号长度不能超过50字符")
    private String version;

    /**
     * 是否嵌入完成
     */
    private Boolean isEmbedded = false;

    /**
     * 是否推荐
     */
    private Boolean isRecommended = false;

    /**
     * 仓库类别
     */
    private ClassifyType classify;

    /**
     * Star数量
     */
    private Integer stars = 0;

    /**
     * Fork数量
     */
    private Integer forks = 0;

    /**
     * 是否启用同步
     */
    private Boolean enableSync = true;
}
