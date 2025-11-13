package ai.opendw.koalawiki.core.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Git操作服务
 * 提供Git仓库的克隆、拉取、提交历史查询等功能
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Slf4j
@Service
public class GitService {

    @Value("${koalawiki.git.storage-path:/data/koalawiki/git}")
    private String storagePath;

    @Value("${koalawiki.git.max-depth:0}")
    private int maxDepth;

    @Value("${koalawiki.git.timeout:300000}")
    private int timeout;

    @Value("${koalawiki.git.cache-cleanup-days:30}")
    private int cacheCleanupDays;

    /**
     * 克隆Git仓库
     *
     * @param remoteUrl   远程仓库URL
     * @param credentials 认证信息（可选）
     * @return 仓库信息
     */
    public GitRepositoryInfo cloneRepository(String remoteUrl, GitCredentials credentials) {
        log.info("Starting to clone repository: {}", remoteUrl);

        try {
            // 生成本地路径
            String localPath = generateLocalPath(remoteUrl);
            File localDir = new File(localPath);

            // 如果目录已存在，先尝试更新
            if (localDir.exists() && isGitRepository(localDir)) {
                log.info("Repository already exists, pulling latest changes: {}", localPath);
                return pullRepository(localPath, credentials);
            }

            // 创建目录
            Files.createDirectories(Paths.get(localPath));

            // 设置克隆命令
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(localDir)
                    .setCloneAllBranches(false)
                    .setProgressMonitor(new LogProgressMonitor());

            // 设置克隆深度
            // JGit 5.x 版本不支持 setDepth，需要通过其他方式实现浅克隆
            // if (maxDepth > 0) {
            //     cloneCommand.setDepth(maxDepth);
            // }

            // 设置认证
            CredentialsProvider credentialsProvider = createCredentialsProvider(credentials);
            if (credentialsProvider != null) {
                cloneCommand.setCredentialsProvider(credentialsProvider);
            }

            // 执行克隆
            try (Git git = cloneCommand.call()) {
                log.info("Repository cloned successfully: {}", localPath);
                return buildRepositoryInfo(git, remoteUrl);
            }

        } catch (Exception e) {
            log.error("Failed to clone repository: {}", remoteUrl, e);
            throw new GitOperationException("Failed to clone repository: " + e.getMessage(), e);
        }
    }

    /**
     * 拉取最新代码
     *
     * @param localPath   本地仓库路径
     * @param credentials 认证信息（可选）
     * @return 仓库信息
     */
    public GitRepositoryInfo pullRepository(String localPath, GitCredentials credentials) {
        log.info("Pulling latest changes for repository: {}", localPath);

        try {
            File localDir = new File(localPath);
            if (!localDir.exists() || !isGitRepository(localDir)) {
                throw new GitOperationException("Not a valid Git repository: " + localPath);
            }

            try (Git git = Git.open(localDir)) {
                // 设置拉取命令
                PullCommand pullCommand = git.pull();

                // 设置认证
                CredentialsProvider credentialsProvider = createCredentialsProvider(credentials);
                if (credentialsProvider != null) {
                    pullCommand.setCredentialsProvider(credentialsProvider);
                }

                // 执行拉取
                PullResult pullResult = pullCommand.call();

                if (pullResult.isSuccessful()) {
                    log.info("Repository pulled successfully: {}", localPath);
                } else {
                    log.warn("Pull was not successful: {}", pullResult.toString());
                }

                // 获取远程URL
                String remoteUrl = git.getRepository()
                        .getConfig()
                        .getString("remote", "origin", "url");

                return buildRepositoryInfo(git, remoteUrl);
            }

        } catch (Exception e) {
            log.error("Failed to pull repository: {}", localPath, e);
            throw new GitOperationException("Failed to pull repository: " + e.getMessage(), e);
        }
    }

    /**
     * 获取提交历史
     *
     * @param localPath 本地仓库路径
     * @param limit     限制数量
     * @return 提交历史列表
     */
    public List<CommitInfo> getCommitHistory(String localPath, int limit) {
        log.debug("Getting commit history for repository: {}", localPath);

        try {
            File localDir = new File(localPath);
            if (!localDir.exists() || !isGitRepository(localDir)) {
                throw new GitOperationException("Not a valid Git repository: " + localPath);
            }

            List<CommitInfo> commits = new ArrayList<>();

            try (Git git = Git.open(localDir)) {
                Iterable<RevCommit> revCommits = git.log()
                        .setMaxCount(limit)
                        .call();

                for (RevCommit revCommit : revCommits) {
                    CommitInfo commitInfo = CommitInfo.builder()
                            .commitId(revCommit.getId().getName())
                            .message(revCommit.getFullMessage())
                            .author(revCommit.getAuthorIdent().getName())
                            .authorEmail(revCommit.getAuthorIdent().getEmailAddress())
                            .commitTime(new Date(revCommit.getCommitTime() * 1000L))
                            .parentIds(Arrays.stream(revCommit.getParents())
                                    .map(parent -> parent.getId().getName())
                                    .collect(Collectors.toList()))
                            .build();

                    commits.add(commitInfo);
                }
            }

            return commits;

        } catch (Exception e) {
            log.error("Failed to get commit history: {}", localPath, e);
            throw new GitOperationException("Failed to get commit history: " + e.getMessage(), e);
        }
    }

    /**
     * 获取两个提交之间的差异
     *
     * @param localPath  本地仓库路径
     * @param fromCommit 起始提交ID
     * @param toCommit   结束提交ID
     * @return 变更文件列表
     */
    public List<String> getCommitDiff(String localPath, String fromCommit, String toCommit) {
        log.debug("Getting diff between {} and {} for repository: {}", fromCommit, toCommit, localPath);

        try {
            File localDir = new File(localPath);
            if (!localDir.exists() || !isGitRepository(localDir)) {
                throw new GitOperationException("Not a valid Git repository: " + localPath);
            }

            List<String> changedFiles = new ArrayList<>();

            try (Git git = Git.open(localDir)) {
                Repository repository = git.getRepository();

                ObjectId fromId = repository.resolve(fromCommit);
                ObjectId toId = repository.resolve(toCommit);

                if (fromId == null || toId == null) {
                    throw new GitOperationException("Invalid commit IDs");
                }

                DiffCommand diffCommand = git.diff()
                        .setOldTree(prepareTreeParser(repository, fromId))
                        .setNewTree(prepareTreeParser(repository, toId));

                diffCommand.call().forEach(diff -> {
                    changedFiles.add(diff.getNewPath());
                });
            }

            return changedFiles;

        } catch (Exception e) {
            log.error("Failed to get commit diff: {}", localPath, e);
            throw new GitOperationException("Failed to get commit diff: " + e.getMessage(), e);
        }
    }

    /**
     * 获取仓库信息
     *
     * @param localPath 本地仓库路径
     * @return 仓库信息
     */
    public GitRepositoryInfo getRepositoryInfo(String localPath) {
        log.debug("Getting repository info for: {}", localPath);

        try {
            File localDir = new File(localPath);
            if (!localDir.exists() || !isGitRepository(localDir)) {
                throw new GitOperationException("Not a valid Git repository: " + localPath);
            }

            try (Git git = Git.open(localDir)) {
                String remoteUrl = git.getRepository()
                        .getConfig()
                        .getString("remote", "origin", "url");

                return buildRepositoryInfo(git, remoteUrl);
            }

        } catch (Exception e) {
            log.error("Failed to get repository info: {}", localPath, e);
            throw new GitOperationException("Failed to get repository info: " + e.getMessage(), e);
        }
    }

    /**
     * 清理过期的仓库缓存
     *
     * @return 清理的仓库数量
     */
    public int cleanupExpiredRepositories() {
        log.info("Starting cleanup of expired repositories");

        try {
            File storageDir = new File(storagePath);
            if (!storageDir.exists()) {
                return 0;
            }

            int cleanedCount = 0;
            long expirationTime = System.currentTimeMillis() - (cacheCleanupDays * 24L * 60 * 60 * 1000);

            File[] repositories = storageDir.listFiles(File::isDirectory);
            if (repositories != null) {
                for (File repo : repositories) {
                    if (repo.lastModified() < expirationTime) {
                        log.info("Cleaning up expired repository: {}", repo.getAbsolutePath());
                        deleteDirectory(repo);
                        cleanedCount++;
                    }
                }
            }

            log.info("Cleanup completed. Removed {} repositories", cleanedCount);
            return cleanedCount;

        } catch (Exception e) {
            log.error("Failed to cleanup expired repositories", e);
            return 0;
        }
    }

    // ===== 私有辅助方法 =====

    /**
     * 生成本地存储路径
     */
    private String generateLocalPath(String remoteUrl) {
        GitRepositoryInfo info = GitRepositoryInfo.fromUrl(remoteUrl);
        String identifier = info.getRepositoryIdentifier()
                .replace("/", "_")
                .replace("\\", "_");

        return Paths.get(storagePath, identifier).toString();
    }

    /**
     * 检查目录是否为Git仓库
     */
    private boolean isGitRepository(File directory) {
        File gitDir = new File(directory, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    /**
     * 创建认证提供者
     */
    private CredentialsProvider createCredentialsProvider(GitCredentials credentials) {
        if (credentials == null || credentials.getType() == GitCredentials.CredentialType.NONE) {
            return null;
        }

        switch (credentials.getType()) {
            case HTTP_BASIC:
                return new UsernamePasswordCredentialsProvider(
                        credentials.getUsername(),
                        credentials.getPassword()
                );

            case OAUTH_TOKEN:
                return new UsernamePasswordCredentialsProvider(
                        "token",
                        credentials.getPassword()
                );

            case SSH_KEY:
                // SSH密钥认证需要额外配置
                configureSshAuth(credentials);
                return null;

            default:
                return null;
        }
    }

    /**
     * 配置SSH认证
     */
    private void configureSshAuth(GitCredentials credentials) {
        // TODO: 实现SSH密钥认证配置
        log.warn("SSH authentication not yet implemented");
    }

    /**
     * 构建仓库信息
     */
    private GitRepositoryInfo buildRepositoryInfo(Git git, String remoteUrl) throws IOException, GitAPIException {
        Repository repository = git.getRepository();

        GitRepositoryInfo info = GitRepositoryInfo.fromUrl(remoteUrl);
        info.setLocalPath(repository.getDirectory().getParentFile().getAbsolutePath());
        info.setCurrentBranch(repository.getBranch());
        info.setStatus(GitRepositoryInfo.RepositoryStatus.READY);
        info.setLastSyncTime(new Date());

        // 获取最新提交信息
        try {
            RevCommit latestCommit = git.log().setMaxCount(1).call().iterator().next();
            info.setLatestCommitId(latestCommit.getId().getName());
            info.setLatestCommitMessage(latestCommit.getShortMessage());
            info.setLatestCommitTime(new Date(latestCommit.getCommitTime() * 1000L));
            info.setLatestCommitAuthor(latestCommit.getAuthorIdent().getName());
        } catch (Exception e) {
            log.warn("No commits found in repository");
        }

        // 获取仓库大小和文件数量
        File repoDir = repository.getDirectory().getParentFile();
        info.setRepositorySize(calculateDirectorySize(repoDir));
        info.setFileCount(countFiles(repoDir));

        // 检查状态
        Status status = git.status().call();
        info.setHasUncommittedChanges(!status.isClean());

        return info;
    }

    /**
     * 准备树解析器
     */
    private org.eclipse.jgit.treewalk.AbstractTreeIterator prepareTreeParser(
            Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            org.eclipse.jgit.treewalk.TreeWalk treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repository);
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(false);
            return new org.eclipse.jgit.treewalk.CanonicalTreeParser(null, repository.newObjectReader(), commit.getTree());
        }
    }

    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory() && !".git".equals(file.getName())) {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }

    /**
     * 统计文件数量
     */
    private int countFiles(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    count++;
                } else if (file.isDirectory() && !".git".equals(file.getName())) {
                    count += countFiles(file);
                }
            }
        }
        return count;
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) throws IOException {
        Files.walk(directory.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /**
     * 进度监控器
     */
    private static class LogProgressMonitor implements ProgressMonitor {
        @Override
        public void start(int totalTasks) {
            log.info("Starting {} tasks", totalTasks);
        }

        @Override
        public void beginTask(String title, int totalWork) {
            log.info("Beginning task: {} (total work: {})", title, totalWork);
        }

        @Override
        public void update(int completed) {
            log.debug("Progress update: {} completed", completed);
        }

        @Override
        public void endTask() {
            log.info("Task completed");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}