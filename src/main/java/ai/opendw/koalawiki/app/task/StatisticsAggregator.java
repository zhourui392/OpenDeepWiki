package ai.opendw.koalawiki.app.task;

import ai.opendw.koalawiki.domain.log.DailyStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计数据聚合器
 * 聚合多个时间段的统计数据
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Component
public class StatisticsAggregator {

    /**
     * 聚合统计数据
     *
     * @param statistics 统计数据列表
     * @return 聚合结果
     */
    public Map<String, Object> aggregate(List<DailyStatistics> statistics) {
        if (statistics == null || statistics.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new HashMap<>();

        // 总PV
        long totalPV = statistics.stream()
                .filter(s -> s.getViewCount() != null)
                .mapToLong(DailyStatistics::getViewCount)
                .sum();
        result.put("totalPV", totalPV);

        // 平均PV
        double avgPV = totalPV / (double) statistics.size();
        result.put("avgPV", avgPV);

        // 总UV
        long totalUV = statistics.stream()
                .filter(s -> s.getUniqueUserCount() != null)
                .mapToLong(DailyStatistics::getUniqueUserCount)
                .sum();
        result.put("totalUV", totalUV);

        // 平均UV
        double avgUV = totalUV / (double) statistics.size();
        result.put("avgUV", avgUV);

        // 总请求数
        long totalRequests = statistics.stream()
                .filter(s -> s.getTotalRequests() != null)
                .mapToLong(DailyStatistics::getTotalRequests)
                .sum();
        result.put("totalRequests", totalRequests);

        // 总成功请求数
        long totalSuccess = statistics.stream()
                .filter(s -> s.getSuccessRequests() != null)
                .mapToLong(DailyStatistics::getSuccessRequests)
                .sum();
        result.put("totalSuccess", totalSuccess);

        // 总失败请求数
        long totalFailed = statistics.stream()
                .filter(s -> s.getFailedRequests() != null)
                .mapToLong(DailyStatistics::getFailedRequests)
                .sum();
        result.put("totalFailed", totalFailed);

        // 平均错误率
        double avgErrorRate = statistics.stream()
                .filter(s -> s.getErrorRate() != null)
                .mapToDouble(DailyStatistics::getErrorRate)
                .average()
                .orElse(0.0);
        result.put("avgErrorRate", avgErrorRate);

        // 平均响应时间
        double avgResponseTime = statistics.stream()
                .filter(s -> s.getAvgResponseTime() != null)
                .mapToDouble(DailyStatistics::getAvgResponseTime)
                .average()
                .orElse(0.0);
        result.put("avgResponseTime", avgResponseTime);

        // 趋势数据
        List<Map<String, Object>> trend = statistics.stream()
                .sorted(Comparator.comparing(DailyStatistics::getStatisticsDate))
                .map(s -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", s.getStatisticsDate());
                    item.put("pv", s.getViewCount());
                    item.put("uv", s.getUniqueUserCount());
                    return item;
                })
                .collect(Collectors.toList());
        result.put("trend", trend);

        return result;
    }

    /**
     * 计算同比增长率
     *
     * @param current 当前值
     * @param previous 上期值
     * @return 增长率（%）
     */
    public double calculateGrowthRate(double current, double previous) {
        if (previous == 0) {
            return 0.0;
        }
        return ((current - previous) / previous) * 100;
    }

    /**
     * 计算环比增长率
     *
     * @param statistics 统计数据列表（按时间排序）
     * @return 环比增长率列表
     */
    public List<Double> calculateChainGrowthRates(List<DailyStatistics> statistics) {
        List<Double> growthRates = new ArrayList<>();

        for (int i = 1; i < statistics.size(); i++) {
            DailyStatistics current = statistics.get(i);
            DailyStatistics previous = statistics.get(i - 1);

            if (current.getViewCount() != null && previous.getViewCount() != null) {
                double rate = calculateGrowthRate(
                        current.getViewCount(),
                        previous.getViewCount()
                );
                growthRates.add(rate);
            }
        }

        return growthRates;
    }
}
