package ai.opendw.koalawiki.core.document.processors;

import ai.opendw.koalawiki.core.document.pipeline.AbstractDocumentProcessor;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingContext;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult;
import ai.opendw.koalawiki.core.git.CommitInfo;
import ai.opendw.koalawiki.core.git.GitService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 变更日志处理器
 * 根据Git提交历史生成变更日志（CHANGELOG.md）
 *
 * 参考C#实现: Pipeline中的变更日志生成逻辑
 */
@Slf4j
@Component
public class ChangeLogProcessor extends AbstractDocumentProcessor {

    private static final String CHANGELOG_FILENAME = "CHANGELOG.md";
    private static final int DEFAULT_COMMIT_LIMIT = 100;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final GitService gitService;

    @Autowired
    public ChangeLogProcessor(GitService gitService) {
        super("ChangeLogProcessor", 50);
        this.gitService = gitService;
    }

    @Override
    public boolean canProcess(DocumentProcessingContext context) {
        // 只处理Git仓库
        if (context.getDocumentType() != DocumentProcessingContext.DocumentType.DIRECTORY) {
            return false;
        }

        // 检查是否启用变更日志生成
        Boolean generateChangelog = context.getParameter("generateChangelog", Boolean.class)
            .orElse(false);

        // 检查是否为Git仓库
        Boolean isGitRepo = context.getParameter("isGitRepo", Boolean.class)
            .orElse(false);

        return generateChangelog && isGitRepo;
    }

    @Override
    protected DocumentProcessingResult doProcess(DocumentProcessingContext context) {
        log.info("生成变更日志: {}", context.getDocumentPath());

        DocumentProcessingResult result = DocumentProcessingResult.success(getName());

        try {
            Path repoPath = context.getLocalPath();
            if (repoPath == null || !Files.exists(repoPath)) {
                return DocumentProcessingResult.failure(getName(),
                    "仓库路径不存在: " + context.getDocumentPath());
            }

            // 获取提交历史
            Integer commitLimit = context.getParameter("commitLimit", Integer.class)
                .orElse(DEFAULT_COMMIT_LIMIT);

            List<CommitInfo> commits = gitService.getCommitHistory(
                repoPath.toString(),
                commitLimit
            );

            if (commits.isEmpty()) {
                return DocumentProcessingResult.skip(getName(),
                    "没有找到提交历史");
            }

            // 按日期分组提交
            Map<String, List<CommitInfo>> commitsByDate = groupCommitsByDate(commits);

            // 生成变更日志内容
            String changelogContent = generateChangelogContent(commitsByDate, context);

            // 写入CHANGELOG文件
            Path changelogPath = repoPath.resolve(CHANGELOG_FILENAME);
            Files.write(changelogPath, changelogContent.getBytes(StandardCharsets.UTF_8));

            result.getGeneratedFiles().add(changelogPath.toString());
            result.setMessage("成功生成变更日志，包含 " + commits.size() + " 个提交");

            // 添加输出信息
            result.addOutput("changelogPath", changelogPath.toString());
            result.addOutput("totalCommits", commits.size());
            result.addOutput("dateGroups", commitsByDate.size());

            // 更新指标
            result.getMetrics()
                .setDocumentsProcessed(1)
                .setBytesProcessed(changelogContent.getBytes(StandardCharsets.UTF_8).length);

            log.info("变更日志生成完成: 提交数={}, 日期组数={}",
                commits.size(), commitsByDate.size());

        } catch (Exception e) {
            log.error("变更日志生成失败: {}", context.getDocumentPath(), e);
            return DocumentProcessingResult.failure(getName(),
                "变更日志生成异常: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 按日期分组提交
     */
    private Map<String, List<CommitInfo>> groupCommitsByDate(List<CommitInfo> commits) {
        // 使用LinkedHashMap保持日期顺序
        Map<String, List<CommitInfo>> grouped = new LinkedHashMap<>();

        for (CommitInfo commit : commits) {
            String dateKey = DATE_FORMAT.format(commit.getCommitTime());
            grouped.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(commit);
        }

        return grouped;
    }

    /**
     * 生成变更日志内容
     */
    private String generateChangelogContent(Map<String, List<CommitInfo>> commitsByDate,
                                             DocumentProcessingContext context) {
        StringBuilder content = new StringBuilder();

        // 标题
        String projectName = context.getParameter("projectName", String.class)
            .orElse("项目");
        content.append("# ").append(projectName).append(" 变更日志\n\n");

        // 说明
        content.append("本文档记录了项目的主要变更历史。\n\n");
        content.append("---\n\n");

        // 按日期输出提交记录
        for (Map.Entry<String, List<CommitInfo>> entry : commitsByDate.entrySet()) {
            String date = entry.getKey();
            List<CommitInfo> commits = entry.getValue();

            content.append("## ").append(date).append("\n\n");

            // 按类型分组提交
            Map<String, List<CommitInfo>> commitsByType = groupCommitsByType(commits);

            for (Map.Entry<String, List<CommitInfo>> typeEntry : commitsByType.entrySet()) {
                String type = typeEntry.getKey();
                List<CommitInfo> typeCommits = typeEntry.getValue();

                content.append("### ").append(type).append("\n\n");

                for (CommitInfo commit : typeCommits) {
                    content.append("- ");

                    // 提交消息
                    String message = commit.getMessage();
                    // 移除类型前缀（如果有）
                    message = removeTypePrefix(message);
                    content.append(message);

                    // 提交哈希（短格式）
                    String shortHash = commit.getCommitId().substring(0,
                        Math.min(7, commit.getCommitId().length()));
                    content.append(" (`").append(shortHash).append("`)");

                    // 作者
                    content.append(" - ").append(commit.getAuthor());

                    content.append("\n");
                }

                content.append("\n");
            }
        }

        // 页脚
        content.append("---\n\n");
        content.append("*此变更日志由系统自动生成，最后更新时间: ")
               .append(new Date()).append("*\n");

        return content.toString();
    }

    /**
     * 按类型分组提交
     * 支持 Conventional Commits 规范
     */
    private Map<String, List<CommitInfo>> groupCommitsByType(List<CommitInfo> commits) {
        // 使用LinkedHashMap保持类型顺序
        Map<String, List<CommitInfo>> grouped = new LinkedHashMap<>();

        // 预定义类型顺序
        String[] typeOrder = {"feat", "fix", "docs", "style", "refactor",
            "perf", "test", "build", "ci", "chore", "other"};

        // 初始化所有类型
        for (String type : typeOrder) {
            grouped.put(getTypeDisplayName(type), new ArrayList<>());
        }

        // 分组提交
        for (CommitInfo commit : commits) {
            String type = extractCommitType(commit.getMessage());
            String displayName = getTypeDisplayName(type);
            grouped.get(displayName).add(commit);
        }

        // 移除空类型
        grouped.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        return grouped;
    }

    /**
     * 从提交消息中提取类型
     */
    private String extractCommitType(String message) {
        if (message == null || message.isEmpty()) {
            return "other";
        }

        // 支持 Conventional Commits 格式: type(scope): message
        String[] parts = message.split(":", 2);
        if (parts.length > 1) {
            String typeWithScope = parts[0].trim();
            // 移除scope
            int parenIndex = typeWithScope.indexOf('(');
            if (parenIndex > 0) {
                return typeWithScope.substring(0, parenIndex).toLowerCase();
            }
            return typeWithScope.toLowerCase();
        }

        // 简单的关键词匹配
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.startsWith("feat") || lowerMessage.contains("新增") ||
            lowerMessage.contains("add")) {
            return "feat";
        } else if (lowerMessage.startsWith("fix") || lowerMessage.contains("修复") ||
                   lowerMessage.contains("bug")) {
            return "fix";
        } else if (lowerMessage.startsWith("docs") || lowerMessage.contains("文档")) {
            return "docs";
        } else if (lowerMessage.startsWith("refactor") || lowerMessage.contains("重构")) {
            return "refactor";
        } else if (lowerMessage.startsWith("test") || lowerMessage.contains("测试")) {
            return "test";
        }

        return "other";
    }

    /**
     * 获取类型的显示名称
     */
    private String getTypeDisplayName(String type) {
        switch (type.toLowerCase()) {
            case "feat":
                return "✨ 新特性";
            case "fix":
                return "🐛 Bug修复";
            case "docs":
                return "📝 文档";
            case "style":
                return "💄 代码样式";
            case "refactor":
                return "♻️ 代码重构";
            case "perf":
                return "⚡️ 性能优化";
            case "test":
                return "✅ 测试";
            case "build":
                return "📦 构建";
            case "ci":
                return "👷 CI/CD";
            case "chore":
                return "🔧 杂项";
            default:
                return "📌 其他";
        }
    }

    /**
     * 移除提交消息中的类型前缀
     */
    private String removeTypePrefix(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // 移除 Conventional Commits 格式前缀
        String[] parts = message.split(":", 2);
        if (parts.length > 1) {
            return parts[1].trim();
        }

        return message;
    }

    /**
     * 变更日志条目
     */
    @Data
    public static class ChangeLogEntry {
        private String date;
        private String type;
        private String message;
        private String commitId;
        private String author;
        private Date commitTime;
    }
}
