package ai.opendw.koalawiki.core.analysis.model;

/**
 * 入口类型枚举
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
public enum EntryType {
    /**
     * HTTP接口
     */
    HTTP,

    /**
     * Dubbo服务
     */
    DUBBO,

    /**
     * 定时任务
     */
    SCHEDULED,

    /**
     * MQ消费者
     */
    MQ,

    /**
     * 其他
     */
    OTHER
}
