package ai.opendw.koalawiki.domain.cluster;

/**
 * 集群状态枚举
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
public enum ClusterStatus {

    /**
     * 活跃状态
     */
    ACTIVE("活跃"),

    /**
     * 非活跃状态
     */
    INACTIVE("非活跃"),

    /**
     * 已归档
     */
    ARCHIVED("已归档");

    private final String displayName;

    ClusterStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
