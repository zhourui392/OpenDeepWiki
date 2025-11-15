package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.AccessLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 访问日志仓储接口
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Repository
public interface AccessLogRepository extends JpaRepository<AccessLogEntity, String> {

    /**
     * 按仓库ID分页查询访问日志
     *
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<AccessLogEntity> findByWarehouseId(String warehouseId, Pageable pageable);

    /**
     * 按仓库ID分页查询访问日志（按时间倒序）
     *
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<AccessLogEntity> findByWarehouseIdOrderByAccessTimeDesc(String warehouseId, Pageable pageable);

    /**
     * 按用户ID分页查询访问日志
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<AccessLogEntity> findByUserId(String userId, Pageable pageable);

    /**
     * 按时间范围查询访问日志
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 访问日志列表
     */
    List<AccessLogEntity> findByAccessTimeBetween(Date start, Date end);

    /**
     * 按仓库ID和时间范围查询访问日志
     *
     * @param warehouseId 仓库ID
     * @param start 开始时间
     * @param end 结束时间
     * @return 访问日志列表
     */
    List<AccessLogEntity> findByWarehouseIdAndAccessTimeBetween(
            String warehouseId, Date start, Date end);

    /**
     * 按动作类型和时间范围查询
     *
     * @param action 动作类型
     * @param start 开始时间
     * @param end 结束时间
     * @return 访问日志列表
     */
    List<AccessLogEntity> findByActionAndAccessTimeBetween(
            String action, Date start, Date end);

    /**
     * 统计指定时间范围内的访问次数
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 访问次数
     */
    @Query("SELECT COUNT(a) FROM AccessLogEntity a WHERE a.accessTime BETWEEN :start AND :end")
    Long countByAccessTimeBetween(@Param("start") Date start, @Param("end") Date end);

    /**
     * 统计指定仓库在时间范围内的访问次数
     *
     * @param warehouseId 仓库ID
     * @param start 开始时间
     * @param end 结束时间
     * @return 访问次数
     */
    @Query("SELECT COUNT(a) FROM AccessLogEntity a WHERE a.warehouseId = :warehouseId " +
           "AND a.accessTime BETWEEN :start AND :end")
    Long countByWarehouseIdAndAccessTimeBetween(
            @Param("warehouseId") String warehouseId,
            @Param("start") Date start,
            @Param("end") Date end);

    /**
     * 统计指定时间范围内的独立用户数
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 独立用户数
     */
    @Query("SELECT COUNT(DISTINCT a.userId) FROM AccessLogEntity a " +
           "WHERE a.accessTime BETWEEN :start AND :end AND a.userId IS NOT NULL")
    Long countDistinctUserByAccessTimeBetween(
            @Param("start") Date start, @Param("end") Date end);

    /**
     * 统计指定时间范围内的独立IP数
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 独立IP数
     */
    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AccessLogEntity a " +
           "WHERE a.accessTime BETWEEN :start AND :end")
    Long countDistinctIpByAccessTimeBetween(
            @Param("start") Date start, @Param("end") Date end);

    /**
     * 获取指定时间范围内的平均响应时间
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 平均响应时间（毫秒）
     */
    @Query("SELECT AVG(a.responseTime) FROM AccessLogEntity a " +
           "WHERE a.accessTime BETWEEN :start AND :end")
    Double avgResponseTimeByAccessTimeBetween(
            @Param("start") Date start, @Param("end") Date end);

    /**
     * 删除指定日期之前的访问日志
     *
     * @param date 日期
     */
    void deleteByAccessTimeBefore(Date date);

    /**
     * 按状态码统计
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param minStatus 最小状态码
     * @param maxStatus 最大状态码
     * @return 统计数量
     */
    @Query("SELECT COUNT(a) FROM AccessLogEntity a WHERE a.accessTime BETWEEN :start AND :end " +
           "AND a.statusCode BETWEEN :minStatus AND :maxStatus")
    Long countByStatusCodeRange(
            @Param("start") Date start,
            @Param("end") Date end,
            @Param("minStatus") Integer minStatus,
            @Param("maxStatus") Integer maxStatus);

    /**
     * 获取热门文档（按访问次数排序）
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param action 动作类型
     * @param limit 限制数量
     * @return 文档ID和访问次数列表
     */
    @Query(value = "SELECT document_id, COUNT(*) as count FROM access_log " +
           "WHERE access_time BETWEEN :start AND :end AND action = :action " +
           "AND document_id IS NOT NULL " +
           "GROUP BY document_id ORDER BY count DESC LIMIT :limit",
           nativeQuery = true)
    List<Object[]> findTopDocuments(
            @Param("start") Date start,
            @Param("end") Date end,
            @Param("action") String action,
            @Param("limit") Integer limit);
}
