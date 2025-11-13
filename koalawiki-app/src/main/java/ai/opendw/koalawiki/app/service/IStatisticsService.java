package ai.opendw.koalawiki.app.service;

import ai.opendw.koalawiki.domain.log.DailyStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
public interface IStatisticsService {

    /**
     * 获取每日统计数据
     *
     * @param warehouseId 仓库ID（null表示全局统计）
     * @param date 统计日期
     * @return 统计数据
     */
    DailyStatistics getDailyStatistics(String warehouseId, Date date);

    /**
     * 获取时间范围内的统计数据
     *
     * @param warehouseId 仓库ID（null表示全局统计）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据列表
     */
    List<DailyStatistics> getRangeStatistics(String warehouseId, Date startDate, Date endDate);

    /**
     * 获取趋势数据
     *
     * @param warehouseId 仓库ID
     * @param days 天数
     * @return 趋势数据
     */
    Map<String, Object> getTrend(String warehouseId, Integer days);

    /**
     * 获取热门文档Top N
     *
     * @param warehouseId 仓库ID
     * @param limit 限制数量
     * @return 热门文档列表
     */
    List<Map<String, Object>> getTopDocuments(String warehouseId, Integer limit);

    /**
     * 获取统计摘要
     *
     * @param warehouseId 仓库ID
     * @return 统计摘要
     */
    Map<String, Object> getSummary(String warehouseId);

    /**
     * 分页查询统计数据
     *
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<DailyStatistics> listStatistics(String warehouseId, Pageable pageable);

    /**
     * 实时统计（从访问日志计算）
     *
     * @param warehouseId 仓库ID
     * @return 实时统计数据
     */
    Map<String, Object> getRealtimeStatistics(String warehouseId);
}
