package ai.opendw.koalawiki.domain.document;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 文档提交记录实体
 * 记录文档相关的Git提交信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentCommitRecord extends BaseEntity {

    /**
     * 所属仓库ID
     */
    private String warehouseId = "";

    /**
     * Git提交ID（SHA）
     */
    private String commitId = "";

    /**
     * 提交消息
     */
    private String commitMessage = "";

    /**
     * 标题
     */
    private String title = "";

    /**
     * 作者
     */
    private String author = "";

    /**
     * 最后更新时间
     */
    private Date lastUpdate = new Date();
}
