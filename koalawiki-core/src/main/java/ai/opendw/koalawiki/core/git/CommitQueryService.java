package ai.opendw.koalawiki.core.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Git提交查询服务
 * 提供Git提交历史查询、差异分析等功能
 */
@Slf4j
@Service
public class CommitQueryService {

    private final GitService gitService;

    @Autowired
    public CommitQueryService(GitService gitService) {
        this.gitService = gitService;
    }

    /**
     * 获取指定范围的提交记录
     *
     * @param repoPath 仓库路径
     * @param fromCommitId 起始提交ID（不包含）
     * @param toCommitId 结束提交ID（包含）
     * @param maxCount 最大数量
     * @return 提交信息列表
     */
    public List<CommitInfo> getCommitRange(String repoPath, String fromCommitId,
                                          String toCommitId, int maxCount) throws IOException, GitAPIException {
        log.info("获取提交范围: repoPath={}, from={}, to={}, maxCount={}",
                repoPath, fromCommitId, toCommitId, maxCount);

        try (Git git = Git.open(new File(repoPath))) {
            Repository repository = git.getRepository();
            LogCommand logCommand = git.log();

            // 设置范围
            if (toCommitId != null && !toCommitId.isEmpty()) {
                ObjectId toId = repository.resolve(toCommitId);
                logCommand.add(toId);
            }

            if (fromCommitId != null && !fromCommitId.isEmpty()) {
                ObjectId fromId = repository.resolve(fromCommitId);
                logCommand.not(fromId);
            }

            // 设置最大数量
            logCommand.setMaxCount(maxCount);

            List<CommitInfo> commits = new ArrayList<>();
            for (RevCommit revCommit : logCommand.call()) {
                commits.add(convertToCommitInfo(revCommit));
            }

            return commits;
        }
    }

    /**
     * 获取指定文件的提交历史
     *
     * @param repoPath 仓库路径
     * @param filePath 文件路径
     * @param maxCount 最大数量
     * @return 提交信息列表
     */
    public List<CommitInfo> getFileHistory(String repoPath, String filePath, int maxCount)
            throws IOException, GitAPIException {
        log.info("获取文件历史: repoPath={}, filePath={}, maxCount={}",
                repoPath, filePath, maxCount);

        try (Git git = Git.open(new File(repoPath))) {
            LogCommand logCommand = git.log()
                    .addPath(filePath)
                    .setMaxCount(maxCount);

            List<CommitInfo> commits = new ArrayList<>();
            for (RevCommit revCommit : logCommand.call()) {
                commits.add(convertToCommitInfo(revCommit));
            }

            return commits;
        }
    }

    /**
     * 获取提交详情
     *
     * @param repoPath 仓库路径
     * @param commitId 提交ID
     * @return 提交详情
     */
    public CommitDetail getCommitDetail(String repoPath, String commitId)
            throws IOException, GitAPIException {
        log.info("获取提交详情: repoPath={}, commitId={}", repoPath, commitId);

        try (Git git = Git.open(new File(repoPath))) {
            Repository repository = git.getRepository();
            ObjectId objectId = repository.resolve(commitId);

            if (objectId == null) {
                throw new IllegalArgumentException("提交不存在: " + commitId);
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(objectId);

                CommitDetail detail = new CommitDetail();
                detail.setCommitInfo(convertToCommitInfo(commit));
                detail.setChangedFiles(getChangedFiles(repository, commit));
                detail.setDiffSummary(getDiffSummary(repository, commit));

                return detail;
            }
        }
    }

    /**
     * 获取两个提交之间的差异
     *
     * @param repoPath 仓库路径
     * @param oldCommitId 旧提交ID
     * @param newCommitId 新提交ID
     * @return 差异列表
     */
    public List<FileDiff> getCommitDiff(String repoPath, String oldCommitId, String newCommitId)
            throws IOException, GitAPIException {
        log.info("获取提交差异: repoPath={}, oldCommit={}, newCommit={}",
                repoPath, oldCommitId, newCommitId);

        try (Git git = Git.open(new File(repoPath))) {
            Repository repository = git.getRepository();

            ObjectId oldId = repository.resolve(oldCommitId);
            ObjectId newId = repository.resolve(newCommitId);

            if (oldId == null || newId == null) {
                throw new IllegalArgumentException("提交ID无效");
            }

            List<FileDiff> diffs = new ArrayList<>();

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit oldCommit = revWalk.parseCommit(oldId);
                RevCommit newCommit = revWalk.parseCommit(newId);

                try (DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream())) {
                    formatter.setRepository(repository);
                    List<DiffEntry> diffEntries = formatter.scan(oldCommit.getTree(), newCommit.getTree());

                    for (DiffEntry entry : diffEntries) {
                        FileDiff fileDiff = new FileDiff();
                        fileDiff.setChangeType(entry.getChangeType().toString());
                        fileDiff.setOldPath(entry.getOldPath());
                        fileDiff.setNewPath(entry.getNewPath());
                        diffs.add(fileDiff);
                    }
                }
            }

            return diffs;
        }
    }

    /**
     * 搜索提交消息
     *
     * @param repoPath 仓库路径
     * @param keyword 关键词
     * @param maxCount 最大数量
     * @return 匹配的提交列表
     */
    public List<CommitInfo> searchCommits(String repoPath, String keyword, int maxCount)
            throws IOException, GitAPIException {
        log.info("搜索提交: repoPath={}, keyword={}, maxCount={}",
                repoPath, keyword, maxCount);

        try (Git git = Git.open(new File(repoPath))) {
            LogCommand logCommand = git.log().setMaxCount(maxCount * 10); // 获取更多以便过滤

            List<CommitInfo> matchedCommits = new ArrayList<>();
            String lowerKeyword = keyword.toLowerCase();

            for (RevCommit revCommit : logCommand.call()) {
                String message = revCommit.getFullMessage();
                if (message.toLowerCase().contains(lowerKeyword)) {
                    matchedCommits.add(convertToCommitInfo(revCommit));
                    if (matchedCommits.size() >= maxCount) {
                        break;
                    }
                }
            }

            return matchedCommits;
        }
    }

    /**
     * 获取分支间的提交差异
     *
     * @param repoPath 仓库路径
     * @param sourceBranch 源分支
     * @param targetBranch 目标分支
     * @return 提交列表
     */
    public List<CommitInfo> getBranchDifference(String repoPath, String sourceBranch,
                                                String targetBranch) throws IOException, GitAPIException {
        log.info("获取分支差异: repoPath={}, source={}, target={}",
                repoPath, sourceBranch, targetBranch);

        try (Git git = Git.open(new File(repoPath))) {
            Repository repository = git.getRepository();

            ObjectId sourceId = repository.resolve(sourceBranch);
            ObjectId targetId = repository.resolve(targetBranch);

            if (sourceId == null || targetId == null) {
                throw new IllegalArgumentException("分支不存在");
            }

            LogCommand logCommand = git.log()
                    .add(sourceId)
                    .not(targetId);

            List<CommitInfo> commits = new ArrayList<>();
            for (RevCommit revCommit : logCommand.call()) {
                commits.add(convertToCommitInfo(revCommit));
            }

            return commits;
        }
    }

    /**
     * 转换为CommitInfo
     */
    private CommitInfo convertToCommitInfo(RevCommit revCommit) {
        CommitInfo info = new CommitInfo();
        info.setCommitId(revCommit.getName());
        info.setAuthor(revCommit.getAuthorIdent().getName());
        info.setAuthorEmail(revCommit.getAuthorIdent().getEmailAddress());
        info.setMessage(revCommit.getFullMessage());
        info.setCommitTime(new Date(revCommit.getCommitTime() * 1000L));

        // 获取父提交
        if (revCommit.getParentCount() > 0) {
            List<String> parentIds = new ArrayList<>();
            for (int i = 0; i < revCommit.getParentCount(); i++) {
                parentIds.add(revCommit.getParent(i).getName());
            }
            info.setParentIds(parentIds);
        }

        return info;
    }

    /**
     * 获取变更文件列表
     */
    private List<String> getChangedFiles(Repository repository, RevCommit commit)
            throws IOException {
        List<String> files = new ArrayList<>();

        if (commit.getParentCount() == 0) {
            // 初始提交，所有文件都是新增的
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);

                while (treeWalk.next()) {
                    files.add(treeWalk.getPathString());
                }
            }
        } else {
            // 获取与父提交的差异
            RevCommit parent = commit.getParent(0);
            try (DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream())) {
                formatter.setRepository(repository);
                List<DiffEntry> diffs = formatter.scan(parent.getTree(), commit.getTree());

                for (DiffEntry diff : diffs) {
                    files.add(diff.getNewPath());
                }
            }
        }

        return files;
    }

    /**
     * 获取差异统计
     */
    private DiffSummary getDiffSummary(Repository repository, RevCommit commit)
            throws IOException {
        DiffSummary summary = new DiffSummary();
        int additions = 0;
        int deletions = 0;
        int modifications = 0;

        if (commit.getParentCount() > 0) {
            RevCommit parent = commit.getParent(0);
            try (DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream())) {
                formatter.setRepository(repository);
                List<DiffEntry> diffs = formatter.scan(parent.getTree(), commit.getTree());

                for (DiffEntry diff : diffs) {
                    switch (diff.getChangeType()) {
                        case ADD:
                            additions++;
                            break;
                        case DELETE:
                            deletions++;
                            break;
                        case MODIFY:
                            modifications++;
                            break;
                    }
                }
            }
        }

        summary.setAdditions(additions);
        summary.setDeletions(deletions);
        summary.setModifications(modifications);
        return summary;
    }

    /**
     * 提交详情
     */
    public static class CommitDetail {
        private CommitInfo commitInfo;
        private List<String> changedFiles;
        private DiffSummary diffSummary;

        // Getters and setters
        public CommitInfo getCommitInfo() { return commitInfo; }
        public void setCommitInfo(CommitInfo commitInfo) { this.commitInfo = commitInfo; }

        public List<String> getChangedFiles() { return changedFiles; }
        public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }

        public DiffSummary getDiffSummary() { return diffSummary; }
        public void setDiffSummary(DiffSummary diffSummary) { this.diffSummary = diffSummary; }
    }

    /**
     * 文件差异
     */
    public static class FileDiff {
        private String changeType;
        private String oldPath;
        private String newPath;

        // Getters and setters
        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }

        public String getOldPath() { return oldPath; }
        public void setOldPath(String oldPath) { this.oldPath = oldPath; }

        public String getNewPath() { return newPath; }
        public void setNewPath(String newPath) { this.newPath = newPath; }
    }

    /**
     * 差异统计
     */
    public static class DiffSummary {
        private int additions;
        private int deletions;
        private int modifications;

        // Getters and setters
        public int getAdditions() { return additions; }
        public void setAdditions(int additions) { this.additions = additions; }

        public int getDeletions() { return deletions; }
        public void setDeletions(int deletions) { this.deletions = deletions; }

        public int getModifications() { return modifications; }
        public void setModifications(int modifications) { this.modifications = modifications; }
    }
}