package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务节点
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Data
public class ServiceNode {

    /**
     * 服务名
     */
    private String serviceName;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 提供的接口列表（全限定名）
     */
    private List<String> providedInterfaces = new ArrayList<>();

    /**
     * 依赖的接口列表（全限定名）
     */
    private List<String> requiredInterfaces = new ArrayList<>();

    /**
     * 添加提供的接口
     */
    public void addProvidedInterface(String interfaceName) {
        if (!providedInterfaces.contains(interfaceName)) {
            providedInterfaces.add(interfaceName);
        }
    }

    /**
     * 添加依赖的接口
     */
    public void addRequiredInterface(String interfaceName) {
        if (!requiredInterfaces.contains(interfaceName)) {
            requiredInterfaces.add(interfaceName);
        }
    }
}
