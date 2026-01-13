package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务依赖分析器
 * 识别服务间的调用关系（Dubbo/Feign等）
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Slf4j
@Component
public class ServiceDependencyAnalyzer {

    /**
     * 分析多个项目的服务依赖关系
     *
     * @param structures 多个项目结构
     * @return 服务依赖图
     */
    public ServiceDependencyGraph analyze(List<ProjectStructure> structures) {
        log.info("开始分析服务依赖，项目数: {}", structures.size());

        ServiceDependencyGraph graph = new ServiceDependencyGraph();

        // 第一遍：扫描所有服务，建立服务节点和接口索引
        for (ProjectStructure structure : structures) {
            ServiceNode serviceNode = buildServiceNode(structure);
            graph.addService(serviceNode);
            log.info("服务节点: {}, 提供接口数: {}",
                serviceNode.getServiceName(),
                serviceNode.getProvidedInterfaces().size());
        }

        // 第二遍：分析依赖关系
        for (ProjectStructure structure : structures) {
            analyzeDependencies(structure, graph);
        }

        log.info("服务依赖分析完成，服务数: {}, 依赖关系数: {}",
            graph.getServices().size(),
            graph.getDependencies().size());

        return graph;
    }

    /**
     * 构建服务节点
     */
    private ServiceNode buildServiceNode(ProjectStructure structure) {
        ServiceNode node = new ServiceNode();
        node.setServiceName(structure.getProjectName());

        // 扫描所有类，识别提供的接口
        for (ClassInfo classInfo : structure.getClasses().values()) {
            // Dubbo服务提供者
            if (classInfo.hasAnnotation("DubboService") ||
                classInfo.hasAnnotation("Service")) {
                // 获取实现的接口
                for (String interfaceName : classInfo.getInterfaces()) {
                    node.addProvidedInterface(resolveFullInterfaceName(interfaceName, classInfo));
                }
            }
        }

        return node;
    }

    /**
     * 分析依赖关系
     */
    private void analyzeDependencies(ProjectStructure structure, ServiceDependencyGraph graph) {
        String serviceName = structure.getProjectName();

        for (ClassInfo classInfo : structure.getClasses().values()) {
            // 分析字段注解，识别Dubbo/Feign依赖
            for (FieldInfo field : classInfo.getFields()) {
                analyzeDubboReference(field, classInfo, serviceName, graph);
                analyzeFeignClient(field, classInfo, serviceName, graph);
            }
        }
    }

    /**
     * 分析Dubbo @Reference依赖
     */
    private void analyzeDubboReference(FieldInfo field, ClassInfo classInfo,
                                       String serviceName, ServiceDependencyGraph graph) {
        if (field.hasAnnotation("Reference") || field.hasAnnotation("DubboReference")) {
            String interfaceName = resolveFullInterfaceName(field.getType(), classInfo);

            ServiceDependency dependency = new ServiceDependency();
            dependency.setSourceService(serviceName);
            dependency.setInterfaceName(interfaceName);
            dependency.setType(DependencyType.DUBBO);
            dependency.setSourceClass(classInfo.getFullClassName());
            dependency.setSourceField(field.getName());

            // 查找目标服务
            ServiceNode targetService = graph.findServiceByInterface(interfaceName);
            if (targetService != null) {
                dependency.setTargetService(targetService.getServiceName());
            }

            graph.addDependency(dependency);
            log.debug("Dubbo依赖: {} -> {}", serviceName, interfaceName);
        }
    }

    /**
     * 分析Feign @FeignClient依赖
     */
    private void analyzeFeignClient(FieldInfo field, ClassInfo classInfo,
                                     String serviceName, ServiceDependencyGraph graph) {
        if (field.hasAnnotation("FeignClient")) {
            // Feign客户端通常是接口，需要从注解中提取服务名
            // 这里简化处理，实际需要解析注解的name/value属性
            String interfaceName = resolveFullInterfaceName(field.getType(), classInfo);

            ServiceDependency dependency = new ServiceDependency();
            dependency.setSourceService(serviceName);
            dependency.setInterfaceName(interfaceName);
            dependency.setType(DependencyType.FEIGN);
            dependency.setSourceClass(classInfo.getFullClassName());
            dependency.setSourceField(field.getName());

            graph.addDependency(dependency);
            log.debug("Feign依赖: {} -> {}", serviceName, interfaceName);
        }
    }

    /**
     * 解析完整的接口名
     * 如果是简单类名，尝试从import中解析
     */
    private String resolveFullInterfaceName(String typeName, ClassInfo classInfo) {
        // 如果已经是全限定名
        if (typeName.contains(".")) {
            return typeName;
        }

        // 如果是同包下的类
        if (classInfo.getPackageName() != null) {
            return classInfo.getPackageName() + "." + typeName;
        }

        return typeName;
    }
}
