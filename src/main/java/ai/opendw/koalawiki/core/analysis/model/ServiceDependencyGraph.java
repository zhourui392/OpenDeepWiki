package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务依赖图
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Data
public class ServiceDependencyGraph {

    /**
     * 服务节点映射 (serviceName -> ServiceNode)
     */
    private Map<String, ServiceNode> services = new HashMap<>();

    /**
     * 接口实现映射 (interfaceName -> ServiceNode)
     */
    private Map<String, ServiceNode> interfaceIndex = new HashMap<>();

    /**
     * 依赖关系列表
     */
    private List<ServiceDependency> dependencies = new ArrayList<>();

    /**
     * 添加服务节点
     */
    public void addService(ServiceNode service) {
        services.put(service.getServiceName(), service);

        // 建立接口索引
        for (String interfaceName : service.getProvidedInterfaces()) {
            interfaceIndex.put(interfaceName, service);
        }
    }

    /**
     * 添加依赖关系
     */
    public void addDependency(ServiceDependency dependency) {
        dependencies.add(dependency);
    }

    /**
     * 根据服务名查找依赖
     */
    public List<ServiceDependency> findDependencies(String serviceName) {
        return dependencies.stream()
                .filter(d -> d.getSourceService().equals(serviceName))
                .collect(Collectors.toList());
    }

    /**
     * 根据接口名查找实现服务
     */
    public ServiceNode findServiceByInterface(String interfaceName) {
        return interfaceIndex.get(interfaceName);
    }

    /**
     * 获取服务节点
     */
    public ServiceNode getService(String serviceName) {
        return services.get(serviceName);
    }
}
