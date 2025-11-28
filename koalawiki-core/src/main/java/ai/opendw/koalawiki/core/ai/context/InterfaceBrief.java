package ai.opendw.koalawiki.core.ai.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口简要信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceBrief {

    /**
     * 接口ID
     */
    private String id;

    /**
     * 接口全限定名
     */
    private String interfaceName;

    /**
     * 接口简名
     */
    private String simpleName;

    /**
     * 接口版本
     */
    private String version;

    /**
     * 提供者服务名
     */
    private String providerServiceName;

    /**
     * 提供者仓库ID
     */
    private String providerWarehouseId;

    /**
     * 消费者数量
     */
    private int consumerCount;

    /**
     * 方法数量
     */
    private int methodCount;

    /**
     * 接口描述
     */
    private String description;

    /**
     * 是否废弃
     */
    private boolean deprecated;

    /**
     * 获取接口简名
     *
     * @return 简名
     */
    public String getSimpleName() {
        if (simpleName != null) {
            return simpleName;
        }
        if (interfaceName != null && interfaceName.contains(".")) {
            return interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
        }
        return interfaceName;
    }
}
