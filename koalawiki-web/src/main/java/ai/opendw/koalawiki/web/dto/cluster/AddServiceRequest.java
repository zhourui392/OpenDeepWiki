package ai.opendw.koalawiki.web.dto.cluster;

import ai.opendw.koalawiki.domain.cluster.ServiceType;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 添加服务到领域请求
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class AddServiceRequest {

    /**
     * 仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务类型
     */
    private ServiceType serviceType;

    /**
     * 是否主领域
     */
    private Boolean isPrimary;
}
