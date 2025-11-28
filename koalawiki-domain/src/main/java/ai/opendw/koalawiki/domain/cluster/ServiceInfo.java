package ai.opendw.koalawiki.domain.cluster;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 服务信息值对象
 * 描述领域内的服务信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class ServiceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 映射ID
     */
    private String id;

    /**
     * 所属领域ID
     */
    private String domainId;

    /**
     * 服务仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * 服务名称
     */
    @Size(max = 128, message = "服务名称长度不能超过128字符")
    private String serviceName;

    /**
     * 服务描述
     */
    @Size(max = 500, message = "服务描述长度不能超过500字符")
    private String description;

    /**
     * 服务类型
     */
    private ServiceType type = ServiceType.PROVIDER;

    /**
     * 排序权重
     */
    private Integer sortOrder = 0;

    /**
     * 是否主领域
     */
    private Boolean isPrimary = false;

    /**
     * 创建时间
     */
    private Date createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceInfo that = (ServiceInfo) o;
        return Objects.equals(warehouseId, that.warehouseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId);
    }
}
