package ai.opendw.koalawiki.app.task;

import ai.opendw.koalawiki.domain.log.AccessLog;
import ai.opendw.koalawiki.domain.log.ActionType;
import ai.opendw.koalawiki.domain.log.DailyStatistics;
import ai.opendw.koalawiki.infra.entity.AccessLogEntity;
import ai.opendw.koalawiki.infra.repository.AccessLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计数据计算器
 * 根据访问日志计算各项统计指标
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Service
public class StatisticsCalculator {

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 计算指定日期的统计数据
     *
     * @param date 统计日期
     * @param warehouseId 仓库ID（null表示全局统计）
     * @return 统计数据
     */
    public DailyStatistics calculate(Date date, String warehouseId) {
        try {
            log.info("Calculating statistics for date: {}, warehouse: {}", date, warehouseId);

            // 获取日期范围
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date startDate = calendar.getTime();

            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date endDate = calendar.getTime();

            // 查询访问日志
            List<AccessLogEntity> logEntities;
            if (warehouseId == null) {
                logEntities = accessLogRepository.findByAccessTimeBetween(startDate, endDate);
            } else {
                logEntities = accessLogRepository.findByWarehouseIdAndAccessTimeBetween(
                        warehouseId, startDate, endDate);
            }

            // 转换为Domain
            List<AccessLog> logs = logEntities.stream()
                    .map(this::convertToDomain)
                    .collect(Collectors.toList());

            // 计算统计数据
            DailyStatistics statistics = new DailyStatistics();
            statistics.setId(UUID.randomUUID().toString().replace("-", ""));
            statistics.setStatisticsDate(startDate);
            statistics.setWarehouseId(warehouseId);
            statistics.setCreatedAt(new Date());
            statistics.setCalculatedAt(new Date());

            // 基本统计
            calculateBasicStats(logs, statistics);

            // 响应时间统计
            calculateResponseTimeStats(logs, statistics);

            // 状态码统计
            calculateStatusCodeStats(logs, statistics, startDate, endDate);

            // 动作统计
            calculateActionStats(logs, statistics);

            // 热门文档
            calculateTopDocuments(logs, statistics, startDate, endDate);

            log.info("Successfully calculated statistics, total requests: {}", statistics.getTotalRequests());
            return statistics;

        } catch (Exception e) {
            log.error("Failed to calculate statistics", e);
            return null;
        }
    }

    /**
     * 计算基本统计指标
     */
    private void calculateBasicStats(List<AccessLog> logs, DailyStatistics statistics) {
        // 总请求数
        statistics.setTotalRequests((long) logs.size());

        // 独立用户数
        long uniqueUsers = logs.stream()
                .filter(log -> log.getUserId() != null)
                .map(AccessLog::getUserId)
                .distinct()
                .count();
        statistics.setUniqueUserCount(uniqueUsers);

        // 独立IP数
        long uniqueIps = logs.stream()
                .filter(log -> log.getIpAddress() != null)
                .map(AccessLog::getIpAddress)
                .distinct()
                .count();
        statistics.setUniqueIpCount(uniqueIps);

        // PV（VIEW动作的次数）
        long viewCount = logs.stream()
                .filter(log -> ActionType.VIEW.getCode().equals(log.getAction()))
                .count();
        statistics.setViewCount(viewCount);

        // 文档访问次数
        long docViewCount = logs.stream()
                .filter(log -> log.getDocumentId() != null)
                .filter(log -> ActionType.VIEW.getCode().equals(log.getAction()))
                .count();
        statistics.setDocumentViewCount(docViewCount);

        // 搜索次数
        long searchCount = logs.stream()
                .filter(log -> ActionType.SEARCH.getCode().equals(log.getAction()))
                .count();
        statistics.setSearchCount(searchCount);

        // 下载次数
        long downloadCount = logs.stream()
                .filter(log -> ActionType.DOWNLOAD.getCode().equals(log.getAction()))
                .count();
        statistics.setDownloadCount(downloadCount);
    }

    /**
     * 计算响应时间统计
     */
    private void calculateResponseTimeStats(List<AccessLog> logs, DailyStatistics statistics) {
        List<Integer> responseTimes = logs.stream()
                .filter(log -> log.getResponseTime() != null && log.getResponseTime() > 0)
                .map(AccessLog::getResponseTime)
                .collect(Collectors.toList());

        if (!responseTimes.isEmpty()) {
            // 平均响应时间
            double avgResponseTime = responseTimes.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            statistics.setAvgResponseTime(avgResponseTime);

            // 最大响应时间
            int maxResponseTime = responseTimes.stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
            statistics.setMaxResponseTime(maxResponseTime);

            // 最小响应时间
            int minResponseTime = responseTimes.stream()
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElse(0);
            statistics.setMinResponseTime(minResponseTime);
        }
    }

    /**
     * 计算状态码统计
     */
    private void calculateStatusCodeStats(List<AccessLog> logs, DailyStatistics statistics,
                                          Date startDate, Date endDate) {
        // 成功请求数（2xx）
        long successRequests = logs.stream()
                .filter(log -> log.getStatusCode() != null)
                .filter(log -> log.getStatusCode() >= 200 && log.getStatusCode() < 300)
                .count();
        statistics.setSuccessRequests(successRequests);

        // 失败请求数（4xx, 5xx）
        long failedRequests = logs.stream()
                .filter(log -> log.getStatusCode() != null)
                .filter(log -> log.getStatusCode() >= 400)
                .count();
        statistics.setFailedRequests(failedRequests);

        // 错误率
        if (statistics.getTotalRequests() > 0) {
            double errorRate = (double) failedRequests / statistics.getTotalRequests() * 100;
            statistics.setErrorRate(errorRate);
        } else {
            statistics.setErrorRate(0.0);
        }
    }

    /**
     * 计算各动作的统计次数
     */
    private void calculateActionStats(List<AccessLog> logs, DailyStatistics statistics) {
        Map<String, Long> actionCounts = logs.stream()
                .filter(log -> log.getAction() != null)
                .collect(Collectors.groupingBy(
                        AccessLog::getAction,
                        Collectors.counting()
                ));

        try {
            String actionCountsJson = objectMapper.writeValueAsString(actionCounts);
            statistics.setActionCounts(actionCountsJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize action counts", e);
        }
    }

    /**
     * 计算热门文档Top10
     */
    private void calculateTopDocuments(List<AccessLog> logs, DailyStatistics statistics,
                                       Date startDate, Date endDate) {
        // 统计文档访问次数
        Map<String, Long> docCounts = logs.stream()
                .filter(log -> log.getDocumentId() != null)
                .filter(log -> ActionType.VIEW.getCode().equals(log.getAction()))
                .collect(Collectors.groupingBy(
                        AccessLog::getDocumentId,
                        Collectors.counting()
                ));

        // 取Top10
        List<Map.Entry<String, Long>> topDocs = docCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        // 转换为JSON
        List<Map<String, Object>> topDocsList = topDocs.stream()
                .map(entry -> {
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("docId", entry.getKey());
                    doc.put("views", entry.getValue());
                    return doc;
                })
                .collect(Collectors.toList());

        try {
            String topDocsJson = objectMapper.writeValueAsString(topDocsList);
            statistics.setTopDocuments(topDocsJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize top documents", e);
        }
    }

    /**
     * 生成周报
     */
    public void generateWeeklyReport(Date startDate, Date endDate) {
        log.info("Generating weekly report from {} to {}", startDate, endDate);
        // TODO: 实现周报生成逻辑
    }

    /**
     * 生成月报
     */
    public void generateMonthlyReport(Date startDate, Date endDate) {
        log.info("Generating monthly report from {} to {}", startDate, endDate);
        // TODO: 实现月报生成逻辑
    }

    /**
     * 转换Entity为Domain
     */
    private AccessLog convertToDomain(AccessLogEntity entity) {
        AccessLog domain = new AccessLog();
        BeanUtils.copyProperties(entity, domain);
        return domain;
    }
}
