package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.DailyStatisticsEntity;
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
 * 每日统计仓储接口
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Repository
public interface DailyStatisticsRepository extends JpaRepository<DailyStatisticsEntity, String> {

    /**
     * 按统计日期查询
     *
     * @param statisticsDate 统计日期
     * @return 统计数据
     */
    Optional<DailyStatisticsEntity> findByStatisticsDate(Date statisticsDate);

    /**
     * 按仓库ID和统计日期查询
     *
     * @param warehouseId 仓库ID
     * @param statisticsDate 统计日期
     * @return 统计数据
     */
    Optional<DailyStatisticsEntity> findByWarehouseIdAndStatisticsDate(
            String warehouseId, Date statisticsDate);

    /**
     * 按时间范围查询统计数据
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 统计数据列表
     */
    List<DailyStatisticsEntity> findByStatisticsDateBetween(Date start, Date end);

    /**
     * 按仓库ID和时间范围查询
     *
     * @param warehouseId 仓库ID
     * @param start 开始日期
     * @param end 结束日期
     * @return 统计数据列表
     */
    List<DailyStatisticsEntity> findByWarehouseIdAndStatisticsDateBetween(
            String warehouseId, Date start, Date end);

    /**
     * 按仓库ID分页查询
     *
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<DailyStatisticsEntity> findByWarehouseId(String warehouseId, Pageable pageable);

    /**
     * 查询所有仓库的统计（warehouseId为null）
     *
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<DailyStatisticsEntity> findByWarehouseIdIsNull(Pageable pageable);

    /**
     * 按时间范围分页查询
     *
     * @param start 开始日期
     * @param end 结束日期
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<DailyStatisticsEntity> findByStatisticsDateBetween(
            Date start, Date end, Pageable pageable);

    /**
     * 查询最近N天的统计数据
     *
     * @param warehouseId 仓库ID
     * @param startDate 开始日期（N天前）
     * @return 统计数据列表
     */
    @Query("SELECT d FROM DailyStatisticsEntity d WHERE d.warehouseId = :warehouseId " +
           "AND d.statisticsDate >= :startDate " +
           "ORDER BY d.statisticsDate DESC")
    List<DailyStatisticsEntity> findRecentStatistics(
            @Param("warehouseId") String warehouseId,
            @Param("startDate") Date startDate);

    /**
     * 计算时间范围内的总PV
     *
     * @param warehouseId 仓库ID
     * @param start 开始日期
     * @param end 结束日期
     * @return 总PV
     */
    @Query("SELECT SUM(d.viewCount) FROM DailyStatisticsEntity d " +
           "WHERE d.warehouseId = :warehouseId " +
           "AND d.statisticsDate BETWEEN :start AND :end")
    Long sumViewCountByWarehouseIdAndDateRange(
            @Param("warehouseId") String warehouseId,
            @Param("start") Date start,
            @Param("end") Date end);

    /**
     * 计算时间范围内的平均UV
     *
     * @param warehouseId 仓库ID
     * @param start 开始日期
     * @param end 结束日期
     * @return 平均UV
     */
    @Query("SELECT AVG(d.uniqueUserCount) FROM DailyStatisticsEntity d " +
           "WHERE d.warehouseId = :warehouseId " +
           "AND d.statisticsDate BETWEEN :start AND :end")
    Double avgUniqueUserCountByWarehouseIdAndDateRange(
            @Param("warehouseId") String warehouseId,
            @Param("start") Date start,
            @Param("end") Date end);

    /**
     * 删除指定日期之前的统计数据
     *
     * @param date 日期
     */
    void deleteByStatisticsDateBefore(Date date);

    /**
     * 检查指定日期和仓库的统计数据是否存在
     *
     * @param warehouseId 仓库ID
     * @param statisticsDate 统计日期
     * @return 是否存在
     */
    boolean existsByWarehouseIdAndStatisticsDate(String warehouseId, Date statisticsDate);
}
