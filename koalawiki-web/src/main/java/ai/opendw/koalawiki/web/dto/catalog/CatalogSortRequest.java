package ai.opendw.koalawiki.web.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 目录排序请求DTO
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogSortRequest {

    /**
     * 排序策略: alphabetical, priority, custom
     */
    @NotBlank(message = "排序策略不能为空")
    private String strategy;
}
