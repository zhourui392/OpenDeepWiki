package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 仓库仓储接口
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Repository
public interface WarehouseRepository extends JpaRepository<WarehouseEntity, String> {

    /**
     * 根据状态查询仓库列表
     *
     * @param status   仓库状态
     * @param pageable 分页参数
     * @return 仓库分页
     */
    Page<WarehouseEntity> findByStatusOrderByCreatedAtDesc(WarehouseStatus status, Pageable pageable);

    /**
     * 根据名称模糊查询仓库
     *
     * @param name     仓库名称（模糊匹配）
     * @param pageable 分页参数
     * @return 仓库分页
     */
    Page<WarehouseEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 查询需要自动同步的仓库
     *
     * @param enableSync 是否启用同步
     * @param status     仓库状态
     * @return 仓库列表
     */
    List<WarehouseEntity> findByEnableSyncAndStatus(Boolean enableSync, WarehouseStatus status);

    /**
     * 根据地址查询仓库
     *
     * @param address 仓库地址
     * @return 仓库实体
     */
    WarehouseEntity findByAddress(String address);

    /**
     * 更新仓库星标数
     *
     * @param warehouseId 仓库ID
     * @param increment   增量（1或-1）
     */
    @Query("UPDATE WarehouseEntity w SET w.stars = w.stars + :increment WHERE w.id = :warehouseId")
    void updateStars(@Param("warehouseId") String warehouseId, @Param("increment") int increment);
}