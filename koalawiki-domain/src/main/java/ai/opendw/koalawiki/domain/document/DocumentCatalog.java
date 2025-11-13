package ai.opendw.koalawiki.domain.document;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 文档目录实体
 * 用于组织文档的层级结构
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentCatalog extends BaseEntity {

    /**
     * 目录名称
     */
    private String name = "";

    /**
     * URL路径
     */
    private String url = "";

    /**
     * 目录描述
     */
    private String description = "";

    /**
     * 父级目录ID
     */
    private String parentId;

    /**
     * 排序顺序
     */
    private Integer order = 0;

    /**
     * 所属文档ID
     */
    private String documentId = "";

    /**
     * 所属仓库ID
     */
    private String warehouseId = "";

    /**
     * 是否处理完成
     */
    private Boolean isCompleted = false;

    /**
     * Prompt内容
     */
    private String prompt = "";

    /**
     * 是否已删除
     */
    private Boolean isDeleted = false;

    /**
     * 删除时间
     */
    private Date deletedTime;

    /**
     * 多语言翻译列表
     */
    private List<DocumentCatalogI18n> i18nTranslations;
}
