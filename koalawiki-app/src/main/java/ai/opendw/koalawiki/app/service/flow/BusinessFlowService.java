package ai.opendw.koalawiki.app.service.flow;

import ai.opendw.koalawiki.core.analysis.*;
import ai.opendw.koalawiki.core.analysis.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务流程服务
 * 整合服务依赖分析、调用链追踪、Mermaid生成
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessFlowService {

    private final ProjectScanner projectScanner;
    private final ServiceDependencyAnalyzer dependencyAnalyzer;
    private final BusinessFlowTracer flowTracer;
    private final MermaidGenerator mermaidGenerator;

    /**
     * 分析多个仓库的服务依赖
     *
     * @param projectPaths 项目路径列表
     * @return 服务依赖图
     */
    public ServiceDependencyGraph analyzeDependencies(List<String> projectPaths) {
        log.info("开始分析服务依赖，项目数: {}", projectPaths.size());

        // 扫描所有项目
        List<ProjectStructure> structures = projectPaths.stream()
            .map(projectScanner::scanProject)
            .collect(java.util.stream.Collectors.toList());

        // 分析依赖关系
        ServiceDependencyGraph graph = dependencyAnalyzer.analyze(structures);

        log.info("服务依赖分析完成: 服务数={}, 依赖数={}",
            graph.getServices().size(),
            graph.getDependencies().size());

        return graph;
    }

    /**
     * 生成业务流程（从入口点追踪调用链并生成Mermaid图）
     *
     * @param entryPoint 入口点
     * @param projectPath 项目路径
     * @param dependencyGraph 服务依赖图
     * @param maxDepth 最大追踪深度
     * @return 业务流程结果
     */
    public BusinessFlowResult generateFlow(
        EntryPoint entryPoint,
        String projectPath,
        ServiceDependencyGraph dependencyGraph,
        int maxDepth
    ) {
        log.info("开始生成业务流程: {}", entryPoint.getPath());

        // 扫描项目
        ProjectStructure structure = projectScanner.scanProject(projectPath);

        // 追踪调用链
        CallChain chain = flowTracer.trace(entryPoint, structure, dependencyGraph, maxDepth);

        // 生成Mermaid图
        String mermaidDiagram = mermaidGenerator.generateSequenceDiagram(chain);

        BusinessFlowResult result = new BusinessFlowResult();
        result.setFlowId(chain.getChainId());
        result.setEntryPoint(entryPoint);
        result.setCallChain(chain);
        result.setMermaidDiagram(mermaidDiagram);
        result.setNodeCount(chain.getNodes().size());
        result.setMaxDepth(chain.getMaxDepth());

        log.info("业务流程生成完成: flowId={}, 节点数={}, 深度={}",
            result.getFlowId(), result.getNodeCount(), result.getMaxDepth());

        return result;
    }

    /**
     * 为指定仓库的所有入口点生成业务流程
     *
     * @param projectPath 项目路径
     * @param dependencyGraph 服务依赖图
     * @param maxDepth 最大追踪深度
     * @return 业务流程结果列表
     */
    public List<BusinessFlowResult> generateAllFlows(
        String projectPath,
        ServiceDependencyGraph dependencyGraph,
        int maxDepth
    ) {
        log.info("开始为项目生成所有业务流程: {}", projectPath);

        // 扫描项目
        ProjectStructure structure = projectScanner.scanProject(projectPath);

        // 为每个入口点生成流程
        List<BusinessFlowResult> results = structure.getEntryPoints().stream()
            .map(ep -> generateFlow(ep, projectPath, dependencyGraph, maxDepth))
            .collect(java.util.stream.Collectors.toList());

        log.info("所有业务流程生成完成: 流程数={}", results.size());
        return results;
    }
}
