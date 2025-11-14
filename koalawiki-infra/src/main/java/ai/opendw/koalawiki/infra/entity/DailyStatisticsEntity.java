package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;

/**
 * 每日统计JPA实体
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "daily_statistics", indexes = {
        @Index(name = "idx_statistics_date", columnList = "statistics_date"),
        @Index(name = "idx_daily_stats_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_daily_stats_warehouse_date", columnList = "warehouse_id,statistics_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_stats_warehouse_date", columnNames = {"warehouse_id", "statistics_date"})
})
public class DailyStatisticsEntity extends BaseJpaEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 统计日期
     */
    @Column(name = "statistics_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date statisticsDate;

    /**
     * 仓库ID（可选，null表示全局统计）
     */
    @Column(name = "warehouse_id", length = 50)
    private String warehouseId;

    /**
     * 页面浏览量（PV）
     */
    @Column(name = "view_count")
    private Long viewCount;

    /**
     * 独立访客数（UV）
     */
    @Column(name = "unique_user_count")
    private Long uniqueUserCount;

    /**
     * 独立IP数
     */
    @Column(name = "unique_ip_count")
    private Long uniqueIpCount;

    /**
     * 文档访问次数
     */
    @Column(name = "document_view_count")
    private Long documentViewCount;

    /**
     * 搜索次数
     */
    @Column(name = "search_count")
    private Long searchCount;

    /**
     * 下载次数
     */
    @Column(name = "download_count")
    private Long downloadCount;

    /**
     * 平均响应时间（毫秒）
     */
    @Column(name = "avg_response_time")
    private Double avgResponseTime;

    /**
     * 最大响应时间（毫秒）
     */
    @Column(name = "max_response_time")
    private Integer maxResponseTime;

    /**
     * 最小响应时间（毫秒）
     */
    @Column(name = "min_response_time")
    private Integer minResponseTime;

    /**
     * 总请求数
     */
    @Column(name = "total_requests")
    private Long totalRequests;

    /**
     * 成功请求数（状态码2xx）
     */
    @Column(name = "success_requests")
    private Long successRequests;

    /**
     * 失败请求数（状态码4xx, 5xx）
     */
    @Column(name = "failed_requests")
    private Long failedRequests;

    /**
     * 错误率（%）
     */
    @Column(name = "error_rate")
    private Double errorRate;

    /**
     * 各动作的统计次数（JSON格式）
     */
    @Column(name = "action_counts", columnDefinition = "TEXT")
    private String actionCounts;

    /**
     * 热门文档Top10（JSON格式）
     */
    @Column(name = "top_documents", columnDefinition = "TEXT")
    private String topDocuments;

    /**
     * 热门搜索关键词Top10（JSON格式）
     */
    @Column(name = "top_search_keywords", columnDefinition = "TEXT")
    private String topSearchKeywords;

    /**
     * 新增用户数
     */
    @Column(name = "new_user_count")
    private Long newUserCount;

    /**
     * 活跃用户数
     */
    @Column(name = "active_user_count")
    private Long activeUserCount;

    /**
     * 备注
     */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /**
     * 计算完成时间
     */
    @Column(name = "calculated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date calculatedAt;
}
