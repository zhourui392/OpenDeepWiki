package ai.opendw.koalawiki.domain.cluster;

/**
 * 依赖图类型枚举
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
public enum GraphType {

    /**
     * 完整集群图
     */
    FULL("完整集群图"),

    /**
     * 领域级别图
     */
    DOMAIN("领域级别图"),

    /**
     * 服务级别图
     */
    SERVICE("服务级别图");

    private final String displayName;

    GraphType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
