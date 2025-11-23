package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.flow.BusinessFlowResult;
import ai.opendw.koalawiki.app.service.flow.BusinessFlowService;
import ai.opendw.koalawiki.core.analysis.model.EntryPoint;
import ai.opendw.koalawiki.core.analysis.model.ServiceDependencyGraph;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务流程API控制器
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/business-flow")
@RequiredArgsConstructor
public class BusinessFlowController {

    private final BusinessFlowService businessFlowService;

    /**
     * 分析服务依赖
     */
    @PostMapping("/dependencies")
    public ServiceDependencyGraph analyzeDependencies(@RequestBody DependencyRequest request) {
        log.info("分析服务依赖: 项目数={}", request.getProjectPaths().size());
        return businessFlowService.analyzeDependencies(request.getProjectPaths());
    }

    /**
     * 生成业务流程
     */
    @PostMapping("/generate")
    public BusinessFlowResult generateFlow(@RequestBody FlowRequest request) {
        log.info("生成业务流程: entryPoint={}", request.getEntryPoint().getPath());

        // 先分析依赖
        ServiceDependencyGraph graph = businessFlowService.analyzeDependencies(request.getProjectPaths());

        // 生成流程
        return businessFlowService.generateFlow(
            request.getEntryPoint(),
            request.getProjectPath(),
            graph,
            request.getMaxDepth() != null ? request.getMaxDepth() : 5
        );
    }

    /**
     * 为仓库生成所有业务流程
     */
    @PostMapping("/generate-all")
    public List<BusinessFlowResult> generateAllFlows(@RequestBody FlowRequest request) {
        log.info("生成所有业务流程: projectPath={}", request.getProjectPath());

        // 先分析依赖
        ServiceDependencyGraph graph = businessFlowService.analyzeDependencies(request.getProjectPaths());

        // 生成所有流程
        return businessFlowService.generateAllFlows(
            request.getProjectPath(),
            graph,
            request.getMaxDepth() != null ? request.getMaxDepth() : 5
        );
    }

    @Data
    public static class DependencyRequest {
        private List<String> projectPaths;
    }

    @Data
    public static class FlowRequest {
        private List<String> projectPaths;
        private String projectPath;
        private EntryPoint entryPoint;
        private Integer maxDepth;
    }
}
