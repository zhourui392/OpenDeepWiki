package ai.opendw.koalawiki.infra.repository.cluster;

import ai.opendw.koalawiki.domain.cluster.ClusterStatus;
import ai.opendw.koalawiki.infra.entity.cluster.ServiceClusterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服务集群仓储接口
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Repository
public interface ServiceClusterRepository extends JpaRepository<ServiceClusterEntity, String> {

    /**
     * 根据编码查询集群
     *
     * @param code 集群编码
     * @return 集群实体
     */
    Optional<ServiceClusterEntity> findByCodeAndDeletedAtIsNull(String code);

    /**
     * 根据状态查询集群列表
     *
     * @param status   状态
     * @param pageable 分页参数
     * @return 集群分页
     */
    Page<ServiceClusterEntity> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(ClusterStatus status, Pageable pageable);

    /**
     * 查询所有未删除的集群
     *
     * @param pageable 分页参数
     * @return 集群分页
     */
    Page<ServiceClusterEntity> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查询所有活跃集群
     *
     * @return 集群列表
     */
    List<ServiceClusterEntity> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(ClusterStatus status);

    /**
     * 根据名称模糊查询
     *
     * @param name     名称关键词
     * @param pageable 分页参数
     * @return 集群分页
     */
    Page<ServiceClusterEntity> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    /**
     * 根据负责人查询
     *
     * @param owner    负责人
     * @param pageable 分页参数
     * @return 集群分页
     */
    Page<ServiceClusterEntity> findByOwnerAndDeletedAtIsNull(String owner, Pageable pageable);

    /**
     * 统计活跃集群数量
     *
     * @return 数量
     */
    long countByStatusAndDeletedAtIsNull(ClusterStatus status);

    /**
     * 检查编码是否存在
     *
     * @param code 编码
     * @return 是否存在
     */
    boolean existsByCodeAndDeletedAtIsNull(String code);

    /**
     * 根据ID查询未删除的集群
     *
     * @param id 集群ID
     * @return 集群实体
     */
    Optional<ServiceClusterEntity> findByIdAndDeletedAtIsNull(String id);

    /**
     * 统计集群内仓库数量
     *
     * @param clusterId 集群ID
     * @return 仓库数量
     */
    @Query("SELECT COUNT(cw) FROM ClusterWarehouseEntity cw WHERE cw.clusterId = :clusterId")
    long countWarehousesByClusterId(@Param("clusterId") String clusterId);

    /**
     * 统计集群内领域数量
     *
     * @param clusterId 集群ID
     * @return 领域数量
     */
    @Query("SELECT COUNT(d) FROM BusinessDomainEntity d WHERE d.clusterId = :clusterId AND d.deletedAt IS NULL")
    long countDomainsByClusterId(@Param("clusterId") String clusterId);
}
