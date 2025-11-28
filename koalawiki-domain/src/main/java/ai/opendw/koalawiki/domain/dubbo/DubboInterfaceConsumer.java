package ai.opendw.koalawiki.domain.dubbo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * Dubbo接口消费者信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class DubboInterfaceConsumer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private String id;

    /**
     * 接口注册ID
     */
    @NotBlank(message = "接口注册ID不能为空")
    private String interfaceRegistryId;

    /**
     * 消费者仓库ID
     */
    @NotBlank(message = "消费者仓库ID不能为空")
    private String consumerWarehouseId;

    /**
     * 消费者服务名
     */
    @Size(max = 128, message = "服务名长度不能超过128字符")
    private String consumerServiceName;

    /**
     * 使用该接口的类
     */
    @Size(max = 255, message = "类名长度不能超过255字符")
    private String sourceClass;

    /**
     * 注入的字段名
     */
    @Size(max = 64, message = "字段名长度不能超过64字符")
    private String sourceField;

    /**
     * 创建时间
     */
    private Date createdAt;
}
