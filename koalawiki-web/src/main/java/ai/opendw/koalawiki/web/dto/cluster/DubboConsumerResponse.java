package ai.opendw.koalawiki.web.dto.cluster;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Dubbo接口消费者响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
@Builder
public class DubboConsumerResponse {

    /**
     * ID
     */
    private String id;

    /**
     * 消费者仓库ID
     */
    private String consumerWarehouseId;

    /**
     * 消费者服务名
     */
    private String consumerServiceName;

    /**
     * 使用接口的类
     */
    private String sourceClass;

    /**
     * 注入的字段名
     */
    private String sourceField;

    /**
     * 创建时间
     */
    private Date createdAt;
}
