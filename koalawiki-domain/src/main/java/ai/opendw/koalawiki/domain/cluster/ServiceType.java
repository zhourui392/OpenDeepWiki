package ai.opendw.koalawiki.domain.cluster;

/**
 * 服务类型枚举
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
public enum ServiceType {

    /**
     * 服务提供者
     */
    PROVIDER("服务提供者"),

    /**
     * 服务消费者
     */
    CONSUMER("服务消费者"),

    /**
     * 网关服务
     */
    GATEWAY("网关服务"),

    /**
     * 调度服务
     */
    SCHEDULER("调度服务"),

    /**
     * 中间件
     */
    MIDDLEWARE("中间件");

    private final String displayName;

    ServiceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
