package ai.opendw.koalawiki.app.service.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 目录过滤条件
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {

    /**
     * 包含的文件类型（如：.md, .pdf）
     */
    private List<String> includeTypes;

    /**
     * 排除的文件类型
     */
    private List<String> excludeTypes;

    /**
     * 搜索关键词
     */
    private String searchKeyword;

    /**
     * 最大深度（限制树的层级）
     */
    private Integer maxDepth;

    /**
     * 是否只显示已完成的目录
     */
    private Boolean completedOnly;

    /**
     * 是否包含已删除的目录
     */
    private Boolean includeDeleted;

    /**
     * 父级ID过滤
     */
    private String parentId;

    /**
     * 最小order值
     */
    private Integer minOrder;

    /**
     * 最大order值
     */
    private Integer maxOrder;
}
