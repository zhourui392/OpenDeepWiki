package ai.opendw.koalawiki.domain.ai;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 服务文档库聚合
 *
 * @author zhourui(V33215020)
 * @since 2025/02/14
 */
@Data
public class ServiceDocumentLibrary {

    /**
     * 配置ID
     */
    private String id;

    /**
     * 仓库ID
     */
    private String warehouseId;

    /**
     * 所属领域ID
     */
    private String domainId;

    /**
     * 服务ID
     */
    private String serviceId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 服务文档内容
     */
    private String documentContent;

    /**
     * 默认文档类型
     */
    private String docType;

    /**
     * 默认提示词模板
     */
    private String promptTemplateId;

    /**
     * 默认 Agent 类型
     */
    private String agentType;

    /**
     * 源码匹配规则
     */
    private List<String> sourceGlobs;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 上次生成文档时的commit ID
     */
    private String lastCommitId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
