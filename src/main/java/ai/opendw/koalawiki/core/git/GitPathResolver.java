package ai.opendw.koalawiki.core.git;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Git路径解析器
 * 负责解析Git URL并生成本地存储路径
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Slf4j
@Component
public class GitPathResolver {

    @Value("${koalawiki.git.storage-path:/data/koalawiki/git}")
    private String storagePath;

    /**
     * 获取存储路径
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * 根据领域代码和仓库名称生成本地存储路径
     *
     * @param domainCode    领域代码
     * @param warehouseName 仓库名称
     * @return 本地存储路径，格式: storagePath/domainCode/warehouseName
     */
    public String getLocalPathByDomain(String domainCode, String warehouseName) {
        return Paths.get(storagePath, domainCode, warehouseName).toString();
    }

    /**
     * 根据远程URL生成本地存储路径
     *
     * @param remoteUrl 远程仓库URL
     * @return 本地存储路径
     */
    public String getLocalPath(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Remote URL cannot be null or empty");
        }

        // 解析URL获取路径组件
        String pathComponent = extractPathComponent(remoteUrl);

        // 生成本地路径
        return Paths.get(storagePath, pathComponent).toString();
    }

    /**
     * 从本地路径获取仓库名称
     *
     * @param localPath 本地路径
     * @return 仓库名称
     */
    public String getRepositoryName(String localPath) {
        File file = new File(localPath);
        return file.getName();
    }

    /**
     * 解析仓库所有者和名称
     *
     * @param remoteUrl 远程仓库URL
     * @return [owner, repository] 数组
     */
    public String[] parseOwnerAndRepository(String remoteUrl) {
        GitRepositoryInfo info = GitRepositoryInfo.fromUrl(remoteUrl);
        return new String[]{info.getOwner(), info.getRepositoryName()};
    }

    /**
     * 判断URL是否为SSH格式
     *
     * @param url URL字符串
     * @return 是否为SSH格式
     */
    public boolean isSshUrl(String url) {
        return url != null && (url.startsWith("git@") || url.contains(":") && !url.contains("://"));
    }

    /**
     * 判断URL是否为HTTP/HTTPS格式
     *
     * @param url URL字符串
     * @return 是否为HTTP/HTTPS格式
     */
    public boolean isHttpUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * 规范化Git URL
     * 确保URL格式统一，便于缓存和比较
     *
     * @param url 原始URL
     * @return 规范化后的URL
     */
    public String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }

        String normalized = url.trim().toLowerCase();

        // 移除末尾的斜杠
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        // 添加.git后缀（如果没有）
        if (!normalized.endsWith(".git") && !normalized.contains(".git/")) {
            normalized += ".git";
        }

        return normalized;
    }

    /**
     * 将SSH URL转换为HTTPS URL
     *
     * @param sshUrl SSH格式的URL
     * @return HTTPS格式的URL
     */
    public String sshToHttpsUrl(String sshUrl) {
        if (!isSshUrl(sshUrl)) {
            return sshUrl;
        }

        // 示例：git@github.com:owner/repo.git -> https://github.com/owner/repo.git
        String url = sshUrl.replace("git@", "https://");
        url = url.replace(":", "/");

        return url;
    }

    /**
     * 将HTTPS URL转换为SSH URL
     *
     * @param httpsUrl HTTPS格式的URL
     * @return SSH格式的URL
     */
    public String httpsToSshUrl(String httpsUrl) {
        if (!isHttpUrl(httpsUrl)) {
            return httpsUrl;
        }

        try {
            URL url = new URL(httpsUrl);
            String host = url.getHost();
            String path = url.getPath();

            // 移除开头的斜杠
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            return String.format("git@%s:%s", host, path);
        } catch (MalformedURLException e) {
            log.warn("Failed to parse URL: {}", httpsUrl, e);
            return httpsUrl;
        }
    }

    // ===== 私有方法 =====

    /**
     * 从URL提取路径组件
     */
    private String extractPathComponent(String remoteUrl) {
        GitRepositoryInfo info = GitRepositoryInfo.fromUrl(remoteUrl);

        // 使用平台、所有者和仓库名生成路径
        String platform = info.getPlatform().name().toLowerCase();
        String owner = info.getOwner();
        String repository = info.getRepositoryName();

        if (owner != null && repository != null) {
            // 格式：platform/owner/repository
            return Paths.get(platform, owner, repository).toString();
        }

        // 如果无法解析，使用URL的hash作为目录名
        return generateHashPath(remoteUrl);
    }

    /**
     * 生成基于URL哈希的路径
     */
    private String generateHashPath(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // 使用hash的前8位作为目录名
            return "unknown/" + hexString.toString().substring(0, 8);

        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate hash for URL: {}", url, e);
            // 降级方案：使用时间戳
            return "unknown/" + System.currentTimeMillis();
        }
    }
}