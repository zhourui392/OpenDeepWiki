package ai.opendw.koalawiki.web.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 创建目录请求DTO
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCatalogRequest {

    /**
     * 目录名称
     */
    @NotBlank(message = "目录名称不能为空")
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
     * 父级ID
     */
    private String parentId;

    /**
     * 排序顺序
     */
    private Integer order;

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * Prompt内容
     */
    private String prompt;
}
