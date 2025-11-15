package ai.opendw.koalawiki.app.service;

import ai.opendw.koalawiki.app.service.impl.StatisticsServiceImpl;
import ai.opendw.koalawiki.app.task.StatisticsAggregator;
import ai.opendw.koalawiki.domain.log.DailyStatistics;
import ai.opendw.koalawiki.infra.entity.DailyStatisticsEntity;
import ai.opendw.koalawiki.infra.repository.AccessLogRepository;
import ai.opendw.koalawiki.infra.repository.DailyStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 统计服务集成测试（对齐当前 StatisticsServiceImpl 实现）
 */
@ExtendWith(MockitoExtension.class)
public class StatisticsServiceIntegrationTest {

    @Mock
    private DailyStatisticsRepository dailyStatisticsRepository;

    @Mock
    private AccessLogRepository accessLogRepository;

    @Mock
    private StatisticsAggregator statisticsAggregator;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private DailyStatisticsEntity mockEntity;

    @BeforeEach
    public void setUp() {
        mockEntity = new DailyStatisticsEntity();
        mockEntity.setId("stat-1");
        mockEntity.setWarehouseId("warehouse-1");
        mockEntity.setStatisticsDate(new Date());
        mockEntity.setViewCount(100L);
        mockEntity.setUniqueUserCount(20L);
        mockEntity.setUniqueIpCount(15L);
        mockEntity.setTotalRequests(120L);
        mockEntity.setSuccessRequests(118L);
        mockEntity.setFailedRequests(2L);
        mockEntity.setErrorRate(1.67);
    }

    @Test
    public void testGetDailyStatistics_Success() {
        String warehouseId = "warehouse-1";
        Date date = new Date();

        when(dailyStatisticsRepository.findByWarehouseIdAndStatisticsDate(warehouseId, date))
                .thenReturn(Optional.of(mockEntity));

        DailyStatistics stats = statisticsService.getDailyStatistics(warehouseId, date);

        assertNotNull(stats);
        assertEquals(warehouseId, stats.getWarehouseId());
        verify(dailyStatisticsRepository, times(1))
                .findByWarehouseIdAndStatisticsDate(warehouseId, date);
    }

    @Test
    public void testListStatistics_Success() {
        String warehouseId = "warehouse-1";
        Pageable pageable = PageRequest.of(0, 10);
        Page<DailyStatisticsEntity> page =
                new PageImpl<>(Collections.singletonList(mockEntity), pageable, 1);

        when(dailyStatisticsRepository.findByWarehouseId(warehouseId, pageable))
                .thenReturn(page);

        Page<DailyStatistics> result =
                statisticsService.listStatistics(warehouseId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(dailyStatisticsRepository, times(1))
                .findByWarehouseId(warehouseId, pageable);
    }

    @Test
    public void testGetSummary_Success() {
        String warehouseId = "warehouse-1";

        // today / yesterday
        when(dailyStatisticsRepository.findByWarehouseIdAndStatisticsDate(anyString(), any(Date.class)))
                .thenReturn(Optional.of(mockEntity));

        // week range
        when(dailyStatisticsRepository.findByWarehouseIdAndStatisticsDateBetween(anyString(), any(Date.class), any(Date.class)))
                .thenReturn(Collections.singletonList(mockEntity));

        when(statisticsAggregator.aggregate(anyList()))
                .thenReturn(Collections.singletonMap("totalRequests", 120L));

        when(statisticsAggregator.calculateGrowthRate(anyDouble(), anyDouble()))
                .thenReturn(10.0);

        Map<String, Object> summary = statisticsService.getSummary(warehouseId);

        assertNotNull(summary);
        assertTrue(summary.containsKey("todayPV"));
        assertTrue(summary.containsKey("yesterdayPV"));
        assertTrue(summary.containsKey("pvGrowth"));
        assertTrue(summary.containsKey("week"));
    }

    @Test
    public void testGetRealtimeStatistics_Success() {
        String warehouseId = "warehouse-1";

        when(accessLogRepository.countByWarehouseIdAndAccessTimeBetween(anyString(), any(Date.class), any(Date.class)))
                .thenReturn(100L);

        Map<String, Object> realtime =
                statisticsService.getRealtimeStatistics(warehouseId);

        assertNotNull(realtime);
        assertTrue(realtime.containsKey("totalRequests"));
        assertTrue(realtime.containsKey("timestamp"));
    }
}
