package ai.opendw.koalawiki.infra.entity.cluster;

import ai.opendw.koalawiki.domain.cluster.GraphType;
import ai.opendw.koalawiki.infra.entity.BaseJpaEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * 服务依赖图缓存JPA实体
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Entity
@Table(name = "service_dependency_graph",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cluster_type_scope", columnNames = {"cluster_id", "graph_type", "scope_id"})
    },
    indexes = {
        @Index(name = "idx_cluster_id", columnList = "cluster_id"),
        @Index(name = "idx_graph_type", columnList = "graph_type"),
        @Index(name = "idx_expires_at", columnList = "expires_at"),
        @Index(name = "idx_generated_at", columnList = "generated_at")
    })
public class ServiceDependencyGraphEntity extends BaseJpaEntity {

    /**
     * 所属集群ID
     */
    @Column(name = "cluster_id", nullable = false, length = 36)
    private String clusterId;

    /**
     * 图类型
     */
    @Column(name = "graph_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private GraphType graphType;

    /**
     * 作用域ID
     */
    @Column(name = "scope_id", length = 36)
    private String scopeId;

    /**
     * 图数据（JSON格式）
     */
    @Column(name = "graph_data", nullable = false, columnDefinition = "LONGTEXT")
    private String graphData;

    /**
     * 节点数
     */
    @Column(name = "node_count", nullable = false)
    private Integer nodeCount = 0;

    /**
     * 边数
     */
    @Column(name = "edge_count", nullable = false)
    private Integer edgeCount = 0;

    /**
     * Mermaid图代码
     */
    @Column(name = "mermaid_code", columnDefinition = "LONGTEXT")
    private String mermaidCode;

    /**
     * 生成时间
     */
    @Column(name = "generated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date generatedAt;

    /**
     * 过期时间
     */
    @Column(name = "expires_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public void setGraphType(GraphType graphType) {
        this.graphType = graphType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getGraphData() {
        return graphData;
    }

    public void setGraphData(String graphData) {
        this.graphData = graphData;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getEdgeCount() {
        return edgeCount;
    }

    public void setEdgeCount(Integer edgeCount) {
        this.edgeCount = edgeCount;
    }

    public String getMermaidCode() {
        return mermaidCode;
    }

    public void setMermaidCode(String mermaidCode) {
        this.mermaidCode = mermaidCode;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
}
