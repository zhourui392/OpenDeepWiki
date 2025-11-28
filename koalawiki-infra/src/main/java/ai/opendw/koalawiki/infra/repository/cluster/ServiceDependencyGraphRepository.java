package ai.opendw.koalawiki.infra.repository.cluster;

import ai.opendw.koalawiki.domain.cluster.GraphType;
import ai.opendw.koalawiki.infra.entity.cluster.ServiceDependencyGraphEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 服务依赖图仓储接口
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Repository
public interface ServiceDependencyGraphRepository extends JpaRepository<ServiceDependencyGraphEntity, String> {

    /**
     * 根据集群ID和图类型查询
     *
     * @param clusterId 集群ID
     * @param graphType 图类型
     * @param scopeId   作用域ID
     * @return 依赖图实体
     */
    Optional<ServiceDependencyGraphEntity> findByClusterIdAndGraphTypeAndScopeId(
            String clusterId, GraphType graphType, String scopeId);

    /**
     * 根据集群ID查询所有图
     *
     * @param clusterId 集群ID
     * @return 依赖图列表
     */
    List<ServiceDependencyGraphEntity> findByClusterId(String clusterId);

    /**
     * 查询未过期的图
     *
     * @param clusterId 集群ID
     * @param graphType 图类型
     * @param scopeId   作用域ID
     * @param now       当前时间
     * @return 依赖图实体
     */
    @Query("SELECT g FROM ServiceDependencyGraphEntity g WHERE g.clusterId = :clusterId " +
           "AND g.graphType = :graphType AND (g.scopeId = :scopeId OR (g.scopeId IS NULL AND :scopeId IS NULL)) " +
           "AND (g.expiresAt IS NULL OR g.expiresAt > :now)")
    Optional<ServiceDependencyGraphEntity> findValidGraph(
            @Param("clusterId") String clusterId,
            @Param("graphType") GraphType graphType,
            @Param("scopeId") String scopeId,
            @Param("now") Date now);

    /**
     * 删除过期的图
     *
     * @param now 当前时间
     */
    @Modifying
    @Query("DELETE FROM ServiceDependencyGraphEntity g WHERE g.expiresAt IS NOT NULL AND g.expiresAt < :now")
    void deleteExpiredGraphs(@Param("now") Date now);

    /**
     * 删除集群的所有图
     *
     * @param clusterId 集群ID
     */
    @Modifying
    @Query("DELETE FROM ServiceDependencyGraphEntity g WHERE g.clusterId = :clusterId")
    void deleteByClusterId(@Param("clusterId") String clusterId);

    /**
     * 检查图是否存在
     *
     * @param clusterId 集群ID
     * @param graphType 图类型
     * @param scopeId   作用域ID
     * @return 是否存在
     */
    boolean existsByClusterIdAndGraphTypeAndScopeId(String clusterId, GraphType graphType, String scopeId);
}
