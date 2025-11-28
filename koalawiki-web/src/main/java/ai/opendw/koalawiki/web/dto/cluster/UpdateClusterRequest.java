package ai.opendw.koalawiki.web.dto.cluster;

import ai.opendw.koalawiki.domain.cluster.ClusterStatus;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新集群请求
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class UpdateClusterRequest {

    /**
     * 集群名称
     */
    @NotBlank(message = "集群名称不能为空")
    @Size(max = 128, message = "集群名称长度不能超过128字符")
    private String name;

    /**
     * 集群编码
     */
    @NotBlank(message = "集群编码不能为空")
    @Size(max = 64, message = "集群编码长度不能超过64字符")
    private String code;

    /**
     * 集群描述
     */
    @Size(max = 2000, message = "集群描述长度不能超过2000字符")
    private String description;

    /**
     * 技术栈描述
     */
    @Size(max = 255, message = "技术栈描述长度不能超过255字符")
    private String techStack;

    /**
     * 负责人
     */
    @Size(max = 64, message = "负责人长度不能超过64字符")
    private String owner;

    /**
     * 集群状态
     */
    private ClusterStatus status;
}
