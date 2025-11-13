package ai.opendw.koalawiki.domain.document;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档文件项实体
 * 代表文档的具体内容
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentFileItem extends BaseEntity {

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 文档实际内容
     */
    private String content = "";

    /**
     * 评论数量
     */
    private Long commentCount = 0L;

    /**
     * 文档大小（字节）
     */
    private Long size = 0L;

    /**
     * 绑定的目录ID
     */
    private String documentCatalogId;

    /**
     * 请求token消耗
     */
    private Integer requestToken = 0;

    /**
     * 响应token消耗
     */
    private Integer responseToken = 0;

    /**
     * 是否嵌入完成（用于向量搜索）
     */
    private Boolean isEmbedded = false;

    /**
     * 相关源文件列表
     */
    private List<DocumentFileItemSource> sources;

    /**
     * 元数据
     */
    private Map<String, String> metadata = new HashMap<>();

    /**
     * 扩展数据
     */
    private Map<String, String> extra = new HashMap<>();

    /**
     * 多语言翻译列表
     */
    private List<DocumentFileItemI18n> i18nTranslations;
}
