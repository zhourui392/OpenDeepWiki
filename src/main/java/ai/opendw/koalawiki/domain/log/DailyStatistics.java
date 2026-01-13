package ai.opendw.koalawiki.domain.log;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.Map;

/**
 * 每日统计数据实体
 * 记录每天的访问统计信息
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DailyStatistics extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 统计日期
     */
    private Date statisticsDate;

    /**
     * 仓库ID（可选，null表示全局统计）
     */
    private String warehouseId;

    /**
     * 页面浏览量（PV）
     */
    private Long viewCount;

    /**
     * 独立访客数（UV）
     */
    private Long uniqueUserCount;

    /**
     * 独立IP数
     */
    private Long uniqueIpCount;

    /**
     * 文档访问次数
     */
    private Long documentViewCount;

    /**
     * 搜索次数
     */
    private Long searchCount;

    /**
     * 下载次数
     */
    private Long downloadCount;

    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTime;

    /**
     * 最大响应时间（毫秒）
     */
    private Integer maxResponseTime;

    /**
     * 最小响应时间（毫秒）
     */
    private Integer minResponseTime;

    /**
     * 总请求数
     */
    private Long totalRequests;

    /**
     * 成功请求数（状态码2xx）
     */
    private Long successRequests;

    /**
     * 失败请求数（状态码4xx, 5xx）
     */
    private Long failedRequests;

    /**
     * 错误率（%）
     */
    private Double errorRate;

    /**
     * 各动作的统计次数（JSON格式）
     * 例如：{"VIEW": 1000, "SEARCH": 200, "DOWNLOAD": 50}
     */
    private String actionCounts;

    /**
     * 热门文档Top10（JSON格式）
     * 例如：[{"docId": "doc1", "title": "README", "views": 500}, ...]
     */
    private String topDocuments;

    /**
     * 热门搜索关键词Top10（JSON格式）
     * 例如：[{"keyword": "api", "count": 100}, ...]
     */
    private String topSearchKeywords;

    /**
     * 新增用户数
     */
    private Long newUserCount;

    /**
     * 活跃用户数
     */
    private Long activeUserCount;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 计算完成时间
     */
    private Date calculatedAt;
}
