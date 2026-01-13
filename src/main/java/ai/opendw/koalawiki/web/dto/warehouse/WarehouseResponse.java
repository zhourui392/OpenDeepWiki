package ai.opendw.koalawiki.web.dto.warehouse;

import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import lombok.Data;

import java.util.Date;

/**
 * 仓库响应DTO
 */
@Data
public class WarehouseResponse {

    /**
     * 仓库ID
     */
    private String id;

    /**
     * 仓库名称
     */
    private String name;

    /**
     * 组织名称
     */
    private String organizationName;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * 仓库地址
     */
    private String address;

    /**
     * 分支名称
     */
    private String branch;

    /**
     * 仓库状态
     */
    private WarehouseStatus status;

    /**
     * 错误信息
     */
    private String error;

    /**
     * Star数量
     */
    private Integer stars;

    /**
     * Fork数量
     */
    private Integer forks;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 版本号
     */
    private String version;

    /**
     * 是否嵌入完成
     */
    private Boolean isEmbedded;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;
}
