package ai.opendw.koalawiki.domain.document;

import ai.opendw.koalawiki.domain.BaseEntity;
import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 文档实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Document extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 关联仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * 最后更新时间
     */
    private Date lastUpdate;

    /**
     * 文档介绍
     */
    @Size(max = 2000, message = "文档介绍长度不能超过2000字符")
    private String description;

    /**
     * 点赞数
     */
    private Long likeCount = 0L;

    /**
     * 评论数
     */
    private Long commentCount = 0L;

    /**
     * 本地git仓库地址
     */
    @Size(max = 500, message = "Git路径长度不能超过500字符")
    private String gitPath;

    /**
     * 仓库状态
     */
    private WarehouseStatus status;
}
