package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 文档文件项JPA实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document_file_item")
public class DocumentFileItemEntity extends BaseJpaEntity {

    /**
     * 标题
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * 描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 文档实际内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 评论数量
     */
    @Column(name = "comment_count")
    private Long commentCount = 0L;

    /**
     * 文档大小（字节）
     */
    @Column(name = "size")
    private Long size = 0L;

    /**
     * 绑定的目录ID
     */
    @Column(name = "document_catalog_id", nullable = false)
    private String documentCatalogId;

    /**
     * 请求token消耗
     */
    @Column(name = "request_token")
    private Integer requestToken = 0;

    /**
     * 响应token消耗
     */
    @Column(name = "response_token")
    private Integer responseToken = 0;

    /**
     * 是否嵌入完成
     */
    @Column(name = "is_embedded")
    private Boolean isEmbedded = false;

    /**
     * 元数据（JSON存储）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 扩展数据（JSON存储）
     */
    @Column(name = "extra", columnDefinition = "TEXT")
    private String extra;
}