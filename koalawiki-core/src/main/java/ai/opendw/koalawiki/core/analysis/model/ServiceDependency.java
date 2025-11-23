package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

/**
 * 服务依赖关系
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Data
public class ServiceDependency {

    /**
     * 调用方服务名
     */
    private String sourceService;

    /**
     * 被调用方服务名
     */
    private String targetService;

    /**
     * 接口全限定名
     */
    private String interfaceName;

    /**
     * 依赖类型
     */
    private DependencyType type;

    /**
     * 调用方类名
     */
    private String sourceClass;

    /**
     * 调用方字段名（用于Dubbo/Feign）
     */
    private String sourceField;
}
