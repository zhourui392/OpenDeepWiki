package ai.opendw.koalawiki.core.git;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Git配置属性
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "koalawiki.git")
public class GitProperties {

    /**
     * Git仓库本地存储路径
     */
    private String storagePath = "/data/koalawiki/git";

    /**
     * 克隆深度，0表示完整克隆
     */
    private int maxDepth = 0;

    /**
     * 操作超时时间（毫秒）
     */
    private int timeout = 300000;

    /**
     * 清理N天未使用的仓库
     */
    private int cacheCleanupDays = 30;

    /**
     * 是否启用自动清理
     */
    private boolean autoCleanup = true;

    /**
     * 代理配置
     */
    private ProxyConfig proxy;

    /**
     * 默认分支名称
     */
    private String defaultBranch = "main";

    /**
     * 最大仓库大小限制（MB）
     */
    private long maxRepositorySize = 500;

    /**
     * 是否启用SSH支持
     */
    private boolean sshEnabled = false;

    /**
     * SSH密钥目录
     */
    private String sshKeyDirectory = "~/.ssh";

    /**
     * 代理配置
     */
    @Data
    public static class ProxyConfig {
        /**
         * 是否启用代理
         */
        private boolean enabled = false;

        /**
         * 代理主机
         */
        private String host;

        /**
         * 代理端口
         */
        private int port;

        /**
         * 代理用户名
         */
        private String username;

        /**
         * 代理密码
         */
        private String password;

        /**
         * 代理类型（HTTP, SOCKS）
         */
        private String type = "HTTP";

        /**
         * 不使用代理的主机列表
         */
        private String[] noProxyHosts;
    }
}