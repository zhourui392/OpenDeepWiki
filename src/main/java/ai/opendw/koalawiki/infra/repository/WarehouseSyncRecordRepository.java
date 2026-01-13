package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncStatus;
import ai.opendw.koalawiki.infra.entity.WarehouseSyncRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 仓库同步记录仓储接口
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Repository
public interface WarehouseSyncRecordRepository extends JpaRepository<WarehouseSyncRecordEntity, String> {

    /**
     * 根据仓库ID查询同步记录（分页）
     *
     * @param warehouseId 仓库ID
     * @param pageable    分页参数
     * @return 同步记录分页
     */
    Page<WarehouseSyncRecordEntity> findByWarehouseIdOrderByCreatedAtDesc(String warehouseId, Pageable pageable);

    /**
     * 根据仓库ID查询最新的同步记录
     */
    Optional<WarehouseSyncRecordEntity> findFirstByWarehouseIdOrderByCreatedAtDesc(String warehouseId);

    /**
     * 根据仓库ID和状态统计记录数
     */
    long countByWarehouseIdAndStatus(String warehouseId, WarehouseSyncStatus status);

    /**
     * 根据仓库ID和状态查询成功记录（Top N）
     */
    List<WarehouseSyncRecordEntity> findTop10ByWarehouseIdAndStatusOrderByCreatedAtDesc(
            String warehouseId, WarehouseSyncStatus status);

    /**
     * 删除指定仓库指定时间之前的记录
     */
    int deleteByWarehouseIdAndCreatedAtBefore(String warehouseId, Date before);

    /**
     * 根据仓库ID查询同步记录（分页）
     *
     * @param warehouseId 仓库ID
     * @param pageable    分页参数
     * @return 同步记录分页
     */
    Page<WarehouseSyncRecordEntity> findByWarehouseIdOrderByStartTimeDesc(String warehouseId, Pageable pageable);

    /**
     * 根据仓库ID和状态查询同步记录
     *
     * @param warehouseId 仓库ID
     * @param status      同步状态
     * @return 同步记录列表
     */
    List<WarehouseSyncRecordEntity> findByWarehouseIdAndStatus(String warehouseId, WarehouseSyncStatus status);

    /**
     * 查询仓库最新的同步记录
     *
     * @param warehouseId 仓库ID
     * @return 最新的同步记录
     */
    Optional<WarehouseSyncRecordEntity> findFirstByWarehouseIdOrderByStartTimeDesc(String warehouseId);

    /**
     * 查询仓库最新的成功同步记录
     *
     * @param warehouseId 仓库ID
     * @return 最新的成功同步记录
     */
    Optional<WarehouseSyncRecordEntity> findFirstByWarehouseIdAndStatusOrderByStartTimeDesc(
            String warehouseId, WarehouseSyncStatus status);

    /**
     * 统计仓库的同步记录数量
     *
     * @param warehouseId 仓库ID
     * @return 记录数量
     */
    long countByWarehouseId(String warehouseId);

    /**
     * 统计指定时间范围内的同步记录数量
     *
     * @param warehouseId 仓库ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 记录数量
     */
    @Query("SELECT COUNT(r) FROM WarehouseSyncRecordEntity r WHERE r.warehouseId = :warehouseId " +
           "AND r.startTime >= :startTime AND r.startTime <= :endTime")
    long countByWarehouseIdAndTimeRange(@Param("warehouseId") String warehouseId,
                                        @Param("startTime") Date startTime,
                                        @Param("endTime") Date endTime);

    /**
     * 查询正在进行的同步记录
     *
     * @param status 同步状态（IN_PROGRESS）
     * @return 同步记录列表
     */
    List<WarehouseSyncRecordEntity> findByStatus(WarehouseSyncStatus status);

    /**
     * 查询超时的同步记录（超过指定时间仍在进行中）
     *
     * @param status      同步状态（IN_PROGRESS）
     * @param timeoutTime 超时时间点
     * @return 超时的同步记录列表
     */
    @Query("SELECT r FROM WarehouseSyncRecordEntity r WHERE r.status = :status AND r.startTime < :timeoutTime")
    List<WarehouseSyncRecordEntity> findTimeoutSyncRecords(@Param("status") WarehouseSyncStatus status,
                                                           @Param("timeoutTime") Date timeoutTime);

    /**
     * 删除指定时间之前的同步记录（用于清理历史数据）
     *
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Query("DELETE FROM WarehouseSyncRecordEntity r WHERE r.startTime < :beforeTime")
    int deleteRecordsBeforeTime(@Param("beforeTime") Date beforeTime);

    /**
     * 查询仓库是否正在同步
     *
     * @param warehouseId 仓库ID
     * @param status      同步状态（IN_PROGRESS）
     * @return 是否存在正在进行的同步
     */
    boolean existsByWarehouseIdAndStatus(String warehouseId, WarehouseSyncStatus status);

    /**
     * 查询仓库是否正在同步（简化方法）
     *
     * @param warehouseId 仓库ID
     * @return 是否正在同步
     */
    default boolean isWarehouseSyncing(String warehouseId) {
        return existsByWarehouseIdAndStatus(warehouseId, WarehouseSyncStatus.IN_PROGRESS);
    }

    /**
     * 查询超时的同步记录（重命名方法）
     */
    default List<WarehouseSyncRecordEntity> findTimeoutRecords(WarehouseSyncStatus status, Date timeoutTime) {
        return findTimeoutSyncRecords(status, timeoutTime);
    }

    /**
     * 根据开始时间统计记录数
     */
    long countByStartTimeAfter(Date since);

    /**
     * 根据状态和开始时间统计记录数
     */
    long countByStatusAndStartTimeAfter(WarehouseSyncStatus status, Date since);

    /**
     * 根据状态统计记录数
     */
    long countByStatus(WarehouseSyncStatus status);

    /**
     * 根据状态和开始时间查询记录
     */
    List<WarehouseSyncRecordEntity> findByStatusAndStartTimeBefore(WarehouseSyncStatus status, Date before);
}