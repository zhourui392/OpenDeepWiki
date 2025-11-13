package ai.opendw.koalawiki.app.service.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 目录搜索请求
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 搜索范围
     */
    private SearchScope scope;

    /**
     * 最大返回结果数
     */
    private Integer maxResults;
}
