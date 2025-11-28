package ai.opendw.koalawiki.domain.cluster;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * 集群与仓库关联信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class ClusterWarehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private String id;

    /**
     * 集群ID
     */
    @NotBlank(message = "集群ID不能为空")
    private String clusterId;

    /**
     * 仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * 排序权重
     */
    private Integer sortOrder = 0;

    /**
     * 创建时间
     */
    private Date createdAt;
}
