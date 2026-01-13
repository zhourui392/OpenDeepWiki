package ai.opendw.koalawiki.domain.businessflow;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 业务流程文档领域实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessFlowDocument extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 关键词（主关键词）
     */
    private String keyword;

    /**
     * 相关度分数
     */
    private Integer relevanceScore;

    /**
     * 入口点类型（HTTP/DUBBO/SCHEDULED/MQ）
     */
    private String entryType;

    /**
     * API路径
     */
    private String apiPath;

    /**
     * 类名（完整类名）
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 方法签名
     */
    private String methodSignature;

    /**
     * 调用链JSON（CallChain序列化）
     */
    private String callChainJson;

    /**
     * Mermaid流程图DSL
     */
    private String mermaidDiagram;

    /**
     * 节点数量
     */
    private Integer nodeCount;

    /**
     * 最大深度
     */
    private Integer maxDepth;

    /**
     * 关联服务列表（JSON数组字符串）
     */
    private String relatedServices;

    /**
     * 主仓库路径
     */
    private String primaryRepository;

    /**
     * 仓库版本（Git commit hash）
     */
    private String repositoryVersion;

    /**
     * 依赖仓库列表（JSON数组字符串）
     */
    private String dependencyRepositories;

    /**
     * AI生成的描述
     */
    private String description;

    /**
     * 创建用户ID
     */
    private String userId;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 获取关联服务列表（反序列化）
     */
    public List<String> getRelatedServicesList() {
        // TODO: JSON反序列化
        if (relatedServices == null || relatedServices.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // 使用Jackson进行反序列化（需要在实际使用时实现）
            return Arrays.asList(relatedServices.split(","));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 设置关联服务列表（序列化）
     */
    public void setRelatedServicesList(List<String> services) {
        // TODO: JSON序列化
        if (services == null || services.isEmpty()) {
            this.relatedServices = null;
        } else {
            this.relatedServices = String.join(",", services);
        }
    }
}
