package ai.opendw.koalawiki.infra.repository.cluster;

import ai.opendw.koalawiki.infra.entity.cluster.ClusterWarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 集群仓库关联仓储接口
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Repository
public interface ClusterWarehouseRepository extends JpaRepository<ClusterWarehouseEntity, String> {

    /**
     * 根据集群ID查询关联
     *
     * @param clusterId 集群ID
     * @return 关联列表
     */
    List<ClusterWarehouseEntity> findByClusterIdOrderBySortOrderAsc(String clusterId);

    /**
     * 根据仓库ID查询关联
     *
     * @param warehouseId 仓库ID
     * @return 关联实体
     */
    Optional<ClusterWarehouseEntity> findByWarehouseId(String warehouseId);

    /**
     * 根据集群ID和仓库ID查询
     *
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     * @return 关联实体
     */
    Optional<ClusterWarehouseEntity> findByClusterIdAndWarehouseId(String clusterId, String warehouseId);

    /**
     * 检查关联是否存在
     *
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     * @return 是否存在
     */
    boolean existsByClusterIdAndWarehouseId(String clusterId, String warehouseId);

    /**
     * 删除集群的所有关联
     *
     * @param clusterId 集群ID
     */
    @Modifying
    @Query("DELETE FROM ClusterWarehouseEntity cw WHERE cw.clusterId = :clusterId")
    void deleteByClusterId(@Param("clusterId") String clusterId);

    /**
     * 删除仓库的所有关联
     *
     * @param warehouseId 仓库ID
     */
    @Modifying
    @Query("DELETE FROM ClusterWarehouseEntity cw WHERE cw.warehouseId = :warehouseId")
    void deleteByWarehouseId(@Param("warehouseId") String warehouseId);

    /**
     * 统计集群内仓库数量
     *
     * @param clusterId 集群ID
     * @return 数量
     */
    long countByClusterId(String clusterId);

    /**
     * 获取集群内仓库ID列表
     *
     * @param clusterId 集群ID
     * @return 仓库ID列表
     */
    @Query("SELECT cw.warehouseId FROM ClusterWarehouseEntity cw WHERE cw.clusterId = :clusterId ORDER BY cw.sortOrder ASC")
    List<String> findWarehouseIdsByClusterId(@Param("clusterId") String clusterId);

    /**
     * 根据仓库ID查询所属集群ID
     *
     * @param warehouseId 仓库ID
     * @return 集群ID
     */
    @Query("SELECT cw.clusterId FROM ClusterWarehouseEntity cw WHERE cw.warehouseId = :warehouseId")
    Optional<String> findClusterIdByWarehouseId(@Param("warehouseId") String warehouseId);
}
