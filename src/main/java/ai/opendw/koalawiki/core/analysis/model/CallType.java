package ai.opendw.koalawiki.core.analysis.model;

/**
 * 调用类型枚举
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
public enum CallType {
    /**
     * 本地方法调用
     */
    LOCAL,

    /**
     * Dubbo RPC调用
     */
    DUBBO,

    /**
     * Feign HTTP调用
     */
    FEIGN,

    /**
     * MQ消息发送
     */
    MQ
}
