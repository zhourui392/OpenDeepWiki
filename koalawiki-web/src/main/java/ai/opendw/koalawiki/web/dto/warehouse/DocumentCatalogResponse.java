package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.Data;

/**
 * 文档目录响应DTO
 */
@Data
public class DocumentCatalogResponse {

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
     * 目录描述
     */
    private String description;

    /**
     * 父级目录ID
     */
    private String parentId;

    /**
     * 排序顺序
     */
    private Integer order;

    /**
     * 所属仓库ID
     */
    private String warehouseId;

    /**
     * 是否处理完成
     */
    private Boolean isCompleted;
}
