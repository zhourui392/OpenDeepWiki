package ai.opendw.koalawiki.app.service.impl;

import ai.opendw.koalawiki.app.service.IStatisticsService;
import ai.opendw.koalawiki.app.task.StatisticsAggregator;
import ai.opendw.koalawiki.domain.log.ActionType;
import ai.opendw.koalawiki.domain.log.DailyStatistics;
import ai.opendw.koalawiki.infra.entity.DailyStatisticsEntity;
import ai.opendw.koalawiki.infra.repository.AccessLogRepository;
import ai.opendw.koalawiki.infra.repository.DailyStatisticsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Autowired
    private DailyStatisticsRepository dailyStatisticsRepository;

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private StatisticsAggregator statisticsAggregator;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Cacheable(value = "statisticsCache", key = "#warehouseId + '_' + #date")
    public DailyStatistics getDailyStatistics(String warehouseId, Date date) {
        Optional<DailyStatisticsEntity> entityOpt;

        if (warehouseId == null) {
            entityOpt = dailyStatisticsRepository.findByStatisticsDate(date);
        } else {
            entityOpt = dailyStatisticsRepository.findByWarehouseIdAndStatisticsDate(warehouseId, date);
        }

        return entityOpt.map(this::convertToDomain).orElse(null);
    }

    @Override
    public List<DailyStatistics> getRangeStatistics(String warehouseId, Date startDate, Date endDate) {
        List<DailyStatisticsEntity> entities;

        if (warehouseId == null) {
            entities = dailyStatisticsRepository.findByStatisticsDateBetween(startDate, endDate);
        } else {
            entities = dailyStatisticsRepository.findByWarehouseIdAndStatisticsDateBetween(
                    warehouseId, startDate, endDate);
        }

        return entities.stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "statisticsCache", key = "#warehouseId + '_trend_' + #days")
    public Map<String, Object> getTrend(String warehouseId, Integer days) {
        // 计算日期范围
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        Date startDate = calendar.getTime();

        // 获取统计数据
        List<DailyStatistics> statistics = getRangeStatistics(warehouseId, startDate, endDate);

        // 构建趋势数据
        Map<String, Object> trend = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> pvData = new ArrayList<>();
        List<Long> uvData = new ArrayList<>();

        statistics.stream()
                .sorted(Comparator.comparing(DailyStatistics::getStatisticsDate))
                .forEach(stat -> {
                    labels.add(formatDate(stat.getStatisticsDate()));
                    pvData.add(stat.getViewCount() != null ? stat.getViewCount() : 0L);
                    uvData.add(stat.getUniqueUserCount() != null ? stat.getUniqueUserCount() : 0L);
                });

        trend.put("labels", labels);
        trend.put("pv", pvData);
        trend.put("uv", uvData);

        return trend;
    }

    @Override
    @Cacheable(value = "statisticsCache", key = "#warehouseId + '_top_' + #limit")
    public List<Map<String, Object>> getTopDocuments(String warehouseId, Integer limit) {
        // 获取最近的统计数据
        DailyStatistics latest = getLatestStatistics(warehouseId);

        if (latest == null || latest.getTopDocuments() == null) {
            return Collections.emptyList();
        }

        try {
            // 解析JSON
            List<Map<String, Object>> topDocs = objectMapper.readValue(
                    latest.getTopDocuments(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            // 限制数量
            return topDocs.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to parse top documents JSON", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = "statisticsCache", key = "#warehouseId + '_summary'")
    public Map<String, Object> getSummary(String warehouseId) {
        Map<String, Object> summary = new HashMap<>();

        // 获取今天的统计
        DailyStatistics today = getTodayStatistics(warehouseId);
        if (today != null) {
            summary.put("todayPV", today.getViewCount());
            summary.put("todayUV", today.getUniqueUserCount());
            summary.put("todayRequests", today.getTotalRequests());
        }

        // 获取昨天的统计
        DailyStatistics yesterday = getYesterdayStatistics(warehouseId);
        if (yesterday != null) {
            summary.put("yesterdayPV", yesterday.getViewCount());
            summary.put("yesterdayUV", yesterday.getUniqueUserCount());
        }

        // 计算增长率
        if (today != null && yesterday != null) {
            double pvGrowth = statisticsAggregator.calculateGrowthRate(
                    today.getViewCount() != null ? today.getViewCount() : 0,
                    yesterday.getViewCount() != null ? yesterday.getViewCount() : 0
            );
            summary.put("pvGrowth", pvGrowth);

            double uvGrowth = statisticsAggregator.calculateGrowthRate(
                    today.getUniqueUserCount() != null ? today.getUniqueUserCount() : 0,
                    yesterday.getUniqueUserCount() != null ? yesterday.getUniqueUserCount() : 0
            );
            summary.put("uvGrowth", uvGrowth);
        }

        // 获取最近7天的统计
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        Date startDate = calendar.getTime();

        List<DailyStatistics> weekStats = getRangeStatistics(warehouseId, startDate, endDate);
        if (!weekStats.isEmpty()) {
            Map<String, Object> weekSummary = statisticsAggregator.aggregate(weekStats);
            summary.put("week", weekSummary);
        }

        return summary;
    }

    @Override
    public Page<DailyStatistics> listStatistics(String warehouseId, Pageable pageable) {
        Page<DailyStatisticsEntity> entityPage;

        if (warehouseId == null) {
            entityPage = dailyStatisticsRepository.findByWarehouseIdIsNull(pageable);
        } else {
            entityPage = dailyStatisticsRepository.findByWarehouseId(warehouseId, pageable);
        }

        List<DailyStatistics> content = entityPage.getContent().stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, entityPage.getTotalElements());
    }

    @Override
    public Map<String, Object> getRealtimeStatistics(String warehouseId) {
        Map<String, Object> realtime = new HashMap<>();

        // 获取今天的日期范围
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();
        Date endDate = new Date();

        // 实时计算统计数据
        Long totalCount;
        Long uniqueUsers;
        Long uniqueIps;
        Double avgResponseTime;

        if (warehouseId == null) {
            totalCount = accessLogRepository.countByAccessTimeBetween(startDate, endDate);
            uniqueUsers = accessLogRepository.countDistinctUserByAccessTimeBetween(startDate, endDate);
            uniqueIps = accessLogRepository.countDistinctIpByAccessTimeBetween(startDate, endDate);
            avgResponseTime = accessLogRepository.avgResponseTimeByAccessTimeBetween(startDate, endDate);
        } else {
            totalCount = accessLogRepository.countByWarehouseIdAndAccessTimeBetween(
                    warehouseId, startDate, endDate);
            // TODO: 添加warehouseId的其他统计方法
            uniqueUsers = 0L;
            uniqueIps = 0L;
            avgResponseTime = 0.0;
        }

        realtime.put("totalRequests", totalCount);
        realtime.put("uniqueUsers", uniqueUsers);
        realtime.put("uniqueIps", uniqueIps);
        realtime.put("avgResponseTime", avgResponseTime);
        realtime.put("timestamp", new Date());

        return realtime;
    }

    /**
     * 获取最新的统计数据
     */
    private DailyStatistics getLatestStatistics(String warehouseId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // 昨天
        Date yesterday = calendar.getTime();

        return getDailyStatistics(warehouseId, yesterday);
    }

    /**
     * 获取今天的统计
     */
    private DailyStatistics getTodayStatistics(String warehouseId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date today = calendar.getTime();

        return getDailyStatistics(warehouseId, today);
    }

    /**
     * 获取昨天的统计
     */
    private DailyStatistics getYesterdayStatistics(String warehouseId) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date yesterday = calendar.getTime();

        return getDailyStatistics(warehouseId, yesterday);
    }

    /**
     * 转换Entity为Domain
     */
    private DailyStatistics convertToDomain(DailyStatisticsEntity entity) {
        DailyStatistics domain = new DailyStatistics();
        BeanUtils.copyProperties(entity, domain);
        return domain;
    }

    /**
     * 格式化日期
     */
    private String formatDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return String.format("%02d-%02d",
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }
}
