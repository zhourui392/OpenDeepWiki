package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建领域请求
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class CreateDomainRequest {

    /**
     * 领域名称
     */
    @NotBlank(message = "领域名称不能为空")
    @Size(max = 64, message = "领域名称长度不能超过64字符")
    private String name;

    /**
     * 领域编码
     */
    @NotBlank(message = "领域编码不能为空")
    @Size(max = 32, message = "领域编码长度不能超过32字符")
    private String code;

    /**
     * 领域描述
     */
    @Size(max = 500, message = "领域描述长度不能超过500字符")
    private String description;

    /**
     * 领域负责人
     */
    @Size(max = 64, message = "负责人长度不能超过64字符")
    private String owner;

    /**
     * 显示颜色
     */
    @Size(max = 20, message = "颜色长度不能超过20字符")
    private String color;

    /**
     * 排序权重
     */
    private Integer sortOrder;
}
