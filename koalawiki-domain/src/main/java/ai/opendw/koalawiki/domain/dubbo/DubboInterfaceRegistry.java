package ai.opendw.koalawiki.domain.dubbo;

import ai.opendw.koalawiki.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Dubbo接口注册表
 * 记录所有Dubbo接口及其提供者信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DubboInterfaceRegistry extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 所属集群ID
     */
    @NotBlank(message = "集群ID不能为空")
    private String clusterId;

    /**
     * 接口全限定名
     */
    @NotBlank(message = "接口名不能为空")
    @Size(max = 255, message = "接口名长度不能超过255字符")
    private String interfaceName;

    /**
     * 接口版本
     */
    @Size(max = 32, message = "版本号长度不能超过32字符")
    private String version = "";

    /**
     * 接口分组
     */
    @Size(max = 64, message = "分组名长度不能超过64字符")
    private String groupName = "";

    /**
     * 提供者仓库ID
     */
    private String providerWarehouseId;

    /**
     * 提供者服务名
     */
    @Size(max = 128, message = "服务名长度不能超过128字符")
    private String providerServiceName;

    /**
     * 接口描述（从Javadoc提取）
     */
    private String description;

    /**
     * 接口方法列表
     */
    private List<DubboMethodInfo> methods = new ArrayList<>();

    /**
     * 是否已废弃
     */
    private Boolean deprecated = false;

    /**
     * 废弃说明
     */
    @Size(max = 500, message = "废弃说明长度不能超过500字符")
    private String deprecatedReason;

    /**
     * 源文件路径
     */
    @Size(max = 500, message = "源文件路径长度不能超过500字符")
    private String sourceFile;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 消费者服务ID列表
     */
    private List<String> consumerServiceIds = new ArrayList<>();

    /**
     * 添加方法
     *
     * @param method 方法信息
     */
    public void addMethod(DubboMethodInfo method) {
        if (method != null) {
            methods.add(method);
        }
    }

    /**
     * 添加消费者
     *
     * @param consumerServiceId 消费者服务ID
     */
    public void addConsumer(String consumerServiceId) {
        if (consumerServiceId != null && !consumerServiceIds.contains(consumerServiceId)) {
            consumerServiceIds.add(consumerServiceId);
        }
    }

    /**
     * 获取方法数量
     *
     * @return 方法数量
     */
    public int getMethodCount() {
        return methods.size();
    }

    /**
     * 获取消费者数量
     *
     * @return 消费者数量
     */
    public int getConsumerCount() {
        return consumerServiceIds.size();
    }

    /**
     * 获取接口唯一标识
     *
     * @return 接口唯一标识
     */
    public String getUniqueKey() {
        return String.format("%s:%s:%s", interfaceName, version, groupName);
    }

    /**
     * 获取简短接口名（不含包名）
     *
     * @return 简短接口名
     */
    public String getSimpleName() {
        if (interfaceName == null || interfaceName.isEmpty()) {
            return "";
        }
        int lastDot = interfaceName.lastIndexOf('.');
        return lastDot >= 0 ? interfaceName.substring(lastDot + 1) : interfaceName;
    }
}
