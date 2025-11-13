package ai.opendw.koalawiki.web.dto.catalog;

import ai.opendw.koalawiki.app.service.catalog.SearchScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 目录搜索请求DTO
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogSearchRequest {

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /**
     * 搜索范围
     */
    private SearchScope scope;

    /**
     * 最大结果数
     */
    private Integer maxResults;
}
