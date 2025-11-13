package ai.opendw.koalawiki.app.task;

import ai.opendw.koalawiki.domain.log.AccessLog;
import ai.opendw.koalawiki.infra.entity.AccessLogEntity;
import ai.opendw.koalawiki.infra.repository.AccessLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 访问日志处理器
 * 处理从队列中获取的日志，进行数据清洗和保存
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Service
public class AccessLogProcessor {

    @Autowired
    private AccessLogRepository accessLogRepository;

    /**
     * 批量处理访问日志
     *
     * @param logs 日志列表
     */
    @Transactional
    public void processBatch(List<AccessLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }

        try {
            // 数据清洗
            List<AccessLog> cleanedLogs = cleanData(logs);

            // 转换为Entity
            List<AccessLogEntity> entities = convertToEntities(cleanedLogs);

            // 批量保存
            accessLogRepository.saveAll(entities);

            log.debug("Successfully saved {} access logs to database", entities.size());

        } catch (Exception e) {
            log.error("Failed to process access log batch", e);
            throw e;
        }
    }

    /**
     * 数据清洗
     * 过滤异常数据，规范化字段
     */
    private List<AccessLog> cleanData(List<AccessLog> logs) {
        return logs.stream()
                .filter(this::isValid)
                .map(this::normalize)
                .collect(Collectors.toList());
    }

    /**
     * 验证日志是否有效
     */
    private boolean isValid(AccessLog log) {
        if (log == null) {
            return false;
        }

        // 必须有访问时间
        if (log.getAccessTime() == null) {
            this.log.warn("Access log missing access time, skipped");
            return false;
        }

        // 必须有动作类型
        if (log.getAction() == null || log.getAction().trim().isEmpty()) {
            this.log.warn("Access log missing action, skipped");
            return false;
        }

        return true;
    }

    /**
     * 规范化日志数据
     */
    private AccessLog normalize(AccessLog log) {
        // 截断过长的字段
        if (log.getRequestUri() != null && log.getRequestUri().length() > 500) {
            log.setRequestUri(log.getRequestUri().substring(0, 500));
        }

        if (log.getUserAgent() != null && log.getUserAgent().length() > 500) {
            log.setUserAgent(log.getUserAgent().substring(0, 500));
        }

        if (log.getReferer() != null && log.getReferer().length() > 500) {
            log.setReferer(log.getReferer().substring(0, 500));
        }

        if (log.getErrorMessage() != null && log.getErrorMessage().length() > 1000) {
            log.setErrorMessage(log.getErrorMessage().substring(0, 1000));
        }

        // 规范化IP地址
        if (log.getIpAddress() != null) {
            log.setIpAddress(log.getIpAddress().trim());
        }

        // 规范化动作类型（转大写）
        if (log.getAction() != null) {
            log.setAction(log.getAction().toUpperCase());
        }

        return log;
    }

    /**
     * 转换Domain对象为Entity对象
     */
    private List<AccessLogEntity> convertToEntities(List<AccessLog> logs) {
        List<AccessLogEntity> entities = new ArrayList<>(logs.size());

        for (AccessLog log : logs) {
            AccessLogEntity entity = new AccessLogEntity();
            BeanUtils.copyProperties(log, entity);
            entities.add(entity);
        }

        return entities;
    }

    /**
     * 异常检测（可选）
     * 检测异常访问模式，如恶意爬虫、暴力破解等
     */
    public void detectAnomalies(List<AccessLog> logs) {
        // TODO: 实现异常检测逻辑
        // 例如：同一IP短时间内大量请求、频繁401/403等
    }
}
