package ai.opendw.koalawiki.core.service;

import ai.opendw.koalawiki.core.git.*;
import ai.opendw.koalawiki.domain.warehouse.Warehouse;
import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncRecord;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仓库同步执行器实现
 * 负责执行具体的Git同步操作
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseSyncExecutorImpl implements IWarehouseSyncExecutor {

    private final GitService gitService;
    private final GitRepositoryManager repositoryManager;
    private final GitPathResolver pathResolver;
    private final WarehouseRepository warehouseRepository;
    private final ObjectMapper objectMapper;

    @Value("${koalawiki.git.default-username:}")
    private String defaultGitUsername;

    @Value("${koalawiki.git.default-password:}")
    private String defaultGitPassword;

    /**
     * 同步进度缓存
     */
    private final Map<String, ProgressInfo> progressCache = new ConcurrentHashMap<>();

    /**
     * 取消标记缓存
     */
    private final Set<String> cancelledTasks = ConcurrentHashMap.newKeySet();

    @Override
    public SyncExecutionResult execute(String warehouseId, WarehouseSyncRecord syncRecord) {
        log.info("Starting sync execution for warehouse: {}, recordId: {}", warehouseId, syncRecord.getId());

        long startTime = System.currentTimeMillis();
        SyncExecutionResult result = new SyncExecutionResult();

        try {
            // 初始化进度信息
            ProgressInfo progress = initializeProgress(syncRecord.getId());

            // 获取仓库信息
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                throw new IllegalArgumentException("Warehouse not found: " + warehouseId);
            }

            WarehouseEntity warehouse = warehouseOpt.get();
            String repositoryUrl = warehouse.getAddress();

            // 更新进度：开始克隆/拉取
            updateProgress(progress, "CLONE_OR_PULL", 1, 5, "Cloning or pulling repository");

            // 检查是否被取消
            if (isCancelled(syncRecord.getId())) {
                throw new InterruptedException("Sync cancelled");
            }

            // 获取或克隆仓库
            GitCredentials credentials = buildCredentials(warehouse);
            GitRepositoryInfo repoInfo = repositoryManager.getOrCloneRepository(repositoryUrl, credentials);

            // 更新进度：获取提交历史
            updateProgress(progress, "GET_COMMITS", 2, 5, "Getting commit history");

            // 获取最新提交信息
            String localPath = repoInfo.getLocalPath();
            List<CommitInfo> commits = gitService.getCommitHistory(localPath, 1);

            if (!commits.isEmpty()) {
                CommitInfo latestCommit = commits.get(0);
                result.setToVersion(latestCommit.getCommitId());
            }

            // 更新进度：分析文件变更
            updateProgress(progress, "ANALYZE_CHANGES", 3, 5, "Analyzing file changes");

            // 分析文件变更
            if (syncRecord.getFromVersion() != null && result.getToVersion() != null) {
                analyzeChanges(localPath, syncRecord.getFromVersion(), result.getToVersion(), result);
            } else {
                // 首次同步，统计所有文件
                countAllFiles(localPath, result);
            }

            // 更新进度：处理文档
            updateProgress(progress, "PROCESS_DOCUMENTS", 4, 5, "Processing documents");

            // TODO: 文档处理将在上层服务中完成

            // 检查是否被取消
            if (isCancelled(syncRecord.getId())) {
                throw new InterruptedException("Sync cancelled");
            }

            // 更新进度：完成
            updateProgress(progress, "COMPLETED", 5, 5, "Sync completed");

            // 设置成功结果
            result.setSuccess(true);
            result.setFromVersion(syncRecord.getFromVersion());
            result.setDuration(System.currentTimeMillis() - startTime);

            // 生成详细信息
            Map<String, Object> details = new HashMap<>();
            details.put("repositoryUrl", repositoryUrl);
            details.put("localPath", localPath);
            details.put("branch", repoInfo.getCurrentBranch());
            details.put("commitMessage", repoInfo.getLatestCommitMessage());
            details.put("commitAuthor", repoInfo.getLatestCommitAuthor());
            result.setDetails(objectMapper.writeValueAsString(details));

            log.info("Sync execution completed successfully for warehouse: {}", warehouseId);

        } catch (InterruptedException e) {
            log.warn("Sync execution cancelled for warehouse: {}", warehouseId);
            result.setSuccess(false);
            result.setErrorMessage("Sync cancelled");
            result.setDuration(System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Sync execution failed for warehouse: {}", warehouseId, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setDuration(System.currentTimeMillis() - startTime);

        } finally {
            // 清理进度缓存和取消标记
            progressCache.remove(syncRecord.getId());
            cancelledTasks.remove(syncRecord.getId());
        }

        return result;
    }

    @Override
    public ValidationResult validate(String warehouseId) {
        log.debug("Validating warehouse: {}", warehouseId);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ValidationResult.invalid("Warehouse not found");
            }

            WarehouseEntity warehouse = warehouseOpt.get();
            String repositoryUrl = warehouse.getAddress();

            if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
                return ValidationResult.invalid("Repository URL is empty");
            }

            // 验证URL格式
            if (!pathResolver.isHttpUrl(repositoryUrl) && !pathResolver.isSshUrl(repositoryUrl)) {
                return ValidationResult.invalid("Invalid repository URL format");
            }

            ValidationResult result = ValidationResult.valid();
            result.setRepositoryUrl(repositoryUrl);

            // 检查是否需要认证
            // TODO: 实际检查仓库是否为私有仓库
            result.setAuthRequired(false);

            return result;

        } catch (Exception e) {
            log.error("Validation failed for warehouse: {}", warehouseId, e);
            return ValidationResult.invalid("Validation failed: " + e.getMessage());
        }
    }

    @Override
    public boolean cancel(String warehouseId, String recordId) {
        log.info("Cancelling sync for warehouse: {}, recordId: {}", warehouseId, recordId);

        // 添加到取消集合
        cancelledTasks.add(recordId);

        // TODO: 实际中断正在进行的Git操作

        return true;
    }

    @Override
    public ProgressInfo getProgress(String recordId) {
        return progressCache.get(recordId);
    }

    // ===== 私有辅助方法 =====

    /**
     * 构建Git凭证
     *
     * <p>优先使用仓库配置的用户名密码，如果未配置则使用系统默认凭据</p>
     *
     * @param warehouse 仓库实体
     * @return Git认证信息，如果都未配置则返回null
     * @author zhourui(V33215020)
     * @since 2025/11/21
     */
    private GitCredentials buildCredentials(WarehouseEntity warehouse) {
        String username = warehouse.getGitUserName();
        String password = warehouse.getGitPassword();

        if (username == null || username.trim().isEmpty()) {
            username = defaultGitUsername;
            if (username != null && !username.trim().isEmpty()) {
                log.debug("使用系统默认Git用户名");
            }
        }

        if (password == null || password.trim().isEmpty()) {
            password = defaultGitPassword;
            if (password != null && !password.trim().isEmpty()) {
                log.debug("使用系统默认Git密码");
            }
        }

        // 如果用户名或密码为空，返回null（用于公开仓库）
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            log.debug("未配置Git凭据，将尝试访问公开仓库");
            return null;
        }

        return GitCredentials.httpBasic(username, password);
    }

    /**
     * 分析文件变更
     */
    private void analyzeChanges(String localPath, String fromVersion, String toVersion, SyncExecutionResult result) {
        try {
            List<String> changedFiles = gitService.getCommitDiff(localPath, fromVersion, toVersion);

            // 简单统计（实际应该更详细地分析每个文件的状态）
            result.setFileCount(changedFiles.size());
            result.setUpdatedCount(changedFiles.size());

            // TODO: 更详细的变更分析

        } catch (Exception e) {
            log.error("Failed to analyze changes", e);
            // 降级处理，统计所有文件
            countAllFiles(localPath, result);
        }
    }

    /**
     * 统计所有文件
     */
    private void countAllFiles(String localPath, SyncExecutionResult result) {
        File directory = new File(localPath);
        int fileCount = countFiles(directory, 0);
        result.setFileCount(fileCount);
        result.setAddedCount(fileCount); // 首次同步，所有文件都是新增
    }

    /**
     * 递归统计文件数量
     */
    private int countFiles(File directory, int currentCount) {
        if (currentCount > 10000) {
            // 防止文件过多导致栈溢出
            return currentCount;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // 跳过.git目录
                if (".git".equals(file.getName())) {
                    continue;
                }

                if (file.isFile()) {
                    currentCount++;
                } else if (file.isDirectory()) {
                    currentCount = countFiles(file, currentCount);
                }
            }
        }
        return currentCount;
    }

    /**
     * 初始化进度信息
     */
    private ProgressInfo initializeProgress(String recordId) {
        ProgressInfo progress = new ProgressInfo();
        progress.setRecordId(recordId);
        progress.setTotalSteps(5);
        progress.setCompletedSteps(0);
        progress.setProgressPercentage(0);
        progressCache.put(recordId, progress);
        return progress;
    }

    /**
     * 更新进度信息
     */
    private void updateProgress(ProgressInfo progress, String step, int completedSteps,
                                int totalSteps, String operation) {
        progress.setCurrentStep(step);
        progress.setCompletedSteps(completedSteps);
        progress.setTotalSteps(totalSteps);
        progress.setCurrentOperation(operation);
        progress.calculateProgress();

        log.debug("Progress update - recordId: {}, step: {}, progress: {}%",
                 progress.getRecordId(), step, progress.getProgressPercentage());
    }

    /**
     * 检查任务是否被取消
     */
    private boolean isCancelled(String recordId) {
        return cancelledTasks.contains(recordId);
    }
}