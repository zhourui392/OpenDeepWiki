package ai.opendw.koalawiki.infra.entity.cluster;

import ai.opendw.koalawiki.infra.entity.BaseJpaEntity;

import javax.persistence.*;

/**
 * 集群仓库关联JPA实体
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Entity
@Table(name = "cluster_warehouse",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cluster_warehouse", columnNames = {"cluster_id", "warehouse_id"})
    },
    indexes = {
        @Index(name = "idx_cluster_id", columnList = "cluster_id"),
        @Index(name = "idx_warehouse_id", columnList = "warehouse_id")
    })
public class ClusterWarehouseEntity extends BaseJpaEntity {

    /**
     * 集群ID
     */
    @Column(name = "cluster_id", nullable = false, length = 36)
    private String clusterId;

    /**
     * 仓库ID
     */
    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    /**
     * 排序权重
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
