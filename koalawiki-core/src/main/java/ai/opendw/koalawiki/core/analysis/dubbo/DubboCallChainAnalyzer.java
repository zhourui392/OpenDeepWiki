package ai.opendw.koalawiki.core.analysis.dubbo;

import ai.opendw.koalawiki.core.cluster.DubboInterfaceService;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceConsumer;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Dubbo接口调用链分析器
 * 分析接口的上下游调用关系，生成调用链
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DubboCallChainAnalyzer {

    private final DubboInterfaceService dubboInterfaceService;

    /**
     * 分析接口的调用链
     *
     * @param interfaceId 接口ID
     * @param maxDepth    最大深度
     * @return 调用链结果
     */
    public CallChainResult analyzeCallChain(String interfaceId, int maxDepth) {
        log.info("分析接口调用链: interfaceId={}, maxDepth={}", interfaceId, maxDepth);

        CallChainResult result = new CallChainResult();
        result.setInterfaceId(interfaceId);

        Optional<DubboInterfaceRegistry> registryOpt = dubboInterfaceService.getById(interfaceId);
        if (!registryOpt.isPresent()) {
            log.warn("接口不存在: interfaceId={}", interfaceId);
            return result;
        }

        DubboInterfaceRegistry registry = registryOpt.get();
        result.setInterfaceName(registry.getInterfaceName());
        result.setProviderServiceName(registry.getProviderServiceName());

        Set<String> visited = new HashSet<>();

        List<ChainNode> upstreamNodes = analyzeUpstream(registry, maxDepth, visited);
        result.setUpstreamNodes(upstreamNodes);

        visited.clear();

        List<ChainNode> downstreamNodes = analyzeDownstream(registry, maxDepth, visited);
        result.setDownstreamNodes(downstreamNodes);

        String mermaidCode = generateMermaidCode(result);
        result.setMermaidCode(mermaidCode);

        log.info("调用链分析完成: interfaceId={}, upstreamCount={}, downstreamCount={}",
                interfaceId, upstreamNodes.size(), downstreamNodes.size());

        return result;
    }

    /**
     * 分析上游调用（谁调用了这个接口）
     */
    private List<ChainNode> analyzeUpstream(DubboInterfaceRegistry registry, int maxDepth, Set<String> visited) {
        List<ChainNode> nodes = new ArrayList<>();

        if (maxDepth <= 0) {
            return nodes;
        }

        List<DubboInterfaceConsumer> consumers = dubboInterfaceService.getConsumers(registry.getId());

        for (DubboInterfaceConsumer consumer : consumers) {
            String consumerWarehouseId = consumer.getConsumerWarehouseId();

            if (visited.contains(consumerWarehouseId)) {
                continue;
            }
            visited.add(consumerWarehouseId);

            ChainNode node = new ChainNode();
            node.setServiceId(consumerWarehouseId);
            node.setServiceName(consumer.getConsumerServiceName());
            node.setInterfaceName(registry.getInterfaceName());
            node.setDirection(ChainDirection.UPSTREAM);
            node.setDepth(1);
            node.setSourceClass(consumer.getSourceClass());

            List<DubboInterfaceRegistry> consumerInterfaces = dubboInterfaceService.listByProvider(consumerWarehouseId);
            for (DubboInterfaceRegistry consumerInterface : consumerInterfaces) {
                if (maxDepth > 1) {
                    List<ChainNode> childNodes = analyzeUpstream(consumerInterface, maxDepth - 1, visited);
                    node.getChildren().addAll(childNodes);
                }
            }

            nodes.add(node);
        }

        return nodes;
    }

    /**
     * 分析下游调用（这个服务调用了哪些接口）
     */
    private List<ChainNode> analyzeDownstream(DubboInterfaceRegistry registry, int maxDepth, Set<String> visited) {
        List<ChainNode> nodes = new ArrayList<>();

        if (maxDepth <= 0 || registry.getProviderWarehouseId() == null) {
            return nodes;
        }

        String providerWarehouseId = registry.getProviderWarehouseId();

        List<DubboInterfaceRegistry> consumedInterfaces = dubboInterfaceService.getConsumedInterfaces(providerWarehouseId);

        for (DubboInterfaceRegistry consumedInterface : consumedInterfaces) {
            String targetWarehouseId = consumedInterface.getProviderWarehouseId();

            if (targetWarehouseId == null || visited.contains(targetWarehouseId)) {
                continue;
            }
            visited.add(targetWarehouseId);

            ChainNode node = new ChainNode();
            node.setServiceId(targetWarehouseId);
            node.setServiceName(consumedInterface.getProviderServiceName());
            node.setInterfaceName(consumedInterface.getInterfaceName());
            node.setDirection(ChainDirection.DOWNSTREAM);
            node.setDepth(1);

            if (maxDepth > 1) {
                List<ChainNode> childNodes = analyzeDownstream(consumedInterface, maxDepth - 1, visited);
                node.getChildren().addAll(childNodes);
            }

            nodes.add(node);
        }

        return nodes;
    }

    /**
     * 生成Mermaid图代码
     */
    private String generateMermaidCode(CallChainResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph LR\n");

        String centerNodeId = sanitizeNodeId(result.getProviderServiceName());
        sb.append(String.format("    %s[%s]:::center\n", centerNodeId, result.getProviderServiceName()));

        Set<String> edges = new HashSet<>();

        for (ChainNode node : result.getUpstreamNodes()) {
            generateUpstreamMermaid(sb, node, centerNodeId, edges);
        }

        for (ChainNode node : result.getDownstreamNodes()) {
            generateDownstreamMermaid(sb, centerNodeId, node, edges);
        }

        sb.append("\n    classDef center fill:#f9f,stroke:#333,stroke-width:2px;\n");
        sb.append("    classDef upstream fill:#bbf,stroke:#333,stroke-width:1px;\n");
        sb.append("    classDef downstream fill:#bfb,stroke:#333,stroke-width:1px;\n");

        return sb.toString();
    }

    /**
     * 生成上游节点的Mermaid代码
     */
    private void generateUpstreamMermaid(StringBuilder sb, ChainNode node, String targetNodeId, Set<String> edges) {
        String nodeId = sanitizeNodeId(node.getServiceName());
        String edgeKey = nodeId + "->" + targetNodeId;

        if (!edges.contains(edgeKey)) {
            edges.add(edgeKey);
            sb.append(String.format("    %s[%s]:::upstream --> %s\n",
                    nodeId, node.getServiceName(), targetNodeId));
        }

        for (ChainNode child : node.getChildren()) {
            generateUpstreamMermaid(sb, child, nodeId, edges);
        }
    }

    /**
     * 生成下游节点的Mermaid代码
     */
    private void generateDownstreamMermaid(StringBuilder sb, String sourceNodeId, ChainNode node, Set<String> edges) {
        String nodeId = sanitizeNodeId(node.getServiceName());
        String edgeKey = sourceNodeId + "->" + nodeId;

        if (!edges.contains(edgeKey)) {
            edges.add(edgeKey);
            sb.append(String.format("    %s --> %s[%s]:::downstream\n",
                    sourceNodeId, nodeId, node.getServiceName()));
        }

        for (ChainNode child : node.getChildren()) {
            generateDownstreamMermaid(sb, nodeId, child, edges);
        }
    }

    /**
     * 清理节点ID（去除特殊字符）
     */
    private String sanitizeNodeId(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * 调用链方向枚举
     */
    public enum ChainDirection {
        UPSTREAM,
        DOWNSTREAM
    }

    /**
     * 调用链节点
     */
    public static class ChainNode {
        private String serviceId;
        private String serviceName;
        private String interfaceName;
        private ChainDirection direction;
        private int depth;
        private String sourceClass;
        private List<ChainNode> children = new ArrayList<>();

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public ChainDirection getDirection() {
            return direction;
        }

        public void setDirection(ChainDirection direction) {
            this.direction = direction;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        public String getSourceClass() {
            return sourceClass;
        }

        public void setSourceClass(String sourceClass) {
            this.sourceClass = sourceClass;
        }

        public List<ChainNode> getChildren() {
            return children;
        }

        public void setChildren(List<ChainNode> children) {
            this.children = children;
        }
    }

    /**
     * 调用链结果
     */
    public static class CallChainResult {
        private String interfaceId;
        private String interfaceName;
        private String providerServiceName;
        private List<ChainNode> upstreamNodes = new ArrayList<>();
        private List<ChainNode> downstreamNodes = new ArrayList<>();
        private String mermaidCode;

        public String getInterfaceId() {
            return interfaceId;
        }

        public void setInterfaceId(String interfaceId) {
            this.interfaceId = interfaceId;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getProviderServiceName() {
            return providerServiceName;
        }

        public void setProviderServiceName(String providerServiceName) {
            this.providerServiceName = providerServiceName;
        }

        public List<ChainNode> getUpstreamNodes() {
            return upstreamNodes;
        }

        public void setUpstreamNodes(List<ChainNode> upstreamNodes) {
            this.upstreamNodes = upstreamNodes;
        }

        public List<ChainNode> getDownstreamNodes() {
            return downstreamNodes;
        }

        public void setDownstreamNodes(List<ChainNode> downstreamNodes) {
            this.downstreamNodes = downstreamNodes;
        }

        public String getMermaidCode() {
            return mermaidCode;
        }

        public void setMermaidCode(String mermaidCode) {
            this.mermaidCode = mermaidCode;
        }

        public int getUpstreamCount() {
            return upstreamNodes.size();
        }

        public int getDownstreamCount() {
            return downstreamNodes.size();
        }
    }
}
