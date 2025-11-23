package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 业务流程追踪器
 * 从入口点开始递归追踪调用链路
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessFlowTracer {

    private static final int DEFAULT_MAX_DEPTH = 5;

    /**
     * 追踪业务流程
     *
     * @param entryPoint 入口点
     * @param structure 项目结构
     * @param dependencyGraph 服务依赖图
     * @return 调用链
     */
    public CallChain trace(EntryPoint entryPoint, ProjectStructure structure,
                           ServiceDependencyGraph dependencyGraph) {
        return trace(entryPoint, structure, dependencyGraph, DEFAULT_MAX_DEPTH);
    }

    /**
     * 追踪业务流程（指定最大深度）
     *
     * @param entryPoint 入口点
     * @param structure 项目结构
     * @param dependencyGraph 服务依赖图
     * @param maxDepth 最大追踪深度
     * @return 调用链
     */
    public CallChain trace(EntryPoint entryPoint, ProjectStructure structure,
                           ServiceDependencyGraph dependencyGraph, int maxDepth) {
        log.info("开始追踪业务流程: {}, 最大深度: {}", entryPoint.getPath(), maxDepth);

        CallChain chain = new CallChain();
        chain.setChainId(UUID.randomUUID().toString());
        chain.setEntryPoint(entryPoint);

        // 查找入口方法
        ClassInfo entryClass = structure.getClasses().get(entryPoint.getClassName());
        if (entryClass == null) {
            log.warn("未找到入口类: {}", entryPoint.getClassName());
            return chain;
        }

        MethodInfo entryMethod = findMethod(entryClass, entryPoint.getMethodName());
        if (entryMethod == null) {
            log.warn("未找到入口方法: {}.{}", entryPoint.getClassName(), entryPoint.getMethodName());
            return chain;
        }

        // 开始递归追踪
        Set<String> visited = new HashSet<>();
        CallNode root = traceRecursive(
            entryMethod,
            entryClass,
            structure,
            dependencyGraph,
            0,
            maxDepth,
            visited
        );

        chain.setRoot(root);
        collectNodes(root, chain);

        log.info("业务流程追踪完成: 节点数={}, 深度={}", chain.getNodes().size(), chain.getMaxDepth());
        return chain;
    }

    /**
     * 递归追踪调用链
     */
    private CallNode traceRecursive(
        MethodInfo method,
        ClassInfo currentClass,
        ProjectStructure structure,
        ServiceDependencyGraph dependencyGraph,
        int depth,
        int maxDepth,
        Set<String> visited
    ) {
        // 深度限制
        if (depth >= maxDepth) {
            log.debug("达到最大深度: {}", maxDepth);
            return null;
        }

        // 循环检测
        String methodKey = currentClass.getFullClassName() + "." + method.getName();
        if (visited.contains(methodKey)) {
            log.debug("检测到循环调用: {}", methodKey);
            return null;
        }
        visited.add(methodKey);

        // 创建当前节点
        CallNode node = new CallNode();
        node.setService(structure.getProjectName());
        node.setClassName(currentClass.getFullClassName());
        node.setMethod(method.getSignature());
        node.setType(CallType.LOCAL);
        node.setDepth(depth);

        // 分析方法内的调用
        for (String calledMethod : method.getCalledMethods()) {
            CallNode childNode = analyzeMethodCall(
                calledMethod,
                currentClass,
                structure,
                dependencyGraph,
                depth,
                maxDepth,
                visited
            );
            node.addChild(childNode);
        }

        return node;
    }

    /**
     * 分析方法调用
     */
    private CallNode analyzeMethodCall(
        String calledMethod,
        ClassInfo currentClass,
        ProjectStructure structure,
        ServiceDependencyGraph dependencyGraph,
        int depth,
        int maxDepth,
        Set<String> visited
    ) {
        // 检查是否是Dubbo/Feign调用
        ServiceDependency remoteDependency = findRemoteDependency(
            calledMethod,
            currentClass,
            dependencyGraph
        );

        if (remoteDependency != null) {
            // 跨服务调用
            return createRemoteCallNode(remoteDependency, depth);
        }

        // 本地方法调用
        MethodInfo nextMethod = findMethodInProject(calledMethod, structure);
        if (nextMethod != null) {
            ClassInfo nextClass = findClassForMethod(nextMethod, structure);
            if (nextClass != null) {
                return traceRecursive(
                    nextMethod,
                    nextClass,
                    structure,
                    dependencyGraph,
                    depth + 1,
                    maxDepth,
                    visited
                );
            }
        }

        return null;
    }

    /**
     * 查找远程依赖
     */
    private ServiceDependency findRemoteDependency(
        String calledMethod,
        ClassInfo currentClass,
        ServiceDependencyGraph dependencyGraph
    ) {
        // 从方法调用中提取字段名（如：inventoryService.checkStock）
        String[] parts = calledMethod.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        String fieldName = parts[0];

        // 查找字段
        for (FieldInfo field : currentClass.getFields()) {
            if (field.getName().equals(fieldName)) {
                // 检查是否是Dubbo/Feign字段
                if (field.hasAnnotation("Reference") ||
                    field.hasAnnotation("DubboReference") ||
                    field.hasAnnotation("FeignClient")) {

                    // 查找对应的依赖关系
                    for (ServiceDependency dep : dependencyGraph.getDependencies()) {
                        if (dep.getSourceClass().equals(currentClass.getFullClassName()) &&
                            dep.getSourceField().equals(fieldName)) {
                            return dep;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 创建远程调用节点
     */
    private CallNode createRemoteCallNode(ServiceDependency dependency, int depth) {
        CallNode node = new CallNode();
        node.setService(dependency.getTargetService());
        node.setClassName(dependency.getInterfaceName());
        node.setMethod("远程调用");
        node.setType(dependency.getType() == DependencyType.DUBBO ? CallType.DUBBO : CallType.FEIGN);
        node.setDepth(depth + 1);
        return node;
    }

    /**
     * 在项目中查找方法
     */
    private MethodInfo findMethodInProject(String methodCall, ProjectStructure structure) {
        // 简化处理：从方法调用中提取方法名
        String[] parts = methodCall.split("\\.");
        String methodName = parts[parts.length - 1];

        // 移除参数部分
        if (methodName.contains("(")) {
            methodName = methodName.substring(0, methodName.indexOf("("));
        }

        // 在所有类中查找
        for (ClassInfo classInfo : structure.getClasses().values()) {
            MethodInfo method = findMethod(classInfo, methodName);
            if (method != null) {
                return method;
            }
        }

        return null;
    }

    /**
     * 查找方法所属的类
     */
    private ClassInfo findClassForMethod(MethodInfo method, ProjectStructure structure) {
        for (ClassInfo classInfo : structure.getClasses().values()) {
            if (classInfo.getMethods().contains(method)) {
                return classInfo;
            }
        }
        return null;
    }

    /**
     * 在类中查找方法
     */
    private MethodInfo findMethod(ClassInfo classInfo, String methodName) {
        return classInfo.getMethods().stream()
            .filter(m -> m.getName().equals(methodName))
            .findFirst()
            .orElse(null);
    }

    /**
     * 收集所有节点到扁平列表
     */
    private void collectNodes(CallNode node, CallChain chain) {
        if (node == null) {
            return;
        }
        chain.addNode(node);
        for (CallNode child : node.getChildren()) {
            collectNodes(child, chain);
        }
    }
}
