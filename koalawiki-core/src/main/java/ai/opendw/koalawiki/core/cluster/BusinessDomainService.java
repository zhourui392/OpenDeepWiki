package ai.opendw.koalawiki.core.cluster;

import ai.opendw.koalawiki.domain.cluster.BusinessDomain;
import ai.opendw.koalawiki.domain.cluster.ServiceInfo;
import ai.opendw.koalawiki.domain.cluster.ServiceType;
import ai.opendw.koalawiki.infra.entity.cluster.BusinessDomainEntity;
import ai.opendw.koalawiki.infra.entity.cluster.ServiceDomainMappingEntity;
import ai.opendw.koalawiki.infra.repository.cluster.BusinessDomainRepository;
import ai.opendw.koalawiki.infra.repository.cluster.ServiceClusterRepository;
import ai.opendw.koalawiki.infra.repository.cluster.ServiceDomainMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 业务领域管理服务
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessDomainService {

    private final BusinessDomainRepository domainRepository;
    private final ServiceDomainMappingRepository mappingRepository;
    private final ServiceClusterRepository clusterRepository;
    private final ClusterMapper mapper;

    /**
     * 创建业务领域
     *
     * @param domain 领域信息
     * @return 创建的领域
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessDomain createDomain(BusinessDomain domain) {
        log.info("创建业务领域: clusterId={}, name={}, code={}",
                domain.getClusterId(), domain.getName(), domain.getCode());

        validateClusterExists(domain.getClusterId());
        validateDomainCode(domain.getClusterId(), domain.getCode());

        domain.setId(UUID.randomUUID().toString());
        domain.setCreatedAt(new Date());
        domain.setUpdatedAt(new Date());

        BusinessDomainEntity entity = mapper.toEntity(domain);
        domainRepository.save(entity);

        log.info("业务领域创建成功: id={}", domain.getId());
        return domain;
    }

    /**
     * 更新业务领域
     *
     * @param domain 领域信息
     * @return 更新后的领域
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessDomain updateDomain(BusinessDomain domain) {
        log.info("更新业务领域: id={}", domain.getId());

        BusinessDomainEntity existing = findEntityById(domain.getId());

        if (!existing.getCode().equals(domain.getCode())) {
            validateDomainCode(existing.getClusterId(), domain.getCode());
        }

        existing.setName(domain.getName());
        existing.setCode(domain.getCode());
        existing.setDescription(domain.getDescription());
        existing.setOwner(domain.getOwner());
        existing.setColor(domain.getColor());
        existing.setSortOrder(domain.getSortOrder());
        existing.setUpdatedAt(new Date());

        domainRepository.save(existing);

        log.info("业务领域更新成功: id={}", domain.getId());
        return mapper.toDomain(existing);
    }

    /**
     * 删除业务领域（软删除）
     *
     * @param domainId 领域ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDomain(String domainId) {
        log.info("删除业务领域: id={}", domainId);

        BusinessDomainEntity entity = findEntityById(domainId);

        if ("unclassified".equals(entity.getCode())) {
            throw new IllegalArgumentException("未分类领域不能删除");
        }

        entity.setDeletedAt(new Date());
        domainRepository.save(entity);

        log.info("业务领域删除成功: id={}", domainId);
    }

    /**
     * 根据ID查询领域
     *
     * @param domainId 领域ID
     * @return 领域信息
     */
    public Optional<BusinessDomain> getById(String domainId) {
        return domainRepository.findByIdAndDeletedAtIsNull(domainId)
                .map(this::enrichDomain);
    }

    /**
     * 根据集群ID和编码查询领域
     *
     * @param clusterId 集群ID
     * @param code      领域编码
     * @return 领域信息
     */
    public Optional<BusinessDomain> getByClusterIdAndCode(String clusterId, String code) {
        return domainRepository.findByClusterIdAndCodeAndDeletedAtIsNull(clusterId, code)
                .map(this::enrichDomain);
    }

    /**
     * 查询集群下的所有领域
     *
     * @param clusterId 集群ID
     * @return 领域列表
     */
    public List<BusinessDomain> listByClusterId(String clusterId) {
        return domainRepository.findByClusterIdAndDeletedAtIsNullOrderBySortOrderAsc(clusterId)
                .stream()
                .map(this::enrichDomain)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询集群下的领域
     *
     * @param clusterId 集群ID
     * @param pageable  分页参数
     * @return 领域分页
     */
    public Page<BusinessDomain> listByClusterId(String clusterId, Pageable pageable) {
        return domainRepository.findByClusterIdAndDeletedAtIsNullOrderBySortOrderAsc(clusterId, pageable)
                .map(this::enrichDomain);
    }

    /**
     * 添加服务到领域
     *
     * @param domainId    领域ID
     * @param warehouseId 仓库ID
     * @param serviceName 服务名称
     * @param serviceType 服务类型
     * @param isPrimary   是否主领域
     */
    @Transactional(rollbackFor = Exception.class)
    public void addServiceToDomain(String domainId, String warehouseId, String serviceName,
                                    ServiceType serviceType, boolean isPrimary) {
        log.info("添加服务到领域: domainId={}, warehouseId={}", domainId, warehouseId);

        findEntityById(domainId);

        if (mappingRepository.existsByDomainIdAndWarehouseId(domainId, warehouseId)) {
            log.warn("服务已存在于领域中: domainId={}, warehouseId={}", domainId, warehouseId);
            return;
        }

        if (isPrimary) {
            mappingRepository.clearPrimaryByWarehouseId(warehouseId);
        }

        ServiceDomainMappingEntity entity = new ServiceDomainMappingEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setDomainId(domainId);
        entity.setWarehouseId(warehouseId);
        entity.setServiceName(serviceName);
        entity.setServiceType(serviceType != null ? serviceType : ServiceType.PROVIDER);
        entity.setIsPrimary(isPrimary);
        entity.setSortOrder(0);
        entity.setCreatedAt(new Date());

        mappingRepository.save(entity);

        log.info("服务添加到领域成功: domainId={}, warehouseId={}", domainId, warehouseId);
    }

    /**
     * 从领域移除服务
     *
     * @param domainId    领域ID
     * @param warehouseId 仓库ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeServiceFromDomain(String domainId, String warehouseId) {
        log.info("从领域移除服务: domainId={}, warehouseId={}", domainId, warehouseId);

        mappingRepository.findByDomainIdAndWarehouseId(domainId, warehouseId)
                .ifPresent(mappingRepository::delete);

        log.info("从领域移除服务成功: domainId={}, warehouseId={}", domainId, warehouseId);
    }

    /**
     * 获取领域内的服务列表
     *
     * @param domainId 领域ID
     * @return 服务列表
     */
    public List<ServiceInfo> getServicesInDomain(String domainId) {
        return mappingRepository.findByDomainIdOrderBySortOrderAsc(domainId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 获取仓库所属的领域列表
     *
     * @param warehouseId 仓库ID
     * @return 领域列表
     */
    public List<BusinessDomain> getDomainsForWarehouse(String warehouseId) {
        List<ServiceDomainMappingEntity> mappings = mappingRepository.findByWarehouseId(warehouseId);

        return mappings.stream()
                .map(m -> domainRepository.findByIdAndDeletedAtIsNull(m.getDomainId()).orElse(null))
                .filter(d -> d != null)
                .map(this::enrichDomain)
                .collect(Collectors.toList());
    }

    /**
     * 获取仓库的主领域
     *
     * @param warehouseId 仓库ID
     * @return 主领域
     */
    public Optional<BusinessDomain> getPrimaryDomainForWarehouse(String warehouseId) {
        return mappingRepository.findByWarehouseIdAndIsPrimaryTrue(warehouseId)
                .flatMap(m -> domainRepository.findByIdAndDeletedAtIsNull(m.getDomainId()))
                .map(this::enrichDomain);
    }

    /**
     * 获取未分类领域
     *
     * @param clusterId 集群ID
     * @return 未分类领域
     */
    public Optional<BusinessDomain> getUnclassifiedDomain(String clusterId) {
        return domainRepository.findUnclassifiedDomain(clusterId)
                .map(this::enrichDomain);
    }

    /**
     * 验证集群存在
     */
    private void validateClusterExists(String clusterId) {
        if (!clusterRepository.findByIdAndDeletedAtIsNull(clusterId).isPresent()) {
            throw new IllegalArgumentException("集群不存在: " + clusterId);
        }
    }

    /**
     * 验证领域编码唯一性
     */
    private void validateDomainCode(String clusterId, String code) {
        if (domainRepository.existsByClusterIdAndCodeAndDeletedAtIsNull(clusterId, code)) {
            throw new IllegalArgumentException("领域编码已存在: " + code);
        }
    }

    /**
     * 根据ID查找实体
     */
    private BusinessDomainEntity findEntityById(String domainId) {
        return domainRepository.findByIdAndDeletedAtIsNull(domainId)
                .orElseThrow(() -> new IllegalArgumentException("领域不存在: " + domainId));
    }

    /**
     * 丰富领域信息
     */
    private BusinessDomain enrichDomain(BusinessDomainEntity entity) {
        BusinessDomain domain = mapper.toDomain(entity);

        List<ServiceInfo> services = mappingRepository.findByDomainIdOrderBySortOrderAsc(entity.getId())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        domain.setServices(services);

        return domain;
    }
}
