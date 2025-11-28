package ai.opendw.koalawiki.core.ai.context;

import ai.opendw.koalawiki.domain.cluster.ContextLevel;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AI上下文
 * 包含分层的微服务上下文信息，用于AI辅助技术方案设计
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class AIContext {

    /**
     * 集群ID
     */
    private String clusterId;

    /**
     * 目标服务ID
     */
    private String targetServiceId;

    /**
     * 上下文级别
     */
    private ContextLevel contextLevel;

    /**
     * L1: 全局服务索引
     */
    private GlobalServiceIndex globalIndex;

    /**
     * L2: 领域摘要
     */
    private DomainSummary domainSummary;

    /**
     * L3: 服务详情
     */
    private ServiceDetail serviceDetail;

    /**
     * L4: 相关服务列表
     */
    private List<ServiceSummary> relatedServices = new ArrayList<>();

    /**
     * 接口详情列表
     */
    private List<InterfaceDetail> interfaceDetails = new ArrayList<>();

    /**
     * 服务摘要列表（按需加载模式）
     */
    private List<ServiceSummary> serviceSummaries = new ArrayList<>();

    /**
     * 生成时间
     */
    private Date generatedAt;

    /**
     * 预估上下文大小（字符数）
     */
    private int estimatedSize;

    /**
     * 添加服务摘要
     *
     * @param summary 服务摘要
     */
    public void addServiceSummary(ServiceSummary summary) {
        if (summary != null) {
            serviceSummaries.add(summary);
        }
    }

    /**
     * 添加接口详情
     *
     * @param detail 接口详情
     */
    public void addInterfaceDetail(InterfaceDetail detail) {
        if (detail != null) {
            interfaceDetails.add(detail);
        }
    }

    /**
     * 添加相关服务
     *
     * @param summary 服务摘要
     */
    public void addRelatedService(ServiceSummary summary) {
        if (summary != null) {
            relatedServices.add(summary);
        }
    }

    /**
     * 计算预估大小
     *
     * @return 预估字符数
     */
    public int calculateEstimatedSize() {
        int size = 0;

        if (globalIndex != null) {
            size += globalIndex.estimateSize();
        }
        if (domainSummary != null) {
            size += domainSummary.estimateSize();
        }
        if (serviceDetail != null) {
            size += serviceDetail.estimateSize();
        }
        for (ServiceSummary summary : relatedServices) {
            size += summary.estimateSize();
        }
        for (InterfaceDetail detail : interfaceDetails) {
            size += detail.estimateSize();
        }
        for (ServiceSummary summary : serviceSummaries) {
            size += summary.estimateSize();
        }

        this.estimatedSize = size;
        return size;
    }

    /**
     * 转换为Markdown格式
     *
     * @return Markdown文本
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();

        sb.append("# AI 上下文\n\n");

        if (globalIndex != null) {
            sb.append(globalIndex.toMarkdown());
        }

        if (domainSummary != null) {
            sb.append(domainSummary.toMarkdown());
        }

        if (serviceDetail != null) {
            sb.append(serviceDetail.toMarkdown());
        }

        if (!relatedServices.isEmpty()) {
            sb.append("## 相关服务\n\n");
            for (ServiceSummary summary : relatedServices) {
                sb.append(summary.toMarkdown());
            }
        }

        return sb.toString();
    }
}
