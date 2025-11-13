package ai.opendw.koalawiki.app.service.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索项
 * 表示单个搜索结果
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchItem {

    /**
     * 目录ID
     */
    private String catalogId;

    /**
     * 目录名称
     */
    private String name;

    /**
     * 高亮的名称（包含标记的关键词）
     */
    private String highlightedName;

    /**
     * 描述
     */
    private String description;

    /**
     * 高亮的描述
     */
    private String highlightedDescription;

    /**
     * URL
     */
    private String url;

    /**
     * 相关性得分
     */
    private Double relevanceScore;

    /**
     * 父级ID
     */
    private String parentId;

    /**
     * 仓库ID
     */
    private String warehouseId;
}
