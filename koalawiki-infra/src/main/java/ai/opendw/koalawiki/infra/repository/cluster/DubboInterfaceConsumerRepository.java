package ai.opendw.koalawiki.infra.repository.cluster;

import ai.opendw.koalawiki.infra.entity.cluster.DubboInterfaceConsumerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Dubbo接口消费者仓储接口
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Repository
public interface DubboInterfaceConsumerRepository extends JpaRepository<DubboInterfaceConsumerEntity, String> {

    /**
     * 根据接口注册ID查询消费者
     *
     * @param interfaceRegistryId 接口注册ID
     * @return 消费者列表
     */
    List<DubboInterfaceConsumerEntity> findByInterfaceRegistryId(String interfaceRegistryId);

    /**
     * 根据消费者仓库ID查询
     *
     * @param consumerWarehouseId 消费者仓库ID
     * @return 消费者列表
     */
    List<DubboInterfaceConsumerEntity> findByConsumerWarehouseId(String consumerWarehouseId);

    /**
     * 根据接口ID和消费者仓库ID查询
     *
     * @param interfaceRegistryId 接口注册ID
     * @param consumerWarehouseId 消费者仓库ID
     * @param sourceClass         来源类
     * @return 消费者实体
     */
    Optional<DubboInterfaceConsumerEntity> findByInterfaceRegistryIdAndConsumerWarehouseIdAndSourceClass(
            String interfaceRegistryId, String consumerWarehouseId, String sourceClass);

    /**
     * 检查消费者是否存在
     *
     * @param interfaceRegistryId 接口注册ID
     * @param consumerWarehouseId 消费者仓库ID
     * @return 是否存在
     */
    boolean existsByInterfaceRegistryIdAndConsumerWarehouseId(String interfaceRegistryId, String consumerWarehouseId);

    /**
     * 删除接口的所有消费者
     *
     * @param interfaceRegistryId 接口注册ID
     */
    @Modifying
    @Query("DELETE FROM DubboInterfaceConsumerEntity dic WHERE dic.interfaceRegistryId = :interfaceRegistryId")
    void deleteByInterfaceRegistryId(@Param("interfaceRegistryId") String interfaceRegistryId);

    /**
     * 删除仓库的所有消费记录
     *
     * @param consumerWarehouseId 消费者仓库ID
     */
    @Modifying
    @Query("DELETE FROM DubboInterfaceConsumerEntity dic WHERE dic.consumerWarehouseId = :consumerWarehouseId")
    void deleteByConsumerWarehouseId(@Param("consumerWarehouseId") String consumerWarehouseId);

    /**
     * 统计接口的消费者数量
     *
     * @param interfaceRegistryId 接口注册ID
     * @return 数量
     */
    long countByInterfaceRegistryId(String interfaceRegistryId);

    /**
     * 获取接口的消费者仓库ID列表
     *
     * @param interfaceRegistryId 接口注册ID
     * @return 仓库ID列表
     */
    @Query("SELECT dic.consumerWarehouseId FROM DubboInterfaceConsumerEntity dic WHERE dic.interfaceRegistryId = :interfaceRegistryId")
    List<String> findConsumerWarehouseIdsByInterfaceRegistryId(@Param("interfaceRegistryId") String interfaceRegistryId);

    /**
     * 获取仓库消费的接口ID列表
     *
     * @param consumerWarehouseId 消费者仓库ID
     * @return 接口ID列表
     */
    @Query("SELECT dic.interfaceRegistryId FROM DubboInterfaceConsumerEntity dic WHERE dic.consumerWarehouseId = :consumerWarehouseId")
    List<String> findInterfaceIdsByConsumerWarehouseId(@Param("consumerWarehouseId") String consumerWarehouseId);
}
