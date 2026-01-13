package ai.opendw.koalawiki.core.git;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Git仓库管理器
 * 管理本地Git仓库的缓存和生命周期
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitRepositoryManager {

    private final GitService gitService;
    private final GitPathResolver pathResolver;

    /**
     * 仓库缓存（remoteUrl -> GitRepositoryInfo）
     */
    private final Map<String, GitRepositoryInfo> repositoryCache = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        log.info("GitRepositoryManager initialized");
        // 扫描本地已存在的仓库
        scanExistingRepositories();
    }

    /**
     * 获取或克隆仓库
     *
     * @param remoteUrl   远程仓库URL
     * @param credentials 认证信息（可选）
     * @return 仓库信息
     */
    public GitRepositoryInfo getOrCloneRepository(String remoteUrl, GitCredentials credentials) {
        log.debug("Getting or cloning repository: {}", remoteUrl);

        // 从缓存获取
        GitRepositoryInfo cachedInfo = repositoryCache.get(remoteUrl);
        if (cachedInfo != null && cachedInfo.getStatus() == GitRepositoryInfo.RepositoryStatus.READY) {
            log.debug("Repository found in cache: {}", remoteUrl);
            return cachedInfo;
        }

        // 检查本地是否已存在
        String localPath = pathResolver.getLocalPath(remoteUrl);
        File localDir = new File(localPath);

        if (localDir.exists() && isValidRepository(localDir)) {
            log.info("Repository exists locally, pulling latest changes: {}", localPath);
            GitRepositoryInfo info = gitService.pullRepository(localPath, credentials);
            repositoryCache.put(remoteUrl, info);
            return info;
        }

        // 克隆新仓库
        log.info("Cloning new repository: {}", remoteUrl);
        GitRepositoryInfo info = gitService.cloneRepository(remoteUrl, credentials);
        repositoryCache.put(remoteUrl, info);
        return info;
    }

    /**
     * 更新仓库
     *
     * @param remoteUrl   远程仓库URL
     * @param credentials 认证信息（可选）
     * @return 仓库信息
     */
    public GitRepositoryInfo updateRepository(String remoteUrl, GitCredentials credentials) {
        log.info("Updating repository: {}", remoteUrl);

        String localPath = pathResolver.getLocalPath(remoteUrl);
        GitRepositoryInfo info = gitService.pullRepository(localPath, credentials);

        // 更新缓存
        repositoryCache.put(remoteUrl, info);
        return info;
    }

    /**
     * 获取仓库信息
     *
     * @param remoteUrl 远程仓库URL
     * @return 仓库信息（如果存在）
     */
    public GitRepositoryInfo getRepositoryInfo(String remoteUrl) {
        // 优先从缓存获取
        GitRepositoryInfo cachedInfo = repositoryCache.get(remoteUrl);
        if (cachedInfo != null) {
            return cachedInfo;
        }

        // 检查本地是否存在
        String localPath = pathResolver.getLocalPath(remoteUrl);
        File localDir = new File(localPath);

        if (localDir.exists() && isValidRepository(localDir)) {
            GitRepositoryInfo info = gitService.getRepositoryInfo(localPath);
            repositoryCache.put(remoteUrl, info);
            return info;
        }

        return null;
    }

    /**
     * 删除仓库
     *
     * @param remoteUrl 远程仓库URL
     * @return 是否删除成功
     */
    public boolean deleteRepository(String remoteUrl) {
        log.info("Deleting repository: {}", remoteUrl);

        String localPath = pathResolver.getLocalPath(remoteUrl);
        File localDir = new File(localPath);

        if (localDir.exists()) {
            boolean deleted = deleteDirectory(localDir);
            if (deleted) {
                repositoryCache.remove(remoteUrl);
                log.info("Repository deleted successfully: {}", remoteUrl);
                return true;
            }
        }

        return false;
    }

    /**
     * 清理过期的仓库（定时任务，每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredRepositories() {
        log.info("Starting scheduled cleanup of expired repositories");
        int cleanedCount = gitService.cleanupExpiredRepositories();

        // 更新缓存
        if (cleanedCount > 0) {
            scanExistingRepositories();
        }
    }

    /**
     * 获取缓存的仓库数量
     */
    public int getCachedRepositoryCount() {
        return repositoryCache.size();
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        log.info("Clearing repository cache");
        repositoryCache.clear();
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        log.info("Refreshing repository cache");
        clearCache();
        scanExistingRepositories();
    }

    // ===== 私有方法 =====

    /**
     * 扫描已存在的仓库
     */
    private void scanExistingRepositories() {
        log.info("Scanning existing repositories");

        String storagePath = pathResolver.getStoragePath();
        File storageDir = new File(storagePath);

        if (!storageDir.exists() || !storageDir.isDirectory()) {
            log.info("Storage directory does not exist or is not a directory: {}", storagePath);
            return;
        }

        File[] repositories = storageDir.listFiles(File::isDirectory);
        if (repositories == null || repositories.length == 0) {
            log.info("No repositories found in storage directory");
            return;
        }

        int scannedCount = 0;
        for (File repo : repositories) {
            if (isValidRepository(repo)) {
                try {
                    GitRepositoryInfo info = gitService.getRepositoryInfo(repo.getAbsolutePath());
                    if (info != null && info.getRemoteUrl() != null) {
                        repositoryCache.put(info.getRemoteUrl(), info);
                        scannedCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to scan repository: {}", repo.getAbsolutePath(), e);
                }
            }
        }

        log.info("Scanned {} existing repositories", scannedCount);
    }

    /**
     * 检查是否为有效的Git仓库
     */
    private boolean isValidRepository(File directory) {
        File gitDir = new File(directory, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    /**
     * 递归删除目录
     */
    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        log.warn("Failed to delete file: {}", file.getAbsolutePath());
                    }
                }
            }
        }
        return directory.delete();
    }
}