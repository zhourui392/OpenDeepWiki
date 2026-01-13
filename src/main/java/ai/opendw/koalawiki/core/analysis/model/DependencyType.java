package ai.opendw.koalawiki.core.analysis.model;

/**
 * 依赖类型枚举
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
public enum DependencyType {
    /**
     * Dubbo RPC调用
     */
    DUBBO,

    /**
     * Feign HTTP调用
     */
    FEIGN,

    /**
     * RestTemplate HTTP调用
     */
    HTTP,

    /**
     * 消息队列
     */
    MQ
}
