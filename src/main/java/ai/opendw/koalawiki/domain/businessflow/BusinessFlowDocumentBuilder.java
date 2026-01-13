package ai.opendw.koalawiki.domain.businessflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 业务流程文档构建器
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-23
 */
public class BusinessFlowDocumentBuilder {

    private final BusinessFlowDocument document;

    public BusinessFlowDocumentBuilder() {
        this.document = new BusinessFlowDocument();
    }

    /**
     * 设置关键词
     */
    public BusinessFlowDocumentBuilder keyword(String keyword) {
        document.setKeyword(keyword);
        return this;
    }

    /**
     * 设置相关度分数
     */
    public BusinessFlowDocumentBuilder relevanceScore(Integer score) {
        document.setRelevanceScore(score);
        return this;
    }

    /**
     * 设置入口点类型
     */
    public BusinessFlowDocumentBuilder entryType(String entryType) {
        document.setEntryType(entryType);
        return this;
    }

    /**
     * 设置API路径
     */
    public BusinessFlowDocumentBuilder apiPath(String apiPath) {
        document.setApiPath(apiPath);
        return this;
    }

    /**
     * 设置类名
     */
    public BusinessFlowDocumentBuilder className(String className) {
        document.setClassName(className);
        return this;
    }

    /**
     * 设置方法名
     */
    public BusinessFlowDocumentBuilder methodName(String methodName) {
        document.setMethodName(methodName);
        return this;
    }

    /**
     * 设置方法签名
     */
    public BusinessFlowDocumentBuilder methodSignature(String methodSignature) {
        document.setMethodSignature(methodSignature);
        return this;
    }

    /**
     * 设置调用链JSON
     */
    public BusinessFlowDocumentBuilder callChainJson(String callChainJson) {
        document.setCallChainJson(callChainJson);
        return this;
    }

    /**
     * 设置Mermaid流程图
     */
    public BusinessFlowDocumentBuilder mermaidDiagram(String mermaidDiagram) {
        document.setMermaidDiagram(mermaidDiagram);
        return this;
    }

    /**
     * 设置节点数量
     */
    public BusinessFlowDocumentBuilder nodeCount(Integer nodeCount) {
        document.setNodeCount(nodeCount);
        return this;
    }

    /**
     * 设置最大深度
     */
    public BusinessFlowDocumentBuilder maxDepth(Integer maxDepth) {
        document.setMaxDepth(maxDepth);
        return this;
    }

    /**
     * 设置关联服务列表JSON
     */
    public BusinessFlowDocumentBuilder relatedServices(String relatedServices) {
        document.setRelatedServices(relatedServices);
        return this;
    }

    /**
     * 设置关联服务列表
     */
    public BusinessFlowDocumentBuilder relatedServicesList(List<String> services) {
        document.setRelatedServicesList(services);
        return this;
    }

    /**
     * 设置关联服务集合
     */
    public BusinessFlowDocumentBuilder relatedServicesSet(Set<String> services) {
        if (services != null && !services.isEmpty()) {
            document.setRelatedServicesList(new ArrayList<>(services));
        }
        return this;
    }

    /**
     * 设置仓库信息
     */
    public BusinessFlowDocumentBuilder repository(String primaryRepo, String version) {
        document.setPrimaryRepository(primaryRepo);
        document.setRepositoryVersion(version);
        return this;
    }

    /**
     * 设置主仓库路径
     */
    public BusinessFlowDocumentBuilder primaryRepository(String primaryRepository) {
        document.setPrimaryRepository(primaryRepository);
        return this;
    }

    /**
     * 设置仓库版本
     */
    public BusinessFlowDocumentBuilder repositoryVersion(String repositoryVersion) {
        document.setRepositoryVersion(repositoryVersion);
        return this;
    }

    /**
     * 设置依赖仓库列表
     */
    public BusinessFlowDocumentBuilder dependencyRepositories(String dependencyRepositories) {
        document.setDependencyRepositories(dependencyRepositories);
        return this;
    }

    /**
     * 设置描述
     */
    public BusinessFlowDocumentBuilder description(String description) {
        document.setDescription(description);
        return this;
    }

    /**
     * 设置创建时间
     */
    public BusinessFlowDocumentBuilder createdAt(Date createdAt) {
        document.setCreatedAt(createdAt);
        return this;
    }

    /**
     * 设置更新时间
     */
    public BusinessFlowDocumentBuilder updatedAt(Date updatedAt) {
        document.setUpdatedAt(updatedAt);
        return this;
    }

    /**
     * 构建文档对象
     */
    public BusinessFlowDocument build() {
        // 设置默认时间
        Date now = new Date();
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(now);
        }
        if (document.getUpdatedAt() == null) {
            document.setUpdatedAt(now);
        }

        return document;
    }
}
