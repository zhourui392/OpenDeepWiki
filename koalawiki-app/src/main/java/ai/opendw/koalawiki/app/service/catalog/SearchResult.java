package ai.opendw.koalawiki.app.service.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索结果
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /**
     * 搜索项列表
     */
    private List<SearchItem> items;

    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 搜索建议
     */
    private String suggestion;
}
