package ai.opendw.koalawiki.infra.repository.cluster;

import ai.opendw.koalawiki.domain.cluster.ServiceType;
import ai.opendw.koalawiki.infra.entity.cluster.ServiceDomainMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服务领域映射仓储接口
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Repository
public interface ServiceDomainMappingRepository extends JpaRepository<ServiceDomainMappingEntity, String> {

    /**
     * 根据领域ID查询服务映射
     *
     * @param domainId 领域ID
     * @return 映射列表
     */
    List<ServiceDomainMappingEntity> findByDomainIdOrderBySortOrderAsc(String domainId);

    /**
     * 根据仓库ID查询服务映射
     *
     * @param warehouseId 仓库ID
     * @return 映射列表
     */
    List<ServiceDomainMappingEntity> findByWarehouseId(String warehouseId);

    /**
     * 查询仓库的主领域映射
     *
     * @param warehouseId 仓库ID
     * @return 映射实体
     */
    Optional<ServiceDomainMappingEntity> findByWarehouseIdAndIsPrimaryTrue(String warehouseId);

    /**
     * 根据领域ID和仓库ID查询
     *
     * @param domainId    领域ID
     * @param warehouseId 仓库ID
     * @return 映射实体
     */
    Optional<ServiceDomainMappingEntity> findByDomainIdAndWarehouseId(String domainId, String warehouseId);

    /**
     * 检查映射是否存在
     *
     * @param domainId    领域ID
     * @param warehouseId 仓库ID
     * @return 是否存在
     */
    boolean existsByDomainIdAndWarehouseId(String domainId, String warehouseId);

    /**
     * 根据服务类型查询
     *
     * @param domainId    领域ID
     * @param serviceType 服务类型
     * @return 映射列表
     */
    List<ServiceDomainMappingEntity> findByDomainIdAndServiceType(String domainId, ServiceType serviceType);

    /**
     * 删除领域的所有映射
     *
     * @param domainId 领域ID
     */
    @Modifying
    @Query("DELETE FROM ServiceDomainMappingEntity sdm WHERE sdm.domainId = :domainId")
    void deleteByDomainId(@Param("domainId") String domainId);

    /**
     * 删除仓库的所有映射
     *
     * @param warehouseId 仓库ID
     */
    @Modifying
    @Query("DELETE FROM ServiceDomainMappingEntity sdm WHERE sdm.warehouseId = :warehouseId")
    void deleteByWarehouseId(@Param("warehouseId") String warehouseId);

    /**
     * 统计领域内服务数量
     *
     * @param domainId 领域ID
     * @return 数量
     */
    long countByDomainId(String domainId);

    /**
     * 获取领域内仓库ID列表
     *
     * @param domainId 领域ID
     * @return 仓库ID列表
     */
    @Query("SELECT sdm.warehouseId FROM ServiceDomainMappingEntity sdm WHERE sdm.domainId = :domainId ORDER BY sdm.sortOrder ASC")
    List<String> findWarehouseIdsByDomainId(@Param("domainId") String domainId);

    /**
     * 清除仓库的主领域标记
     *
     * @param warehouseId 仓库ID
     */
    @Modifying
    @Query("UPDATE ServiceDomainMappingEntity sdm SET sdm.isPrimary = false WHERE sdm.warehouseId = :warehouseId")
    void clearPrimaryByWarehouseId(@Param("warehouseId") String warehouseId);
}
