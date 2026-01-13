package ai.opendw.koalawiki.infra.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI服务配置属性
 * Phase 3: OpenAI配置，预留扩展性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "koalawiki.ai")
public class AIProperties {

    /**
     * AI提供商类型
     */
    private String provider = "claude";

    /**
     * Claude CLI路径
     */
    private String claudeCliPath = "claude";

    /**
     * 聊天模型
     */
    private String chatModel = "claude-code-cli";

    /**
     * 分析模型
     */
    private String analysisModel = "claude-code-cli";

    /**
     * 最大token数
     */
    private Integer maxTokens = 4096;

    /**
     * 温度参数 (0.0-2.0)
     */
    private Double temperature = 0.7;

    /**
     * Top P参数
     */
    private Double topP = 1.0;

    /**
     * 频率惩罚
     */
    private Double frequencyPenalty = 0.0;

    /**
     * 存在惩罚
     */
    private Double presencePenalty = 0.0;

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 30000;

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 成本控制配置
     */
    private CostControlConfig costControl = new CostControlConfig();

    @Data
    public static class CacheConfig {
        private Boolean enabled = true;
        private Integer ttl = 3600;
        private Integer maxSize = 1000;
    }

    @Data
    public static class RateLimitConfig {
        private Boolean enabled = true;
        private Integer requestsPerMinute = 20;
        private Integer tokensPerMinute = 40000;
    }

    @Data
    public static class RetryConfig {
        private Integer maxAttempts = 3;
        private Long initialInterval = 1000L;
        private Long maxInterval = 10000L;
        private Double multiplier = 2.0;
    }

    @Data
    public static class CostControlConfig {
        private Integer dailyLimit = 100;
        private Integer monthlyLimit = 3000;
        private Double alertThreshold = 0.8;
    }
}
