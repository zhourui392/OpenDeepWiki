package ai.opendw.koalawiki.web.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新目录请求DTO
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCatalogRequest {

    /**
     * 目录名称
     */
    private String name;

    /**
     * URL路径
     */
    private String url;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序顺序
     */
    private Integer order;

    /**
     * 是否完成
     */
    private Boolean isCompleted;

    /**
     * Prompt内容
     */
    private String prompt;
}
