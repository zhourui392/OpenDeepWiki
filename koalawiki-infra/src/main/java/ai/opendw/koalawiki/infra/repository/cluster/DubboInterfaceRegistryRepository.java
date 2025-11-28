package ai.opendw.koalawiki.infra.repository.cluster;

import ai.opendw.koalawiki.infra.entity.cluster.DubboInterfaceRegistryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Dubbo接口注册仓储接口
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Repository
public interface DubboInterfaceRegistryRepository extends JpaRepository<DubboInterfaceRegistryEntity, String> {

    /**
     * 根据集群ID查询接口列表
     *
     * @param clusterId 集群ID
     * @param pageable  分页参数
     * @return 接口分页
     */
    Page<DubboInterfaceRegistryEntity> findByClusterIdOrderByInterfaceNameAsc(String clusterId, Pageable pageable);

    /**
     * 根据接口名查询
     *
     * @param clusterId     集群ID
     * @param interfaceName 接口名
     * @param version       版本
     * @param groupName     分组
     * @return 接口实体
     */
    Optional<DubboInterfaceRegistryEntity> findByClusterIdAndInterfaceNameAndVersionAndGroupName(
            String clusterId, String interfaceName, String version, String groupName);

    /**
     * 根据接口名模糊查询
     *
     * @param clusterId 集群ID
     * @param keyword   关键词
     * @param pageable  分页参数
     * @return 接口分页
     */
    @Query("SELECT d FROM DubboInterfaceRegistryEntity d WHERE d.clusterId = :clusterId " +
           "AND (LOWER(d.interfaceName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DubboInterfaceRegistryEntity> searchByKeyword(
            @Param("clusterId") String clusterId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 根据提供者仓库ID查询
     *
     * @param providerWarehouseId 提供者仓库ID
     * @return 接口列表
     */
    List<DubboInterfaceRegistryEntity> findByProviderWarehouseId(String providerWarehouseId);

    /**
     * 查询废弃的接口
     *
     * @param clusterId 集群ID
     * @return 接口列表
     */
    List<DubboInterfaceRegistryEntity> findByClusterIdAndDeprecatedTrue(String clusterId);

    /**
     * 统计集群内接口数量
     *
     * @param clusterId 集群ID
     * @return 数量
     */
    long countByClusterId(String clusterId);

    /**
     * 统计仓库提供的接口数量
     *
     * @param providerWarehouseId 提供者仓库ID
     * @return 数量
     */
    long countByProviderWarehouseId(String providerWarehouseId);

    /**
     * 获取热门接口（按消费者数量排序）
     *
     * @param clusterId 集群ID
     * @param pageable  分页参数
     * @return 接口列表
     */
    @Query("SELECT d FROM DubboInterfaceRegistryEntity d WHERE d.clusterId = :clusterId " +
           "ORDER BY (SELECT COUNT(c) FROM DubboInterfaceConsumerEntity c WHERE c.interfaceRegistryId = d.id) DESC")
    List<DubboInterfaceRegistryEntity> findTopInterfacesByConsumerCount(
            @Param("clusterId") String clusterId, Pageable pageable);

    /**
     * 检查接口是否存在
     *
     * @param clusterId     集群ID
     * @param interfaceName 接口名
     * @param version       版本
     * @param groupName     分组
     * @return 是否存在
     */
    boolean existsByClusterIdAndInterfaceNameAndVersionAndGroupName(
            String clusterId, String interfaceName, String version, String groupName);
}
