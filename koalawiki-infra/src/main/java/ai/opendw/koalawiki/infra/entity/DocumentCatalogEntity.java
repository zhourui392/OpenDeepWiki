package ai.opendw.koalawiki.infra.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;

/**
 * 文档目录JPA实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "document_catalog")
public class DocumentCatalogEntity extends BaseJpaEntity {

    /**
     * 目录名称
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * URL路径
     */
    @Column(name = "url")
    private String url;

    /**
     * 目录描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 父级目录ID
     */
    @Column(name = "parent_id")
    private String parentId;

    /**
     * 排序顺序
     */
    @Column(name = "order_num")
    private Integer order = 0;

    /**
     * 所属文档ID
     */
    @Column(name = "document_id")
    private String documentId;

    /**
     * 所属仓库ID
     */
    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    /**
     * 是否处理完成
     */
    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    /**
     * Prompt内容
     */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /**
     * 是否已删除
     */
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    /**
     * 删除时间
     */
    @Column(name = "deleted_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deletedTime;
}