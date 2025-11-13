package ai.opendw.koalawiki.domain.document;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 文档目录多语言支持实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentCatalogI18n extends BaseEntity {

    /**
     * 关联的文档目录ID
     */
    private String documentCatalogId = "";

    /**
     * 语言代码 (如: zh-CN, en-US)
     */
    private String languageCode = "";

    /**
     * 多语言目录名称
     */
    private String name = "";

    /**
     * 多语言目录描述
     */
    private String description = "";

    /**
     * 关联的DocumentCatalog（导航属性）
     */
    private DocumentCatalog documentCatalog;
}
