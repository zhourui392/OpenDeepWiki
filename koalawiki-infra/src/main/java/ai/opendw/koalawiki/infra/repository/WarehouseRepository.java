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
     * 根据用户ID查询仓库列表
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 仓库分页
     */
    Page<WarehouseEntity> findByUserIdOrderByCreatedTimeDesc(String userId, Pageable pageable);

    /**
     * 根据状态查询仓库列表
     *
     * @param status   仓库状态
     * @param pageable 分页参数
     * @return 仓库分页
     */
    Page<WarehouseEntity> findByStatusOrderByCreatedTimeDesc(WarehouseStatus status, Pageable pageable);

    /**
     * 查询公开的仓库
     *
     * @param isPublic 是否公开
     * @param pageable 分页参数
     * @return 仓库分页
     */
    Page<WarehouseEntity> findByIsPublicOrderByViewCountDesc(Boolean isPublic, Pageable pageable);

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
     * @param autoSync 是否自动同步
     * @param status   仓库状态
     * @return 仓库列表
     */
    List<WarehouseEntity> findByAutoSyncAndStatus(Boolean autoSync, WarehouseStatus status);

    /**
     * 查询超过同步间隔的仓库
     *
     * @param autoSync     是否自动同步
     * @param status       仓库状态
     * @param syncDeadline 同步截止时间
     * @return 仓库列表
     */
    @Query("SELECT w FROM WarehouseEntity w WHERE w.autoSync = :autoSync " +
           "AND w.status = :status " +
           "AND (w.lastSyncTime IS NULL OR w.lastSyncTime < :syncDeadline)")
    List<WarehouseEntity> findWarehousesNeedSync(@Param("autoSync") Boolean autoSync,
                                                 @Param("status") WarehouseStatus status,
                                                 @Param("syncDeadline") Date syncDeadline);

    /**
     * 根据URL查询仓库
     *
     * @param url 仓库URL
     * @return 仓库实体
     */
    WarehouseEntity findByUrl(String url);

    /**
     * 统计用户的仓库数量
     *
     * @param userId 用户ID
     * @return 仓库数量
     */
    long countByUserId(String userId);

    /**
     * 更新仓库访问次数
     *
     * @param warehouseId 仓库ID
     */
    @Query("UPDATE WarehouseEntity w SET w.viewCount = w.viewCount + 1 WHERE w.id = :warehouseId")
    void incrementViewCount(@Param("warehouseId") String warehouseId);

    /**
     * 更新仓库星标数
     *
     * @param warehouseId 仓库ID
     * @param increment   增量（1或-1）
     */
    @Query("UPDATE WarehouseEntity w SET w.starCount = w.starCount + :increment WHERE w.id = :warehouseId")
    void updateStarCount(@Param("warehouseId") String warehouseId, @Param("increment") int increment);

    /**
     * 查询热门仓库（按访问量排序）
     *
     * @param isPublic 是否公开
     * @param pageable 分页参数
     * @return 仓库分页
     */
    Page<WarehouseEntity> findByIsPublicOrderByViewCountDescStarCountDesc(Boolean isPublic, Pageable pageable);

    /**
     * 查询需要自动同步的仓库（简化方法）
     *
     * @return 需要同步的仓库列表
     */
    default List<WarehouseEntity> findWarehousesForAutoSync() {
        // 查询启用自动同步且状态为已完成的仓库
        // 这里假设同步间隔是30分钟
        Date syncDeadline = new Date(System.currentTimeMillis() - 30 * 60 * 1000);
        return findWarehousesNeedSync(true, WarehouseStatus.COMPLETED, syncDeadline);
    }
}