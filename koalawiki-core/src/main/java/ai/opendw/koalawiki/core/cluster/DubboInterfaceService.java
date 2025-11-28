package ai.opendw.koalawiki.core.cluster;

import ai.opendw.koalawiki.core.analysis.dubbo.DubboInterfaceScanner;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceConsumer;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceRegistry;
import ai.opendw.koalawiki.domain.dubbo.DubboMethodInfo;
import ai.opendw.koalawiki.infra.entity.cluster.DubboInterfaceConsumerEntity;
import ai.opendw.koalawiki.infra.entity.cluster.DubboInterfaceRegistryEntity;
import ai.opendw.koalawiki.infra.repository.cluster.DubboInterfaceConsumerRepository;
import ai.opendw.koalawiki.infra.repository.cluster.DubboInterfaceRegistryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dubbo接口服务
 * 管理Dubbo接口注册表和消费者关系
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DubboInterfaceService {

    private final DubboInterfaceRegistryRepository registryRepository;
    private final DubboInterfaceConsumerRepository consumerRepository;
    private final DubboInterfaceScanner interfaceScanner;
    private final ObjectMapper objectMapper;

    /**
     * 扫描并更新仓库的Dubbo接口
     *
     * @param repoPath    仓库路径
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     * @param serviceName 服务名
     * @return 扫描结果
     */
    @Transactional(rollbackFor = Exception.class)
    public DubboInterfaceScanner.ScanResult scanAndUpdate(
            Path repoPath, String clusterId, String warehouseId, String serviceName) {
        log.info("开始扫描并更新Dubbo接口: warehouseId={}, serviceName={}", warehouseId, serviceName);

        DubboInterfaceScanner.ScanResult result = interfaceScanner.scanRepository(
                repoPath, clusterId, warehouseId, serviceName);

        saveProviderInterfaces(result.getInterfaceRegistries());

        saveConsumerRelations(clusterId, result.getInterfaceConsumers());

        log.info("Dubbo接口更新完成: warehouseId={}, 提供接口数={}, 消费接口数={}",
                warehouseId, result.getProviderCount(), result.getConsumerCount());

        return result;
    }

    /**
     * 保存提供者接口
     */
    private void saveProviderInterfaces(List<DubboInterfaceRegistry> registries) {
        for (DubboInterfaceRegistry registry : registries) {
            Optional<DubboInterfaceRegistryEntity> existingOpt = registryRepository
                    .findByClusterIdAndInterfaceNameAndVersionAndGroupName(
                            registry.getClusterId(),
                            registry.getInterfaceName(),
                            registry.getVersion() != null ? registry.getVersion() : "",
                            registry.getGroupName() != null ? registry.getGroupName() : ""
                    );

            DubboInterfaceRegistryEntity entity;
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                updateRegistryEntity(entity, registry);
            } else {
                entity = createRegistryEntity(registry);
            }

            registryRepository.save(entity);
            registry.setId(entity.getId());
        }
    }

    /**
     * 保存消费者关系
     */
    private void saveConsumerRelations(String clusterId, List<DubboInterfaceConsumer> consumers) {
        for (DubboInterfaceConsumer consumer : consumers) {
            String interfaceRegistryId = findInterfaceRegistryId(clusterId, consumer);
            if (interfaceRegistryId == null) {
                log.debug("未找到接口注册信息，跳过消费者: class={}, field={}",
                        consumer.getSourceClass(), consumer.getSourceField());
                continue;
            }

            consumer.setInterfaceRegistryId(interfaceRegistryId);

            Optional<DubboInterfaceConsumerEntity> existingOpt = consumerRepository
                    .findByInterfaceRegistryIdAndConsumerWarehouseIdAndSourceClass(
                            interfaceRegistryId,
                            consumer.getConsumerWarehouseId(),
                            consumer.getSourceClass()
                    );

            if (!existingOpt.isPresent()) {
                DubboInterfaceConsumerEntity entity = createConsumerEntity(consumer);
                consumerRepository.save(entity);
            }
        }
    }

    /**
     * 查找接口注册ID
     */
    private String findInterfaceRegistryId(String clusterId, DubboInterfaceConsumer consumer) {
        return null;
    }

    /**
     * 根据ID获取接口详情
     *
     * @param interfaceId 接口ID
     * @return 接口信息
     */
    public Optional<DubboInterfaceRegistry> getById(String interfaceId) {
        return registryRepository.findById(interfaceId)
                .map(this::toRegistry);
    }

    /**
     * 根据接口名获取接口详情
     *
     * @param clusterId     集群ID
     * @param interfaceName 接口名
     * @param version       版本
     * @param group         分组
     * @return 接口信息
     */
    public Optional<DubboInterfaceRegistry> getByName(
            String clusterId, String interfaceName, String version, String group) {
        return registryRepository.findByClusterIdAndInterfaceNameAndVersionAndGroupName(
                        clusterId, interfaceName, version != null ? version : "", group != null ? group : "")
                .map(this::toRegistry);
    }

    /**
     * 分页查询集群接口列表
     *
     * @param clusterId 集群ID
     * @param pageable  分页参数
     * @return 接口分页
     */
    public Page<DubboInterfaceRegistry> listByCluster(String clusterId, Pageable pageable) {
        return registryRepository.findByClusterIdOrderByInterfaceNameAsc(clusterId, pageable)
                .map(this::toRegistry);
    }

    /**
     * 搜索接口
     *
     * @param clusterId 集群ID
     * @param keyword   关键词
     * @param pageable  分页参数
     * @return 接口分页
     */
    public Page<DubboInterfaceRegistry> search(String clusterId, String keyword, Pageable pageable) {
        return registryRepository.searchByKeyword(clusterId, keyword, pageable)
                .map(this::toRegistry);
    }

    /**
     * 根据仓库ID查询提供的接口
     *
     * @param providerWarehouseId 提供者仓库ID
     * @return 接口列表
     */
    public List<DubboInterfaceRegistry> listByProvider(String providerWarehouseId) {
        return registryRepository.findByProviderWarehouseId(providerWarehouseId)
                .stream()
                .map(this::toRegistry)
                .collect(Collectors.toList());
    }

    /**
     * 获取热门接口
     *
     * @param clusterId 集群ID
     * @param limit     数量限制
     * @return 接口列表
     */
    public List<DubboInterfaceRegistry> getTopInterfaces(String clusterId, int limit) {
        return registryRepository.findTopInterfacesByConsumerCount(clusterId, PageRequest.of(0, limit))
                .stream()
                .map(this::toRegistry)
                .collect(Collectors.toList());
    }

    /**
     * 获取接口的消费者列表
     *
     * @param interfaceId 接口ID
     * @return 消费者列表
     */
    public List<DubboInterfaceConsumer> getConsumers(String interfaceId) {
        return consumerRepository.findByInterfaceRegistryId(interfaceId)
                .stream()
                .map(this::toConsumer)
                .collect(Collectors.toList());
    }

    /**
     * 获取仓库消费的接口列表
     *
     * @param consumerWarehouseId 消费者仓库ID
     * @return 接口列表
     */
    public List<DubboInterfaceRegistry> getConsumedInterfaces(String consumerWarehouseId) {
        List<String> interfaceIds = consumerRepository.findInterfaceIdsByConsumerWarehouseId(consumerWarehouseId);
        return interfaceIds.stream()
                .map(id -> registryRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(this::toRegistry)
                .collect(Collectors.toList());
    }

    /**
     * 统计集群接口数量
     *
     * @param clusterId 集群ID
     * @return 数量
     */
    public long countByCluster(String clusterId) {
        return registryRepository.countByClusterId(clusterId);
    }

    /**
     * 统计仓库提供的接口数量
     *
     * @param providerWarehouseId 提供者仓库ID
     * @return 数量
     */
    public long countByProvider(String providerWarehouseId) {
        return registryRepository.countByProviderWarehouseId(providerWarehouseId);
    }

    /**
     * 统计接口的消费者数量
     *
     * @param interfaceId 接口ID
     * @return 数量
     */
    public long countConsumers(String interfaceId) {
        return consumerRepository.countByInterfaceRegistryId(interfaceId);
    }

    /**
     * 刷新集群接口注册表
     *
     * @param clusterId 集群ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshClusterInterfaces(String clusterId) {
        log.info("刷新集群接口注册表: clusterId={}", clusterId);
    }

    /**
     * 删除仓库的所有接口信息
     *
     * @param warehouseId 仓库ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByWarehouse(String warehouseId) {
        log.info("删除仓库的Dubbo接口信息: warehouseId={}", warehouseId);

        consumerRepository.deleteByConsumerWarehouseId(warehouseId);

        List<DubboInterfaceRegistryEntity> interfaces = registryRepository.findByProviderWarehouseId(warehouseId);
        for (DubboInterfaceRegistryEntity entity : interfaces) {
            consumerRepository.deleteByInterfaceRegistryId(entity.getId());
            registryRepository.delete(entity);
        }

        log.info("删除仓库Dubbo接口信息完成: warehouseId={}", warehouseId);
    }

    /**
     * 创建注册实体
     */
    private DubboInterfaceRegistryEntity createRegistryEntity(DubboInterfaceRegistry registry) {
        DubboInterfaceRegistryEntity entity = new DubboInterfaceRegistryEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setClusterId(registry.getClusterId());
        entity.setInterfaceName(registry.getInterfaceName());
        entity.setVersion(registry.getVersion() != null ? registry.getVersion() : "");
        entity.setGroupName(registry.getGroupName() != null ? registry.getGroupName() : "");
        entity.setProviderWarehouseId(registry.getProviderWarehouseId());
        entity.setProviderServiceName(registry.getProviderServiceName());
        entity.setDescription(registry.getDescription());
        entity.setMethods(serializeMethods(registry.getMethods()));
        entity.setDeprecated(registry.getDeprecated() != null && registry.getDeprecated());
        entity.setDeprecatedReason(registry.getDeprecatedReason());
        entity.setSourceFile(registry.getSourceFile());
        entity.setUpdatedAt(new Date());
        return entity;
    }

    /**
     * 更新注册实体
     */
    private void updateRegistryEntity(DubboInterfaceRegistryEntity entity, DubboInterfaceRegistry registry) {
        entity.setProviderWarehouseId(registry.getProviderWarehouseId());
        entity.setProviderServiceName(registry.getProviderServiceName());
        entity.setDescription(registry.getDescription());
        entity.setMethods(serializeMethods(registry.getMethods()));
        entity.setDeprecated(registry.getDeprecated() != null && registry.getDeprecated());
        entity.setDeprecatedReason(registry.getDeprecatedReason());
        entity.setSourceFile(registry.getSourceFile());
        entity.setUpdatedAt(new Date());
    }

    /**
     * 创建消费者实体
     */
    private DubboInterfaceConsumerEntity createConsumerEntity(DubboInterfaceConsumer consumer) {
        DubboInterfaceConsumerEntity entity = new DubboInterfaceConsumerEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setInterfaceRegistryId(consumer.getInterfaceRegistryId());
        entity.setConsumerWarehouseId(consumer.getConsumerWarehouseId());
        entity.setConsumerServiceName(consumer.getConsumerServiceName());
        entity.setSourceClass(consumer.getSourceClass());
        entity.setSourceField(consumer.getSourceField());
        return entity;
    }

    /**
     * 转换为领域对象
     */
    private DubboInterfaceRegistry toRegistry(DubboInterfaceRegistryEntity entity) {
        DubboInterfaceRegistry registry = new DubboInterfaceRegistry();
        registry.setId(entity.getId());
        registry.setClusterId(entity.getClusterId());
        registry.setInterfaceName(entity.getInterfaceName());
        registry.setVersion(entity.getVersion());
        registry.setGroupName(entity.getGroupName());
        registry.setProviderWarehouseId(entity.getProviderWarehouseId());
        registry.setProviderServiceName(entity.getProviderServiceName());
        registry.setDescription(entity.getDescription());
        registry.setMethods(deserializeMethods(entity.getMethods()));
        registry.setDeprecated(entity.getDeprecated());
        registry.setDeprecatedReason(entity.getDeprecatedReason());
        registry.setSourceFile(entity.getSourceFile());
        registry.setCreatedAt(entity.getCreatedAt());
        registry.setUpdatedAt(entity.getUpdatedAt());

        List<String> consumerIds = consumerRepository.findConsumerWarehouseIdsByInterfaceRegistryId(entity.getId());
        registry.setConsumerServiceIds(consumerIds);

        return registry;
    }

    /**
     * 转换消费者为领域对象
     */
    private DubboInterfaceConsumer toConsumer(DubboInterfaceConsumerEntity entity) {
        DubboInterfaceConsumer consumer = new DubboInterfaceConsumer();
        consumer.setId(entity.getId());
        consumer.setInterfaceRegistryId(entity.getInterfaceRegistryId());
        consumer.setConsumerWarehouseId(entity.getConsumerWarehouseId());
        consumer.setConsumerServiceName(entity.getConsumerServiceName());
        consumer.setSourceClass(entity.getSourceClass());
        consumer.setSourceField(entity.getSourceField());
        consumer.setCreatedAt(entity.getCreatedAt());
        return consumer;
    }

    /**
     * 序列化方法列表
     */
    private String serializeMethods(List<DubboMethodInfo> methods) {
        if (methods == null || methods.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(methods);
        } catch (JsonProcessingException e) {
            log.warn("序列化方法列表失败", e);
            return "[]";
        }
    }

    /**
     * 反序列化方法列表
     */
    private List<DubboMethodInfo> deserializeMethods(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DubboMethodInfo>>() {});
        } catch (JsonProcessingException e) {
            log.warn("反序列化方法列表失败", e);
            return new ArrayList<>();
        }
    }
}
