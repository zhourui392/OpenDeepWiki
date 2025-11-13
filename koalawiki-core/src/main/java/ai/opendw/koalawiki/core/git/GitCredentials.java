package ai.opendw.koalawiki.core.git;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Git凭证配置
 * 用于访问私有仓库的认证信息
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitCredentials {

    /**
     * 认证类型
     */
    private CredentialType type;

    /**
     * 用户名（用于HTTP/HTTPS认证）
     */
    private String username;

    /**
     * 密码或访问令牌（用于HTTP/HTTPS认证）
     */
    private String password;

    /**
     * SSH私钥路径（用于SSH认证）
     */
    private String sshKeyPath;

    /**
     * SSH私钥密码（如果私钥有密码保护）
     */
    private String sshKeyPassphrase;

    /**
     * 代理配置（可选）
     */
    private ProxyConfig proxy;

    /**
     * 认证类型枚举
     */
    public enum CredentialType {
        /**
         * 无需认证（公开仓库）
         */
        NONE,

        /**
         * HTTP Basic认证
         */
        HTTP_BASIC,

        /**
         * OAuth令牌认证
         */
        OAUTH_TOKEN,

        /**
         * SSH密钥认证
         */
        SSH_KEY
    }

    /**
     * 代理配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProxyConfig {
        /**
         * 代理主机
         */
        private String host;

        /**
         * 代理端口
         */
        private int port;

        /**
         * 代理用户名（可选）
         */
        private String username;

        /**
         * 代理密码（可选）
         */
        private String password;

        /**
         * 代理类型（HTTP, SOCKS等）
         */
        private String type = "HTTP";
    }

    /**
     * 创建无需认证的凭证
     */
    public static GitCredentials none() {
        return GitCredentials.builder()
                .type(CredentialType.NONE)
                .build();
    }

    /**
     * 创建HTTP Basic认证凭证
     */
    public static GitCredentials httpBasic(String username, String password) {
        return GitCredentials.builder()
                .type(CredentialType.HTTP_BASIC)
                .username(username)
                .password(password)
                .build();
    }

    /**
     * 创建OAuth令牌认证凭证
     */
    public static GitCredentials oauthToken(String token) {
        return GitCredentials.builder()
                .type(CredentialType.OAUTH_TOKEN)
                .password(token)
                .build();
    }

    /**
     * 创建SSH密钥认证凭证
     */
    public static GitCredentials sshKey(String keyPath, String passphrase) {
        return GitCredentials.builder()
                .type(CredentialType.SSH_KEY)
                .sshKeyPath(keyPath)
                .sshKeyPassphrase(passphrase)
                .build();
    }
}