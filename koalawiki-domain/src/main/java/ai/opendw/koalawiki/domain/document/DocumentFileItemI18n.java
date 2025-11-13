package ai.opendw.koalawiki.domain.document;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 文档文件多语言支持实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentFileItemI18n extends BaseEntity {

    /**
     * 关联的文档文件ID
     */
    private String documentFileItemId = "";

    /**
     * 语言代码 (如: zh-CN, en-US)
     */
    private String languageCode = "";

    /**
     * 多语言标题
     */
    private String title = "";

    /**
     * 多语言描述
     */
    private String description = "";

    /**
     * 多语言文档内容
     */
    private String content = "";

    /**
     * 关联的DocumentFileItem（导航属性）
     */
    private DocumentFileItem documentFileItem;
}
