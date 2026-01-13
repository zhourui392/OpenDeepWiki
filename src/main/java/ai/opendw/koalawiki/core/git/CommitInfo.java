package ai.opendw.koalawiki.core.git;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Git提交信息
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitInfo {

    /**
     * 提交ID (SHA-1)
     */
    private String commitId;

    /**
     * 提交消息
     */
    private String message;

    /**
     * 作者
     */
    private String author;

    /**
     * 作者邮箱
     */
    private String authorEmail;

    /**
     * 提交时间
     */
    private Date commitTime;

    /**
     * 父提交ID列表
     */
    private List<String> parentIds;

    /**
     * 变更的文件列表（可选）
     */
    private List<String> changedFiles;

    /**
     * 新增行数（可选）
     */
    private Integer additions;

    /**
     * 删除行数（可选）
     */
    private Integer deletions;

    /**
     * 获取简短的提交ID（前7位）
     */
    public String getShortCommitId() {
        if (commitId != null && commitId.length() >= 7) {
            return commitId.substring(0, 7);
        }
        return commitId;
    }

    /**
     * 获取提交消息的第一行
     */
    public String getShortMessage() {
        if (message != null) {
            int newlineIndex = message.indexOf('\n');
            if (newlineIndex > 0) {
                return message.substring(0, newlineIndex);
            }
            return message;
        }
        return "";
    }
}