package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 添加仓库到集群请求
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class AddWarehouseRequest {

    /**
     * 仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * 排序权重
     */
    private Integer sortOrder;
}
