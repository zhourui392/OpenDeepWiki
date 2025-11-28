package ai.opendw.koalawiki.core.ai.context;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询意图
 * 用于分析用户查询的意图，智能选择上下文级别
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class QueryIntent {

    /**
     * 意图类型
     */
    private IntentType intentType;

    /**
     * 识别的服务名列表
     */
    private List<String> identifiedServices = new ArrayList<>();

    /**
     * 识别的接口名列表
     */
    private List<String> identifiedInterfaces = new ArrayList<>();

    /**
     * 识别的领域列表
     */
    private List<String> identifiedDomains = new ArrayList<>();

    /**
     * 置信度（0-1）
     */
    private double confidence;

    /**
     * 是否需要接口详情
     *
     * @return true表示需要
     */
    public boolean needsInterfaceDetail() {
        return intentType == IntentType.INTERFACE_QUERY
                || intentType == IntentType.INTEGRATION_DESIGN
                || !identifiedInterfaces.isEmpty();
    }

    /**
     * 是否需要服务详情
     *
     * @return true表示需要
     */
    public boolean needsServiceDetail() {
        return intentType == IntentType.SERVICE_QUERY
                || intentType == IntentType.IMPACT_ANALYSIS
                || intentType == IntentType.INTEGRATION_DESIGN
                || !identifiedServices.isEmpty();
    }

    /**
     * 是否需要依赖关系
     *
     * @return true表示需要
     */
    public boolean needsDependencies() {
        return intentType == IntentType.IMPACT_ANALYSIS
                || intentType == IntentType.CALL_CHAIN_QUERY
                || intentType == IntentType.INTEGRATION_DESIGN;
    }

    /**
     * 推荐的上下文级别
     *
     * @return 上下文级别
     */
    public ai.opendw.koalawiki.domain.cluster.ContextLevel recommendedContextLevel() {
        switch (intentType) {
            case OVERVIEW_QUERY:
                return ai.opendw.koalawiki.domain.cluster.ContextLevel.L1;
            case DOMAIN_QUERY:
                return ai.opendw.koalawiki.domain.cluster.ContextLevel.L2;
            case SERVICE_QUERY:
            case INTERFACE_QUERY:
                return ai.opendw.koalawiki.domain.cluster.ContextLevel.L3;
            case INTEGRATION_DESIGN:
            case IMPACT_ANALYSIS:
            case CALL_CHAIN_QUERY:
                return ai.opendw.koalawiki.domain.cluster.ContextLevel.L4;
            default:
                return ai.opendw.koalawiki.domain.cluster.ContextLevel.L2;
        }
    }

    /**
     * 意图类型枚举
     */
    public enum IntentType {

        /**
         * 集群概览查询
         */
        OVERVIEW_QUERY("概览查询"),

        /**
         * 领域查询
         */
        DOMAIN_QUERY("领域查询"),

        /**
         * 服务查询
         */
        SERVICE_QUERY("服务查询"),

        /**
         * 接口查询
         */
        INTERFACE_QUERY("接口查询"),

        /**
         * 调用链查询
         */
        CALL_CHAIN_QUERY("调用链查询"),

        /**
         * 集成方案设计
         */
        INTEGRATION_DESIGN("集成设计"),

        /**
         * 影响分析
         */
        IMPACT_ANALYSIS("影响分析"),

        /**
         * 通用查询
         */
        GENERAL("通用查询");

        private final String displayName;

        IntentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
