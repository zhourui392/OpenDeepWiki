package ai.opendw.koalawiki.infra.entity.cluster;

import ai.opendw.koalawiki.infra.entity.BaseJpaEntity;

import javax.persistence.*;

/**
 * Dubbo接口消费者JPA实体
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Entity
@Table(name = "dubbo_interface_consumer",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_consumer", columnNames = {"interface_registry_id", "consumer_warehouse_id", "source_class"})
    },
    indexes = {
        @Index(name = "idx_interface", columnList = "interface_registry_id"),
        @Index(name = "idx_consumer", columnList = "consumer_warehouse_id")
    })
public class DubboInterfaceConsumerEntity extends BaseJpaEntity {

    /**
     * 接口注册ID
     */
    @Column(name = "interface_registry_id", nullable = false, length = 36)
    private String interfaceRegistryId;

    /**
     * 消费者仓库ID
     */
    @Column(name = "consumer_warehouse_id", nullable = false, length = 36)
    private String consumerWarehouseId;

    /**
     * 消费者服务名
     */
    @Column(name = "consumer_service_name", length = 128)
    private String consumerServiceName;

    /**
     * 使用该接口的类
     */
    @Column(name = "source_class", length = 255)
    private String sourceClass;

    /**
     * 注入的字段名
     */
    @Column(name = "source_field", length = 64)
    private String sourceField;

    public String getInterfaceRegistryId() {
        return interfaceRegistryId;
    }

    public void setInterfaceRegistryId(String interfaceRegistryId) {
        this.interfaceRegistryId = interfaceRegistryId;
    }

    public String getConsumerWarehouseId() {
        return consumerWarehouseId;
    }

    public void setConsumerWarehouseId(String consumerWarehouseId) {
        this.consumerWarehouseId = consumerWarehouseId;
    }

    public String getConsumerServiceName() {
        return consumerServiceName;
    }

    public void setConsumerServiceName(String consumerServiceName) {
        this.consumerServiceName = consumerServiceName;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public void setSourceClass(String sourceClass) {
        this.sourceClass = sourceClass;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }
}
