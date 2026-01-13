package ai.opendw.koalawiki.domain.ai;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 领域信息
 *
 * @author zhourui(V33215020)
 * @since 2025/01/13
 */
@Data
public class DomainInfo {

    /**
     * 领域ID
     */
    private String id;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 领域名称
     */
    private String name;

    /**
     * 领域描述
     */
    private String description;

    /**
     * 领域代码，用于Git路径
     */
    private String code;

    /**
     * 领域文档内容
     */
    private String documentContent;

    /**
     * 领域下的服务列表
     */
    private List<ServiceDocumentLibrary> services;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
