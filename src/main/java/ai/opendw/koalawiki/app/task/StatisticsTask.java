package ai.opendw.koalawiki.app.task;

import ai.opendw.koalawiki.domain.log.DailyStatistics;
import ai.opendw.koalawiki.infra.entity.DailyStatisticsEntity;
import ai.opendw.koalawiki.infra.repository.DailyStatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

/**
 * 统计数据生成定时任务
 * 定期生成每日统计数据
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Component
public class StatisticsTask {

    @Autowired
    private StatisticsCalculator statisticsCalculator;

    @Autowired
    private DailyStatisticsRepository dailyStatisticsRepository;

    /**
     * 生成每日统计数据
     * 每天凌晨1点执行，统计前一天的数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void generateDailyStatistics() {
        try {
            log.info("Starting to generate daily statistics");

            // 计算前一天的日期
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date targetDate = calendar.getTime();

            // 生成全局统计
            DailyStatistics globalStats = statisticsCalculator.calculate(targetDate, null);
            if (globalStats != null) {
                saveDailyStatistics(globalStats);
                log.info("Generated global daily statistics for {}", targetDate);
            }

            // TODO: 生成各仓库的统计
            // List<String> warehouseIds = warehouseRepository.findAllIds();
            // for (String warehouseId : warehouseIds) {
            //     DailyStatistics stats = statisticsCalculator.calculate(targetDate, warehouseId);
            //     saveDailyStatistics(stats);
            // }

            log.info("Successfully generated daily statistics for {}", targetDate);

        } catch (Exception e) {
            log.error("Failed to generate daily statistics", e);
        }
    }

    /**
     * 生成每周报告
     * 每周一凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 ? * MON")
    public void generateWeeklyReport() {
        try {
            log.info("Starting to generate weekly report");

            // 计算上周的日期范围
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.WEEK_OF_YEAR, -1);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Date startDate = calendar.getTime();

            calendar.add(Calendar.DAY_OF_MONTH, 6);
            Date endDate = calendar.getTime();

            // 生成周报
            statisticsCalculator.generateWeeklyReport(startDate, endDate);

            log.info("Successfully generated weekly report for {} to {}", startDate, endDate);

        } catch (Exception e) {
            log.error("Failed to generate weekly report", e);
        }
    }

    /**
     * 生成每月报告
     * 每月1号凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyReport() {
        try {
            log.info("Starting to generate monthly report");

            // 计算上个月的日期范围
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date startDate = calendar.getTime();

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Date endDate = calendar.getTime();

            // 生成月报
            statisticsCalculator.generateMonthlyReport(startDate, endDate);

            log.info("Successfully generated monthly report for {} to {}", startDate, endDate);

        } catch (Exception e) {
            log.error("Failed to generate monthly report", e);
        }
    }

    /**
     * 保存每日统计数据
     */
    private void saveDailyStatistics(DailyStatistics statistics) {
        if (statistics == null) {
            return;
        }

        // 检查是否已存在
        boolean exists = dailyStatisticsRepository.existsByWarehouseIdAndStatisticsDate(
                statistics.getWarehouseId(), statistics.getStatisticsDate());

        if (exists) {
            log.warn("Daily statistics already exists for warehouse: {}, date: {}",
                    statistics.getWarehouseId(), statistics.getStatisticsDate());
            return;
        }

        // 转换并保存
        DailyStatisticsEntity entity = new DailyStatisticsEntity();
        BeanUtils.copyProperties(statistics, entity);
        dailyStatisticsRepository.save(entity);
    }
}
