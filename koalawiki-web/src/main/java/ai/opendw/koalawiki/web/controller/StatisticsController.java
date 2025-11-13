package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.IStatisticsService;
import ai.opendw.koalawiki.domain.log.DailyStatistics;
import ai.opendw.koalawiki.web.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统计数据REST API控制器
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private IStatisticsService statisticsService;

    @GetMapping("/daily/{warehouseId}")
    public ResponseEntity<Result<DailyStatistics>> getDailyStatistics(
            @PathVariable String warehouseId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        try {
            DailyStatistics statistics = statisticsService.getDailyStatistics(warehouseId, date);
            if (statistics == null) {
                return ResponseEntity.ok(Result.error("未找到统计数据"));
            }
            return ResponseEntity.ok(Result.success(statistics));
        } catch (Exception e) {
            log.error("Failed to get daily statistics", e);
            return ResponseEntity.ok(Result.error("获取统计数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/trend/{warehouseId}")
    public ResponseEntity<Result<Map<String, Object>>> getTrend(
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "7") Integer days) {
        try {
            Map<String, Object> trend = statisticsService.getTrend(warehouseId, days);
            return ResponseEntity.ok(Result.success(trend));
        } catch (Exception e) {
            log.error("Failed to get trend", e);
            return ResponseEntity.ok(Result.error("获取趋势数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/summary/{warehouseId}")
    public ResponseEntity<Result<Map<String, Object>>> getSummary(
            @PathVariable String warehouseId) {
        try {
            Map<String, Object> summary = statisticsService.getSummary(warehouseId);
            return ResponseEntity.ok(Result.success(summary));
        } catch (Exception e) {
            log.error("Failed to get statistics summary", e);
            return ResponseEntity.ok(Result.error("获取统计摘要失败: " + e.getMessage()));
        }
    }
}
