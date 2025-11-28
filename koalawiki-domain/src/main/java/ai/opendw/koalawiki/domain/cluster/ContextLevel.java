package ai.opendw.koalawiki.domain.cluster;

/**
 * AI上下文级别枚举
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
public enum ContextLevel {

    /**
     * L1: 全局索引
     */
    L1("全局索引", 2),

    /**
     * L2: 领域摘要
     */
    L2("领域摘要", 5),

    /**
     * L3: 服务详情
     */
    L3("服务详情", 10),

    /**
     * L4: 完整扩展
     */
    L4("完整扩展", 30);

    private final String displayName;

    /**
     * 预估上下文大小（KB）
     */
    private final int estimatedSizeKb;

    ContextLevel(String displayName, int estimatedSizeKb) {
        this.displayName = displayName;
        this.estimatedSizeKb = estimatedSizeKb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getEstimatedSizeKb() {
        return estimatedSizeKb;
    }
}
