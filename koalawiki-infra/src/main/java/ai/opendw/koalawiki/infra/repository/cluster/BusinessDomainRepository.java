package ai.opendw.koalawiki.infra.repository.cluster;

import ai.opendw.koalawiki.infra.entity.cluster.BusinessDomainEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 业务领域仓储接口
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Repository
public interface BusinessDomainRepository extends JpaRepository<BusinessDomainEntity, String> {

    /**
     * 根据集群ID查询领域列表
     *
     * @param clusterId 集群ID
     * @return 领域列表
     */
    List<BusinessDomainEntity> findByClusterIdAndDeletedAtIsNullOrderBySortOrderAsc(String clusterId);

    /**
     * 根据集群ID分页查询
     *
     * @param clusterId 集群ID
     * @param pageable  分页参数
     * @return 领域分页
     */
    Page<BusinessDomainEntity> findByClusterIdAndDeletedAtIsNullOrderBySortOrderAsc(String clusterId, Pageable pageable);

    /**
     * 根据集群ID和编码查询
     *
     * @param clusterId 集群ID
     * @param code      领域编码
     * @return 领域实体
     */
    Optional<BusinessDomainEntity> findByClusterIdAndCodeAndDeletedAtIsNull(String clusterId, String code);

    /**
     * 根据ID查询未删除的领域
     *
     * @param id 领域ID
     * @return 领域实体
     */
    Optional<BusinessDomainEntity> findByIdAndDeletedAtIsNull(String id);

    /**
     * 检查编码是否存在
     *
     * @param clusterId 集群ID
     * @param code      编码
     * @return 是否存在
     */
    boolean existsByClusterIdAndCodeAndDeletedAtIsNull(String clusterId, String code);

    /**
     * 根据名称模糊查询
     *
     * @param clusterId 集群ID
     * @param name      名称关键词
     * @return 领域列表
     */
    List<BusinessDomainEntity> findByClusterIdAndNameContainingIgnoreCaseAndDeletedAtIsNull(String clusterId, String name);

    /**
     * 统计集群内领域数量
     *
     * @param clusterId 集群ID
     * @return 数量
     */
    long countByClusterIdAndDeletedAtIsNull(String clusterId);

    /**
     * 统计领域内服务数量
     *
     * @param domainId 领域ID
     * @return 服务数量
     */
    @Query("SELECT COUNT(sdm) FROM ServiceDomainMappingEntity sdm WHERE sdm.domainId = :domainId")
    long countServicesByDomainId(@Param("domainId") String domainId);

    /**
     * 获取默认未分类领域
     *
     * @param clusterId 集群ID
     * @return 未分类领域
     */
    @Query("SELECT d FROM BusinessDomainEntity d WHERE d.clusterId = :clusterId AND d.code = 'unclassified' AND d.deletedAt IS NULL")
    Optional<BusinessDomainEntity> findUnclassifiedDomain(@Param("clusterId") String clusterId);
}
