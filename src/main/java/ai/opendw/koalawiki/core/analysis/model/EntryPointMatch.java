package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 入口点匹配结果
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-23
 */
@Data
public class EntryPointMatch {

    /**
     * 匹配的入口点
     */
    private EntryPoint entryPoint;

    /**
     * 相关度分数
     */
    private int relevanceScore;

    /**
     * 匹配原因列表
     */
    private List<String> matchReasons = new ArrayList<>();

    /**
     * 所属项目
     */
    private String projectName;

    /**
     * 添加匹配原因
     */
    public void addMatchReason(String reason) {
        this.matchReasons.add(reason);
    }
}
