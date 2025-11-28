package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Dubbo接口响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class DubboInterfaceResponse {

    /**
     * 接口ID
     */
    private String id;

    /**
     * 接口全限定名
     */
    private String interfaceName;

    /**
     * 简短接口名
     */
    private String simpleName;

    /**
     * 接口版本
     */
    private String version;

    /**
     * 接口分组
     */
    private String groupName;

    /**
     * 提供者仓库ID
     */
    private String providerWarehouseId;

    /**
     * 提供者服务名
     */
    private String providerServiceName;

    /**
     * 接口描述
     */
    private String description;

    /**
     * 方法列表
     */
    private List<DubboMethodResponse> methods;

    /**
     * 是否废弃
     */
    private Boolean deprecated;

    /**
     * 废弃原因
     */
    private String deprecatedReason;

    /**
     * 源文件路径
     */
    private String sourceFile;

    /**
     * 方法数量
     */
    private Integer methodCount;

    /**
     * 消费者数量
     */
    private Integer consumerCount;

    /**
     * 消费者服务ID列表
     */
    private List<String> consumerServiceIds;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
