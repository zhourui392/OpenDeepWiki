package ai.opendw.koalawiki.web.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 目录响应DTO
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogResponse {

    /**
     * 目录ID
     */
    private String id;

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
    private String warehouseId;

    /**
     * 是否完成
     */
    private Boolean isCompleted;

    /**
     * 是否删除
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 删除时间
     */
    private Date deletedTime;
}
