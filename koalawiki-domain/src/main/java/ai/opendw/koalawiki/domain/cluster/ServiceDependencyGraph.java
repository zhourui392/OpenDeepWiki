package ai.opendw.koalawiki.domain.cluster;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 服务依赖图缓存
 * 用于存储生成的服务依赖关系图
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceDependencyGraph extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 所属集群ID
     */
    @NotBlank(message = "集群ID不能为空")
    private String clusterId;

    /**
     * 图类型
     */
    @NotNull(message = "图类型不能为空")
    private GraphType graphType;

    /**
     * 作用域ID（领域ID或服务ID，NULL表示全集群）
     */
    private String scopeId;

    /**
     * 图数据（JSON格式）
     */
    @NotBlank(message = "图数据不能为空")
    private String graphData;

    /**
     * 节点数
     */
    private Integer nodeCount = 0;

    /**
     * 边数
     */
    private Integer edgeCount = 0;

    /**
     * Mermaid图代码
     */
    private String mermaidCode;

    /**
     * 生成时间
     */
    @NotNull(message = "生成时间不能为空")
    private Date generatedAt;

    /**
     * 过期时间
     */
    private Date expiresAt;

    /**
     * 判断是否已过期
     *
     * @return 是否过期
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return new Date().after(expiresAt);
    }

    /**
     * 设置过期时间（小时）
     *
     * @param hours 小时数
     */
    public void setExpiresAfterHours(int hours) {
        long expiresTimeMillis = System.currentTimeMillis() + hours * 60 * 60 * 1000L;
        this.expiresAt = new Date(expiresTimeMillis);
    }

    /**
     * 刷新生成时间
     */
    public void refreshGeneratedAt() {
        this.generatedAt = new Date();
    }
}
