package ai.opendw.koalawiki.domain.document;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档文件项源文件实体
 * 记录文档内容来源的源文件信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentFileItemSource extends BaseEntity {

    /**
     * 所属文档文件项ID
     */
    private String documentFileItemId = "";

    /**
     * 源文件地址
     */
    private String address = "";

    /**
     * 源文件名称
     */
    private String name;

    /**
     * 关联的DocumentFileItem（导航属性）
     */
    private DocumentFileItem documentFileItem;
}
