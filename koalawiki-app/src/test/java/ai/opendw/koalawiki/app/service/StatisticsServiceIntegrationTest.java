package ai.opendw.koalawiki.app.service;

import ai.opendw.koalawiki.domain.log.AccessLog;
import ai.opendw.koalawiki.domain.log.ActionType;
import ai.opendw.koalawiki.domain.log.DailyStatistics;
import ai.opendw.koalawiki.infra.entity.AccessLogEntity;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 统计服务集成测试
 *
 * @author OpenDeepWiki Team
 * @date 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
public class StatisticsServiceIntegrationTest {

    @Mock
    private DailyStatisticsRepository statisticsRepository;

    @Mock
    private AccessLogRepository accessLogRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private DailyStatisticsEntity mockStatisticsEntity;
    private AccessLogEntity mockAccessLogEntity;

    @BeforeEach
    public void setUp() {
        // 准备统计数据
        mockStatisticsEntity = new DailyStatisticsEntity();
        mockStatisticsEntity.setId("stat-1");
        mockStatisticsEntity.setStatisticsDate(LocalDate.now());
        mockStatisticsEntity.setWarehouseId("warehouse-1");
        mockStatisticsEntity.setViewCount(1000L);
        mockStatisticsEntity.setUniqueUserCount(250L);
        mockStatisticsEntity.setUniqueIpCount(200L);
        mockStatisticsEntity.setDocumentViewCount(800L);
        mockStatisticsEntity.setSearchCount(150L);
        mockStatisticsEntity.setDownloadCount(50L);
        mockStatisticsEntity.setAvgResponseTime(125.5);
        mockStatisticsEntity.setMaxResponseTime(5000);
        mockStatisticsEntity.setMinResponseTime(10);
        mockStatisticsEntity.setTotalRequests(1200L);
        mockStatisticsEntity.setSuccessRequests(1150L);
        mockStatisticsEntity.setFailedRequests(50L);
        mockStatisticsEntity.setErrorRate(4.17);
        mockStatisticsEntity.setActionCounts("{\"VIEW\":800,\"SEARCH\":150,\"DOWNLOAD\":50}");
        mockStatisticsEntity.setTopDocuments("[{\"docId\":\"doc1\",\"views\":500}]");
        mockStatisticsEntity.setCreatedAt(LocalDateTime.now());

        // 准备访问日志数据
        mockAccessLogEntity = new AccessLogEntity();
        mockAccessLogEntity.setId("log-1");
        mockAccessLogEntity.setUserId("user-1");
        mockAccessLogEntity.setWarehouseId("warehouse-1");
        mockAccessLogEntity.setDocumentId("doc-1");
        mockAccessLogEntity.setAction("VIEW");
        mockAccessLogEntity.setIpAddress("192.168.1.100");
        mockAccessLogEntity.setUserAgent("Mozilla/5.0");
        mockAccessLogEntity.setRequestUri("/api/documents/doc-1");
        mockAccessLogEntity.setRequestMethod("GET");
        mockAccessLogEntity.setResponseTime(150);
        mockAccessLogEntity.setStatusCode(200);
        mockAccessLogEntity.setAccessTime(LocalDateTime.now());
        mockAccessLogEntity.setCreatedAt(LocalDateTime.now());
    }

    /**
     * 测试：获取每日统计 - 成功场景
     */
    @Test
    public void testGetDailyStatistics_Success() {
        // Given
        String warehouseId = "warehouse-1";
        LocalDate date = LocalDate.now();

        when(statisticsRepository.findByWarehouseIdAndStatisticsDate(warehouseId, date))
            .thenReturn(Optional.of(mockStatisticsEntity));

        // When
        DailyStatistics result = statisticsService.getDailyStatistics(warehouseId, date);

        // Then
        assertNotNull(result);
        assertEquals(warehouseId, result.getWarehouseId());
        assertEquals(1000L, result.getViewCount());
        assertEquals(250L, result.getUniqueUserCount());
        verify(statisticsRepository, times(1))
            .findByWarehouseIdAndStatisticsDate(warehouseId, date);
    }

    /**
     * 测试：获取每日统计 - 数据不存在
     */
    @Test
    public void testGetDailyStatistics_NotFound() {
        // Given
        String warehouseId = "warehouse-1";
        LocalDate date = LocalDate.now();

        when(statisticsRepository.findByWarehouseIdAndStatisticsDate(warehouseId, date))
            .thenReturn(Optional.empty());

        // When
        DailyStatistics result = statisticsService.getDailyStatistics(warehouseId, date);

        // Then
        assertNull(result);
    }

    /**
     * 测试：获取时间范围统计 - 成功场景
     */
    @Test
    public void testGetRangeStatistics_Success() {
        // Given
        String warehouseId = "warehouse-1";
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        List<DailyStatisticsEntity> entities = Arrays.asList(
            mockStatisticsEntity,
            createStatisticsEntity("stat-2", LocalDate.now().minusDays(1))
        );

        when(statisticsRepository.findByWarehouseIdAndStatisticsDateBetween(
            warehouseId, startDate, endDate))
            .thenReturn(entities);

        // When
        List<DailyStatistics> result = statisticsService.getRangeStatistics(
            warehouseId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(statisticsRepository, times(1))
            .findByWarehouseIdAndStatisticsDateBetween(warehouseId, startDate, endDate);
    }

    /**
     * 测试：获取趋势数据 - 成功场景
     */
    @Test
    public void testGetTrend_Success() {
        // Given
        String warehouseId = "warehouse-1";
        int days = 7;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<DailyStatisticsEntity> entities = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            entities.add(createStatisticsEntity(
                "stat-" + i,
                endDate.minusDays(days - 1 - i)
            ));
        }

        when(statisticsRepository.findByWarehouseIdAndStatisticsDateBetween(
            warehouseId, startDate, endDate))
            .thenReturn(entities);

        // When
        Map<String, Object> result = statisticsService.getTrend(warehouseId, days);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("labels"));
        assertTrue(result.containsKey("pv"));
        assertTrue(result.containsKey("uv"));

        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) result.get("labels");
        assertEquals(days, labels.size());
    }

    /**
     * 测试：获取热门文档 - 成功场景
     */
    @Test
    public void testGetTopDocuments_Success() {
        // Given
        String warehouseId = "warehouse-1";
        int limit = 10;
        LocalDate today = LocalDate.now();

        when(statisticsRepository.findByWarehouseIdAndStatisticsDate(warehouseId, today))
            .thenReturn(Optional.of(mockStatisticsEntity));

        // When
        List<Map<String, Object>> result = statisticsService.getTopDocuments(warehouseId, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    /**
     * 测试：获取统计摘要 - 成功场景
     */
    @Test
    public void testGetSummary_Success() {
        // Given
        String warehouseId = "warehouse-1";
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekStart = today.minusDays(6);

        DailyStatisticsEntity todayStats = mockStatisticsEntity;
        DailyStatisticsEntity yesterdayStats = createStatisticsEntity("stat-yesterday", yesterday);
        yesterdayStats.setViewCount(900L);
        yesterdayStats.setUniqueUserCount(220L);

        List<DailyStatisticsEntity> weekStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekStats.add(createStatisticsEntity("stat-week-" + i, today.minusDays(i)));
        }

        when(statisticsRepository.findByWarehouseIdAndStatisticsDate(warehouseId, today))
            .thenReturn(Optional.of(todayStats));
        when(statisticsRepository.findByWarehouseIdAndStatisticsDate(warehouseId, yesterday))
            .thenReturn(Optional.of(yesterdayStats));
        when(statisticsRepository.findByWarehouseIdAndStatisticsDateBetween(
            warehouseId, weekStart, today))
            .thenReturn(weekStats);

        // When
        Map<String, Object> result = statisticsService.getSummary(warehouseId);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("todayPV"));
        assertTrue(result.containsKey("todayUV"));
        assertTrue(result.containsKey("yesterdayPV"));
        assertTrue(result.containsKey("yesterdayUV"));
        assertTrue(result.containsKey("pvGrowth"));
        assertTrue(result.containsKey("uvGrowth"));
        assertTrue(result.containsKey("week"));
    }

    /**
     * 测试：分页查询统计 - 成功场景
     */
    @Test
    public void testListStatistics_Success() {
        // Given
        String warehouseId = "warehouse-1";
        Pageable pageable = PageRequest.of(0, 10);

        List<DailyStatisticsEntity> entities = Arrays.asList(mockStatisticsEntity);
        Page<DailyStatisticsEntity> page = new PageImpl<>(entities, pageable, 1);

        when(statisticsRepository.findByWarehouseId(warehouseId, pageable))
            .thenReturn(page);

        // When
        Page<DailyStatistics> result = statisticsService.listStatistics(warehouseId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    /**
     * 测试：实时统计 - 成功场景
     */
    @Test
    public void testGetRealtimeStatistics_Success() {
        // Given
        String warehouseId = "warehouse-1";
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        List<AccessLogEntity> todayLogs = Arrays.asList(
            mockAccessLogEntity,
            createAccessLogEntity("log-2", "user-2", "VIEW"),
            createAccessLogEntity("log-3", "user-1", "SEARCH")
        );

        when(accessLogRepository.findByWarehouseIdAndAccessTimeBetween(
            warehouseId, today, now))
            .thenReturn(todayLogs);

        // When
        Map<String, Object> result = statisticsService.getRealtimeStatistics(warehouseId);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("totalRequests"));
        assertTrue(result.containsKey("uniqueUsers"));
        assertTrue(result.containsKey("uniqueIps"));
    }

    /**
     * 测试：全局统计 - 成功场景
     */
    @Test
    public void testGetGlobalStatistics_Success() {
        // Given
        LocalDate date = LocalDate.now();

        when(statisticsRepository.findByWarehouseIdAndStatisticsDate(null, date))
            .thenReturn(Optional.of(mockStatisticsEntity));

        // When
        DailyStatistics result = statisticsService.getDailyStatistics(null, date);

        // Then
        assertNotNull(result);
        verify(statisticsRepository, times(1))
            .findByWarehouseIdAndStatisticsDate(null, date);
    }

    /**
     * 测试：计算增长率
     */
    @Test
    public void testCalculateGrowthRate() {
        // Given
        long current = 1000L;
        long previous = 800L;

        // When
        double growth = statisticsService.calculateGrowthRate(current, previous);

        // Then
        assertEquals(25.0, growth, 0.01);
    }

    /**
     * 测试：计算增长率 - 前值为0
     */
    @Test
    public void testCalculateGrowthRate_ZeroPrevious() {
        // Given
        long current = 1000L;
        long previous = 0L;

        // When
        double growth = statisticsService.calculateGrowthRate(current, previous);

        // Then
        assertEquals(0.0, growth, 0.01);
    }

    /**
     * 测试：计算错误率
     */
    @Test
    public void testCalculateErrorRate() {
        // Given
        long totalRequests = 1000L;
        long failedRequests = 50L;

        // When
        double errorRate = statisticsService.calculateErrorRate(totalRequests, failedRequests);

        // Then
        assertEquals(5.0, errorRate, 0.01);
    }

    // 辅助方法
    private DailyStatisticsEntity createStatisticsEntity(String id, LocalDate date) {
        DailyStatisticsEntity entity = new DailyStatisticsEntity();
        entity.setId(id);
        entity.setStatisticsDate(date);
        entity.setWarehouseId("warehouse-1");
        entity.setViewCount(1000L);
        entity.setUniqueUserCount(250L);
        entity.setUniqueIpCount(200L);
        entity.setTotalRequests(1200L);
        entity.setSuccessRequests(1150L);
        entity.setFailedRequests(50L);
        entity.setErrorRate(4.17);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private AccessLogEntity createAccessLogEntity(String id, String userId, String action) {
        AccessLogEntity entity = new AccessLogEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setWarehouseId("warehouse-1");
        entity.setAction(action);
        entity.setIpAddress("192.168.1." + id.hashCode() % 255);
        entity.setStatusCode(200);
        entity.setResponseTime(100);
        entity.setAccessTime(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
