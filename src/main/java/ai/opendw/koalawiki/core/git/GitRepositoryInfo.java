package ai.opendw.koalawiki.core.git;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Git仓库信息
 * 封装Git仓库的基本信息和状态
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitRepositoryInfo {

    /**
     * 仓库URL（远程仓库地址）
     */
    private String remoteUrl;

    /**
     * 本地仓库路径
     */
    private String localPath;

    /**
     * 当前分支
     */
    private String currentBranch;

    /**
     * 最新提交ID (SHA-1)
     */
    private String latestCommitId;

    /**
     * 最新提交消息
     */
    private String latestCommitMessage;

    /**
     * 最新提交时间
     */
    private Date latestCommitTime;

    /**
     * 最新提交作者
     */
    private String latestCommitAuthor;

    /**
     * 仓库大小（字节）
     */
    private Long repositorySize;

    /**
     * 文件数量
     */
    private Integer fileCount;

    /**
     * 是否有未提交的更改
     */
    private Boolean hasUncommittedChanges;

    /**
     * 是否有未推送的提交
     */
    private Boolean hasUnpushedCommits;

    /**
     * 仓库状态
     */
    private RepositoryStatus status;

    /**
     * 最后同步时间
     */
    private Date lastSyncTime;

    /**
     * 仓库平台类型（GitHub, GitLab, Gitee等）
     */
    private GitPlatform platform;

    /**
     * 仓库所有者
     */
    private String owner;

    /**
     * 仓库名称
     */
    private String repositoryName;

    /**
     * 仓库状态枚举
     */
    public enum RepositoryStatus {
        /**
         * 未初始化
         */
        NOT_INITIALIZED,

        /**
         * 正在克隆
         */
        CLONING,

        /**
         * 正在拉取
         */
        PULLING,

        /**
         * 就绪
         */
        READY,

        /**
         * 错误
         */
        ERROR,

        /**
         * 已删除
         */
        DELETED
    }

    /**
     * Git平台枚举
     */
    public enum GitPlatform {
        GITHUB("github.com"),
        GITLAB("gitlab.com"),
        GITEE("gitee.com"),
        BITBUCKET("bitbucket.org"),
        AZURE_DEVOPS("dev.azure.com"),
        CUSTOM("custom");

        private final String domain;

        GitPlatform(String domain) {
            this.domain = domain;
        }

        public String getDomain() {
            return domain;
        }

        /**
         * 根据URL判断平台类型
         */
        public static GitPlatform fromUrl(String url) {
            if (url == null) {
                return CUSTOM;
            }

            String lowerUrl = url.toLowerCase();
            for (GitPlatform platform : values()) {
                if (platform != CUSTOM && lowerUrl.contains(platform.domain)) {
                    return platform;
                }
            }
            return CUSTOM;
        }
    }

    /**
     * 从URL解析仓库信息
     */
    public static GitRepositoryInfo fromUrl(String remoteUrl) {
        GitRepositoryInfo info = new GitRepositoryInfo();
        info.setRemoteUrl(remoteUrl);
        info.setPlatform(GitPlatform.fromUrl(remoteUrl));
        info.setStatus(RepositoryStatus.NOT_INITIALIZED);

        // 解析owner和repositoryName
        // 支持的URL格式：
        // - https://github.com/owner/repo.git
        // - git@github.com:owner/repo.git
        // - https://github.com/owner/repo

        if (remoteUrl != null) {
            String url = remoteUrl.trim();

            // 移除.git后缀
            if (url.endsWith(".git")) {
                url = url.substring(0, url.length() - 4);
            }

            // 解析SSH格式
            if (url.contains("@") && url.contains(":")) {
                String[] parts = url.split(":");
                if (parts.length == 2) {
                    String[] pathParts = parts[1].split("/");
                    if (pathParts.length >= 2) {
                        info.setOwner(pathParts[pathParts.length - 2]);
                        info.setRepositoryName(pathParts[pathParts.length - 1]);
                    }
                }
            }
            // 解析HTTP/HTTPS格式
            else if (url.contains("://")) {
                String[] parts = url.split("://");
                if (parts.length == 2) {
                    String[] pathParts = parts[1].split("/");
                    if (pathParts.length >= 3) {
                        info.setOwner(pathParts[pathParts.length - 2]);
                        info.setRepositoryName(pathParts[pathParts.length - 1]);
                    }
                }
            }
        }

        return info;
    }

    /**
     * 获取仓库标识符
     */
    public String getRepositoryIdentifier() {
        if (owner != null && repositoryName != null) {
            return owner + "/" + repositoryName;
        }
        return remoteUrl;
    }

    /**
     * 判断是否需要同步
     */
    public boolean needsSync() {
        return status == RepositoryStatus.NOT_INITIALIZED
            || status == RepositoryStatus.ERROR
            || hasUnpushedCommits == Boolean.TRUE;
    }
}