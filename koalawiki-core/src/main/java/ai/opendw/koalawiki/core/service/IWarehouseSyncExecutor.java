package ai.opendw.koalawiki.core.service;

import ai.opendw.koalawiki.domain.warehouse.WarehouseSyncRecord;

/**
 * 仓库同步执行器接口
 * 负责执行具体的同步任务
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
public interface IWarehouseSyncExecutor {

    /**
     * 执行仓库同步
     *
     * @param warehouseId 仓库ID
     * @param syncRecord  同步记录
     * @return 执行结果
     */
    SyncExecutionResult execute(String warehouseId, WarehouseSyncRecord syncRecord);

    /**
     * 验证仓库是否可以同步
     *
     * @param warehouseId 仓库ID
     * @return 验证结果
     */
    ValidationResult validate(String warehouseId);

    /**
     * 取消同步执行
     *
     * @param warehouseId 仓库ID
     * @param recordId    同步记录ID
     * @return 是否成功取消
     */
    boolean cancel(String warehouseId, String recordId);

    /**
     * 获取同步进度
     *
     * @param recordId 同步记录ID
     * @return 进度信息
     */
    ProgressInfo getProgress(String recordId);

    /**
     * 同步执行结果
     */
    class SyncExecutionResult {
        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 错误消息
         */
        private String errorMessage;

        /**
         * 从版本
         */
        private String fromVersion;

        /**
         * 到版本
         */
        private String toVersion;

        /**
         * 总文件数
         */
        private int fileCount;

        /**
         * 新增文件数
         */
        private int addedCount;

        /**
         * 更新文件数
         */
        private int updatedCount;

        /**
         * 删除文件数
         */
        private int deletedCount;

        /**
         * 执行耗时（毫秒）
         */
        private long duration;

        /**
         * 详细信息
         */
        private String details;

        // Getters and Setters

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getFromVersion() {
            return fromVersion;
        }

        public void setFromVersion(String fromVersion) {
            this.fromVersion = fromVersion;
        }

        public String getToVersion() {
            return toVersion;
        }

        public void setToVersion(String toVersion) {
            this.toVersion = toVersion;
        }

        public int getFileCount() {
            return fileCount;
        }

        public void setFileCount(int fileCount) {
            this.fileCount = fileCount;
        }

        public int getAddedCount() {
            return addedCount;
        }

        public void setAddedCount(int addedCount) {
            this.addedCount = addedCount;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }

        public void setUpdatedCount(int updatedCount) {
            this.updatedCount = updatedCount;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public void setDeletedCount(int deletedCount) {
            this.deletedCount = deletedCount;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        /**
         * 创建成功结果
         */
        public static SyncExecutionResult success() {
            SyncExecutionResult result = new SyncExecutionResult();
            result.setSuccess(true);
            return result;
        }

        /**
         * 创建失败结果
         */
        public static SyncExecutionResult failure(String errorMessage) {
            SyncExecutionResult result = new SyncExecutionResult();
            result.setSuccess(false);
            result.setErrorMessage(errorMessage);
            return result;
        }
    }

    /**
     * 验证结果
     */
    class ValidationResult {
        /**
         * 是否有效
         */
        private boolean valid;

        /**
         * 错误消息
         */
        private String errorMessage;

        /**
         * 仓库URL
         */
        private String repositoryUrl;

        /**
         * 是否需要认证
         */
        private boolean authRequired;

        // Getters and Setters

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getRepositoryUrl() {
            return repositoryUrl;
        }

        public void setRepositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
        }

        public boolean isAuthRequired() {
            return authRequired;
        }

        public void setAuthRequired(boolean authRequired) {
            this.authRequired = authRequired;
        }

        /**
         * 创建有效结果
         */
        public static ValidationResult valid() {
            ValidationResult result = new ValidationResult();
            result.setValid(true);
            return result;
        }

        /**
         * 创建无效结果
         */
        public static ValidationResult invalid(String errorMessage) {
            ValidationResult result = new ValidationResult();
            result.setValid(false);
            result.setErrorMessage(errorMessage);
            return result;
        }
    }

    /**
     * 进度信息
     */
    class ProgressInfo {
        /**
         * 同步记录ID
         */
        private String recordId;

        /**
         * 当前步骤
         */
        private String currentStep;

        /**
         * 总步骤数
         */
        private int totalSteps;

        /**
         * 已完成步骤数
         */
        private int completedSteps;

        /**
         * 进度百分比（0-100）
         */
        private int progressPercentage;

        /**
         * 当前操作描述
         */
        private String currentOperation;

        /**
         * 预计剩余时间（秒）
         */
        private Long estimatedRemainingSeconds;

        // Getters and Setters

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }

        public String getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(String currentStep) {
            this.currentStep = currentStep;
        }

        public int getTotalSteps() {
            return totalSteps;
        }

        public void setTotalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
        }

        public int getCompletedSteps() {
            return completedSteps;
        }

        public void setCompletedSteps(int completedSteps) {
            this.completedSteps = completedSteps;
        }

        public int getProgressPercentage() {
            return progressPercentage;
        }

        public void setProgressPercentage(int progressPercentage) {
            this.progressPercentage = progressPercentage;
        }

        public String getCurrentOperation() {
            return currentOperation;
        }

        public void setCurrentOperation(String currentOperation) {
            this.currentOperation = currentOperation;
        }

        public Long getEstimatedRemainingSeconds() {
            return estimatedRemainingSeconds;
        }

        public void setEstimatedRemainingSeconds(Long estimatedRemainingSeconds) {
            this.estimatedRemainingSeconds = estimatedRemainingSeconds;
        }

        /**
         * 计算进度百分比
         */
        public void calculateProgress() {
            if (totalSteps > 0) {
                this.progressPercentage = (completedSteps * 100) / totalSteps;
            }
        }
    }
}