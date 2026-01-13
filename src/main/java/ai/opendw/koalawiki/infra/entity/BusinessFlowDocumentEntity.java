package ai.opendw.koalawiki.infra.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * 业务流程文档JPA实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-23
 */
@Entity
@Table(name = "business_flow_documents",
        indexes = {
                @Index(name = "idx_keyword", columnList = "keyword"),
                @Index(name = "idx_api_path", columnList = "api_path"),
                @Index(name = "idx_class_name", columnList = "class_name"),
                @Index(name = "idx_repository_version", columnList = "repository_version"),
                @Index(name = "idx_created_at", columnList = "created_at"),
                @Index(name = "idx_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "idx_unique_flow",
                        columnNames = {"keyword", "repository_version", "api_path"}
                )
        })
public class BusinessFlowDocumentEntity extends BaseJpaEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 关键词
     */
    @Column(name = "keyword", nullable = false, length = 50)
    private String keyword;

    /**
     * 相关度分数
     */
    @Column(name = "relevance_score")
    private Integer relevanceScore;

    /**
     * 入口点类型
     */
    @Column(name = "entry_type", length = 20)
    private String entryType;

    /**
     * API路径
     */
    @Column(name = "api_path", length = 500)
    private String apiPath;

    /**
     * 类名
     */
    @Column(name = "class_name", length = 500)
    private String className;

    /**
     * 方法名
     */
    @Column(name = "method_name", length = 200)
    private String methodName;

    /**
     * 方法签名
     */
    @Column(name = "method_signature", length = 500)
    private String methodSignature;

    /**
     * 调用链JSON
     */
    @Column(name = "call_chain_json", columnDefinition = "TEXT")
    @Lob
    private String callChainJson;

    /**
     * Mermaid流程图
     */
    @Column(name = "mermaid_diagram", columnDefinition = "TEXT")
    @Lob
    private String mermaidDiagram;

    /**
     * 节点数量
     */
    @Column(name = "node_count")
    private Integer nodeCount;

    /**
     * 最大深度
     */
    @Column(name = "max_depth")
    private Integer maxDepth;

    /**
     * 关联服务列表（JSON）
     */
    @Column(name = "related_services", columnDefinition = "TEXT")
    @Lob
    private String relatedServices;

    /**
     * 主仓库路径
     */
    @Column(name = "primary_repository", length = 500)
    private String primaryRepository;

    /**
     * 仓库版本
     */
    @Column(name = "repository_version", length = 50)
    private String repositoryVersion;

    /**
     * 依赖仓库列表（JSON）
     */
    @Column(name = "dependency_repositories", columnDefinition = "TEXT")
    @Lob
    private String dependencyRepositories;

    /**
     * 描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @Lob
    private String description;

    /**
     * 创建用户ID
     */
    @Column(name = "user_id", length = 50)
    private String userId;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Date updatedAt;

    // Getters and Setters

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Integer relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getCallChainJson() {
        return callChainJson;
    }

    public void setCallChainJson(String callChainJson) {
        this.callChainJson = callChainJson;
    }

    public String getMermaidDiagram() {
        return mermaidDiagram;
    }

    public void setMermaidDiagram(String mermaidDiagram) {
        this.mermaidDiagram = mermaidDiagram;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public String getRelatedServices() {
        return relatedServices;
    }

    public void setRelatedServices(String relatedServices) {
        this.relatedServices = relatedServices;
    }

    public String getPrimaryRepository() {
        return primaryRepository;
    }

    public void setPrimaryRepository(String primaryRepository) {
        this.primaryRepository = primaryRepository;
    }

    public String getRepositoryVersion() {
        return repositoryVersion;
    }

    public void setRepositoryVersion(String repositoryVersion) {
        this.repositoryVersion = repositoryVersion;
    }

    public String getDependencyRepositories() {
        return dependencyRepositories;
    }

    public void setDependencyRepositories(String dependencyRepositories) {
        this.dependencyRepositories = dependencyRepositories;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
