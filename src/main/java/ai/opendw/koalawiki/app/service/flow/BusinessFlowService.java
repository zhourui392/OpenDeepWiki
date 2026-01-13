package ai.opendw.koalawiki.app.service.flow;

import ai.opendw.koalawiki.core.analysis.*;
import ai.opendw.koalawiki.core.analysis.model.*;
import ai.opendw.koalawiki.core.git.GitCredentials;
import ai.opendw.koalawiki.core.git.GitRepositoryInfo;
import ai.opendw.koalawiki.core.git.GitRepositoryManager;
import ai.opendw.koalawiki.domain.businessflow.BusinessFlowDocument;
import ai.opendw.koalawiki.domain.businessflow.BusinessFlowDocumentBuilder;
import ai.opendw.koalawiki.infra.entity.BusinessFlowDocumentEntity;
import ai.opendw.koalawiki.infra.mapper.BusinessFlowDocumentMapper;
import ai.opendw.koalawiki.infra.repository.BusinessFlowDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final EntryPointFinder entryPointFinder;
    private final GitRepositoryManager gitRepositoryManager;
    private final BusinessFlowDocumentRepository flowDocumentRepository;
    private final BusinessFlowDocumentMapper flowDocumentMapper;
    private final ObjectMapper objectMapper;

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

    /**
     * 基于关键词生成并存储业务流程
     *
     * @param keywords       关键词列表
     * @param repositoryUrls Git仓库URL列表
     * @param credentials    认证信息
     * @param maxDepth       最大追踪深度
     * @return 按关键词分组的流程结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, List<BusinessFlowResult>> generateAndSaveFlowsByKeywords(
            List<String> keywords,
            List<String> repositoryUrls,
            GitCredentials credentials,
            int maxDepth) {

        log.info("开始基于关键词生成业务流程，关键词：{}，仓库数量：{}", keywords, repositoryUrls.size());

        try {
            // 1. 下载/更新所有仓库（使用现有GitRepositoryManager）
            List<GitRepositoryInfo> repositories = downloadOrUpdateRepositories(repositoryUrls, credentials);

            // 2. 提取本地路径和版本信息
            List<String> projectPaths = repositories.stream()
                    .map(GitRepositoryInfo::getLocalPath)
                    .collect(Collectors.toList());

            String primaryRepoVersion = repositories.get(0).getLatestCommitId();
            String primaryRepoUrl = repositoryUrls.get(0);

            // 3. 扫描项目
            List<ProjectStructure> structures = scanProjects(projectPaths);

            // 4. 分析服务依赖
            ServiceDependencyGraph dependencyGraph = dependencyAnalyzer.analyze(structures);

            // 5. 智能搜索入口点
            List<EntryPointMatch> entryPointMatches = entryPointFinder.findByKeywords(keywords, structures);

            log.info("找到 {} 个匹配的入口点", entryPointMatches.size());

            // 6. 批量生成并存储流程
            Map<String, List<BusinessFlowResult>> resultsByKeyword = new LinkedHashMap<>();

            for (String keyword : keywords) {
                List<BusinessFlowResult> keywordFlows = new ArrayList<>();

                // 过滤该关键词相关的入口点
                List<EntryPointMatch> keywordMatches = filterMatchesByKeyword(entryPointMatches, keyword);

                for (EntryPointMatch match : keywordMatches) {
                    // 检查是否已存在（避免重复生成）
                    Optional<BusinessFlowDocumentEntity> existing =
                            flowDocumentRepository.findByKeywordAndRepositoryVersionAndApiPath(
                                    keyword,
                                    primaryRepoVersion,
                                    match.getEntryPoint().getPath()
                            );

                    if (existing.isPresent()) {
                        log.info("流程已存在，使用缓存：keyword={}, api={}", keyword, match.getEntryPoint().getPath());
                        keywordFlows.add(convertToResult(existing.get(), match));
                        continue;
                    }

                    // 生成新流程
                    BusinessFlowResult flow = generateFlowForEntryPoint(
                            match.getEntryPoint(),
                            match.getProjectName(),
                            projectPaths,
                            dependencyGraph,
                            maxDepth
                    );

                    // 持久化存储
                    BusinessFlowDocument document = buildDocument(
                            flow,
                            keyword,
                            match.getRelevanceScore(),
                            primaryRepoUrl,
                            primaryRepoVersion,
                            repositoryUrls
                    );

                    BusinessFlowDocumentEntity entity = flowDocumentMapper.toEntity(document);
                    flowDocumentRepository.save(entity);

                    log.info("流程已保存：keyword={}, api={}, score={}",
                            keyword, match.getEntryPoint().getPath(), match.getRelevanceScore());

                    keywordFlows.add(flow);
                }

                resultsByKeyword.put(keyword, keywordFlows);
            }

            log.info("业务流程生成完成，总计：{} 个关键词，{} 个流程",
                    resultsByKeyword.size(),
                    resultsByKeyword.values().stream().mapToInt(List::size).sum());

            return resultsByKeyword;

        } catch (Exception e) {
            log.error("生成业务流程失败", e);
            throw new RuntimeException("生成业务流程失败：" + e.getMessage(), e);
        }
    }

    /**
     * 下载或更新仓库
     */
    private List<GitRepositoryInfo> downloadOrUpdateRepositories(
            List<String> repositoryUrls,
            GitCredentials credentials) {

        return repositoryUrls.stream()
                .map(url -> {
                    try {
                        return gitRepositoryManager.getOrCloneRepository(url, credentials);
                    } catch (Exception e) {
                        log.error("下载仓库失败：{}", url, e);
                        throw new RuntimeException("下载仓库失败：" + url, e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 扫描项目
     */
    private List<ProjectStructure> scanProjects(List<String> projectPaths) {
        return projectPaths.stream()
                .map(path -> {
                    try {
                        return projectScanner.scanProject(path);
                    } catch (Exception e) {
                        log.error("扫描项目失败：{}", path, e);
                        throw new RuntimeException("扫描项目失败：" + path, e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 过滤匹配该关键词的入口点
     */
    private List<EntryPointMatch> filterMatchesByKeyword(
            List<EntryPointMatch> allMatches,
            String keyword) {

        return allMatches.stream()
                .filter(match -> matchesKeyword(match, keyword))
                .collect(Collectors.toList());
    }

    /**
     * 判断匹配结果是否包含关键词
     */
    private boolean matchesKeyword(EntryPointMatch match, String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        return match.getMatchReasons().stream()
                .anyMatch(reason -> reason.toLowerCase().contains(lowerKeyword)) ||
                match.getEntryPoint().getClassName().toLowerCase().contains(lowerKeyword) ||
                match.getEntryPoint().getMethodName().toLowerCase().contains(lowerKeyword) ||
                (match.getEntryPoint().getPath() != null &&
                        match.getEntryPoint().getPath().toLowerCase().contains(lowerKeyword));
    }

    /**
     * 为入口点生成流程
     */
    private BusinessFlowResult generateFlowForEntryPoint(
            EntryPoint entryPoint,
            String projectName,
            List<String> projectPaths,
            ServiceDependencyGraph dependencyGraph,
            int maxDepth) {

        // 找到入口点所在的项目路径
        String projectPath = findProjectPath(projectName, projectPaths);

        // 扫描该项目
        ProjectStructure structure = projectScanner.scanProject(projectPath);

        // 追踪调用链
        CallChain callChain = flowTracer.trace(entryPoint, structure, dependencyGraph, maxDepth);

        // 生成Mermaid图
        String mermaidDiagram = mermaidGenerator.generateSequenceDiagram(callChain);

        // 构建结果
        BusinessFlowResult result = new BusinessFlowResult();
        result.setFlowId(UUID.randomUUID().toString());
        result.setEntryPoint(entryPoint);
        result.setCallChain(callChain);
        result.setMermaidDiagram(mermaidDiagram);
        result.setNodeCount(callChain.getNodes().size());
        result.setMaxDepth(callChain.getMaxDepth());

        return result;
    }

    /**
     * 查找项目路径
     */
    private String findProjectPath(String projectName, List<String> projectPaths) {
        // 简单实现：返回第一个路径
        // TODO: 实现更智能的项目路径匹配逻辑
        return projectPaths.isEmpty() ? null : projectPaths.get(0);
    }

    /**
     * 构建文档对象
     */
    private BusinessFlowDocument buildDocument(
            BusinessFlowResult flow,
            String keyword,
            int relevanceScore,
            String primaryRepoUrl,
            String primaryRepoVersion,
            List<String> allRepoUrls) {

        // 提取关联服务
        Set<String> relatedServices = extractServices(flow.getCallChain());

        // 序列化调用链
        String callChainJson = serializeCallChain(flow.getCallChain());

        // 序列化关联服务
        String relatedServicesJson = serializeList(new ArrayList<>(relatedServices));

        // 序列化依赖仓库
        String dependencyReposJson = serializeList(allRepoUrls);

        // 使用Builder构建
        BusinessFlowDocument document = new BusinessFlowDocument();
        document.setKeyword(keyword);
        document.setRelevanceScore(relevanceScore);
        document.setEntryType(flow.getEntryPoint().getType().name());
        document.setApiPath(flow.getEntryPoint().getPath());
        document.setClassName(flow.getEntryPoint().getClassName());
        document.setMethodName(flow.getEntryPoint().getMethodName());
        document.setMethodSignature(flow.getEntryPoint().getMethodSignature());
        document.setCallChainJson(callChainJson);
        document.setMermaidDiagram(flow.getMermaidDiagram());
        document.setNodeCount(flow.getNodeCount());
        document.setMaxDepth(flow.getMaxDepth());
        document.setRelatedServices(relatedServicesJson);
        document.setPrimaryRepository(primaryRepoUrl);
        document.setRepositoryVersion(primaryRepoVersion);
        document.setDependencyRepositories(dependencyReposJson);
        document.setDescription(flow.getDescription());

        return document;
    }

    /**
     * 序列化调用链
     */
    private String serializeCallChain(CallChain callChain) {
        try {
            return objectMapper.writeValueAsString(callChain);
        } catch (Exception e) {
            log.error("序列化调用链失败", e);
            return null;
        }
    }

    /**
     * 序列化列表
     */
    private String serializeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("序列化列表失败", e);
            return null;
        }
    }

    /**
     * 从调用链提取服务列表
     */
    private Set<String> extractServices(CallChain callChain) {
        Set<String> services = new HashSet<>();
        if (callChain != null && callChain.getRoot() != null) {
            extractServicesRecursive(callChain.getRoot(), services);
        }
        return services;
    }

    /**
     * 递归提取服务
     */
    private void extractServicesRecursive(CallNode node, Set<String> services) {
        if (node == null) {
            return;
        }

        if (node.getService() != null) {
            services.add(node.getService());
        }

        if (node.getChildren() != null) {
            node.getChildren().forEach(child -> extractServicesRecursive(child, services));
        }
    }

    /**
     * 将Entity转换为Result（用于缓存命中）
     */
    private BusinessFlowResult convertToResult(
            BusinessFlowDocumentEntity entity,
            EntryPointMatch match) {

        BusinessFlowResult result = new BusinessFlowResult();
        result.setFlowId(entity.getId());
        result.setEntryPoint(match.getEntryPoint());
        // TODO: 反序列化CallChain
        result.setMermaidDiagram(entity.getMermaidDiagram());
        result.setNodeCount(entity.getNodeCount());
        result.setMaxDepth(entity.getMaxDepth());
        result.setDescription(entity.getDescription());

        return result;
    }

    /**
     * 按关键词查询历史流程
     */
    public Page<BusinessFlowDocument> searchByKeyword(String keyword, Pageable pageable) {
        log.info("查询历史流程：keyword={}", keyword);

        Page<BusinessFlowDocumentEntity> entities = flowDocumentRepository.findByKeyword(keyword, pageable);

        return entities.map(flowDocumentMapper::toDomain);
    }

    /**
     * 按服务名查询相关流程
     */
    public Page<BusinessFlowDocument> searchByService(String serviceName, Pageable pageable) {
        log.info("查询服务相关流程：service={}", serviceName);

        Page<BusinessFlowDocumentEntity> entities =
                flowDocumentRepository.findByRelatedServiceContaining(serviceName, pageable);

        return entities.map(flowDocumentMapper::toDomain);
    }

    /**
     * 按API路径查询
     */
    public Optional<BusinessFlowDocument> searchByApiPath(String apiPath) {
        log.info("查询流程：apiPath={}", apiPath);

        return flowDocumentRepository.findByApiPath(apiPath)
                .map(flowDocumentMapper::toDomain);
    }

    /**
     * 获取流程详情
     */
    public Optional<BusinessFlowDocument> getFlowDetail(String id) {
        log.info("获取流程详情：id={}", id);

        return flowDocumentRepository.findById(id)
                .map(flowDocumentMapper::toDomain);
    }
}
