package ai.opendw.koalawiki.core.cluster;

import ai.opendw.koalawiki.domain.cluster.BusinessDomain;
import ai.opendw.koalawiki.domain.cluster.ClusterStatus;
import ai.opendw.koalawiki.domain.cluster.ServiceCluster;
import ai.opendw.koalawiki.infra.entity.cluster.BusinessDomainEntity;
import ai.opendw.koalawiki.infra.entity.cluster.ClusterWarehouseEntity;
import ai.opendw.koalawiki.infra.entity.cluster.ServiceClusterEntity;
import ai.opendw.koalawiki.infra.repository.cluster.BusinessDomainRepository;
import ai.opendw.koalawiki.infra.repository.cluster.ClusterWarehouseRepository;
import ai.opendw.koalawiki.infra.repository.cluster.ServiceClusterRepository;
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
 * 服务集群管理服务
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceClusterService {

    private final ServiceClusterRepository clusterRepository;
    private final ClusterWarehouseRepository clusterWarehouseRepository;
    private final BusinessDomainRepository domainRepository;
    private final ClusterMapper mapper;

    /**
     * 创建集群
     *
     * @param cluster 集群信息
     * @return 创建的集群
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceCluster createCluster(ServiceCluster cluster) {
        log.info("创建服务集群: name={}, code={}", cluster.getName(), cluster.getCode());

        validateClusterCode(cluster.getCode());

        cluster.setId(UUID.randomUUID().toString());
        cluster.setCreatedAt(new Date());
        cluster.setUpdatedAt(new Date());

        if (cluster.getStatus() == null) {
            cluster.setStatus(ClusterStatus.ACTIVE);
        }

        ServiceClusterEntity entity = mapper.toEntity(cluster);
        clusterRepository.save(entity);

        createDefaultDomain(cluster.getId());

        log.info("服务集群创建成功: id={}", cluster.getId());
        return cluster;
    }

    /**
     * 更新集群
     *
     * @param cluster 集群信息
     * @return 更新后的集群
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceCluster updateCluster(ServiceCluster cluster) {
        log.info("更新服务集群: id={}", cluster.getId());

        ServiceClusterEntity existing = findEntityById(cluster.getId());

        if (!existing.getCode().equals(cluster.getCode())) {
            validateClusterCode(cluster.getCode());
        }

        existing.setName(cluster.getName());
        existing.setCode(cluster.getCode());
        existing.setDescription(cluster.getDescription());
        existing.setTechStack(cluster.getTechStack());
        existing.setOwner(cluster.getOwner());
        existing.setStatus(cluster.getStatus());
        existing.setUpdatedAt(new Date());

        clusterRepository.save(existing);

        log.info("服务集群更新成功: id={}", cluster.getId());
        return mapper.toDomain(existing);
    }

    /**
     * 删除集群（软删除）
     *
     * @param clusterId 集群ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCluster(String clusterId) {
        log.info("删除服务集群: id={}", clusterId);

        ServiceClusterEntity entity = findEntityById(clusterId);
        entity.setDeletedAt(new Date());
        clusterRepository.save(entity);

        log.info("服务集群删除成功: id={}", clusterId);
    }

    /**
     * 根据ID查询集群
     *
     * @param clusterId 集群ID
     * @return 集群信息
     */
    public Optional<ServiceCluster> getById(String clusterId) {
        return clusterRepository.findByIdAndDeletedAtIsNull(clusterId)
                .map(this::enrichCluster);
    }

    /**
     * 根据编码查询集群
     *
     * @param code 集群编码
     * @return 集群信息
     */
    public Optional<ServiceCluster> getByCode(String code) {
        return clusterRepository.findByCodeAndDeletedAtIsNull(code)
                .map(this::enrichCluster);
    }

    /**
     * 分页查询集群列表
     *
     * @param pageable 分页参数
     * @return 集群分页
     */
    public Page<ServiceCluster> listClusters(Pageable pageable) {
        return clusterRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageable)
                .map(this::enrichCluster);
    }

    /**
     * 根据状态查询集群列表
     *
     * @param status   状态
     * @param pageable 分页参数
     * @return 集群分页
     */
    public Page<ServiceCluster> listClustersByStatus(ClusterStatus status, Pageable pageable) {
        return clusterRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status, pageable)
                .map(this::enrichCluster);
    }

    /**
     * 查询所有活跃集群
     *
     * @return 集群列表
     */
    public List<ServiceCluster> listActiveClusters() {
        return clusterRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(ClusterStatus.ACTIVE)
                .stream()
                .map(this::enrichCluster)
                .collect(Collectors.toList());
    }

    /**
     * 添加仓库到集群
     *
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     * @param sortOrder   排序权重
     */
    @Transactional(rollbackFor = Exception.class)
    public void addWarehouseToCluster(String clusterId, String warehouseId, Integer sortOrder) {
        log.info("添加仓库到集群: clusterId={}, warehouseId={}", clusterId, warehouseId);

        findEntityById(clusterId);

        if (clusterWarehouseRepository.existsByClusterIdAndWarehouseId(clusterId, warehouseId)) {
            log.warn("仓库已存在于集群中: clusterId={}, warehouseId={}", clusterId, warehouseId);
            return;
        }

        ClusterWarehouseEntity entity = new ClusterWarehouseEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setClusterId(clusterId);
        entity.setWarehouseId(warehouseId);
        entity.setSortOrder(sortOrder != null ? sortOrder : 0);
        entity.setCreatedAt(new Date());

        clusterWarehouseRepository.save(entity);

        log.info("仓库添加到集群成功: clusterId={}, warehouseId={}", clusterId, warehouseId);
    }

    /**
     * 从集群移除仓库
     *
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeWarehouseFromCluster(String clusterId, String warehouseId) {
        log.info("从集群移除仓库: clusterId={}, warehouseId={}", clusterId, warehouseId);

        clusterWarehouseRepository.findByClusterIdAndWarehouseId(clusterId, warehouseId)
                .ifPresent(clusterWarehouseRepository::delete);

        log.info("从集群移除仓库成功: clusterId={}, warehouseId={}", clusterId, warehouseId);
    }

    /**
     * 获取集群内的仓库ID列表
     *
     * @param clusterId 集群ID
     * @return 仓库ID列表
     */
    public List<String> getWarehouseIds(String clusterId) {
        return clusterWarehouseRepository.findWarehouseIdsByClusterId(clusterId);
    }

    /**
     * 获取集群统计信息
     *
     * @param clusterId 集群ID
     * @return 统计信息
     */
    public ClusterStatistics getStatistics(String clusterId) {
        long warehouseCount = clusterRepository.countWarehousesByClusterId(clusterId);
        long domainCount = clusterRepository.countDomainsByClusterId(clusterId);

        return new ClusterStatistics(clusterId, warehouseCount, domainCount);
    }

    /**
     * 验证集群编码唯一性
     */
    private void validateClusterCode(String code) {
        if (clusterRepository.existsByCodeAndDeletedAtIsNull(code)) {
            throw new IllegalArgumentException("集群编码已存在: " + code);
        }
    }

    /**
     * 根据ID查找实体
     */
    private ServiceClusterEntity findEntityById(String clusterId) {
        return clusterRepository.findByIdAndDeletedAtIsNull(clusterId)
                .orElseThrow(() -> new IllegalArgumentException("集群不存在: " + clusterId));
    }

    /**
     * 创建默认未分类领域
     */
    private void createDefaultDomain(String clusterId) {
        BusinessDomainEntity domain = new BusinessDomainEntity();
        domain.setId(UUID.randomUUID().toString());
        domain.setClusterId(clusterId);
        domain.setName("未分类");
        domain.setCode("unclassified");
        domain.setDescription("未分类的服务");
        domain.setOwner("system");
        domain.setColor("#8c8c8c");
        domain.setSortOrder(999);
        domain.setCreatedAt(new Date());

        domainRepository.save(domain);
    }

    /**
     * 丰富集群信息
     */
    private ServiceCluster enrichCluster(ServiceClusterEntity entity) {
        ServiceCluster cluster = mapper.toDomain(entity);

        List<String> warehouseIds = clusterWarehouseRepository.findWarehouseIdsByClusterId(entity.getId());
        cluster.setWarehouseIds(warehouseIds);

        List<BusinessDomain> domains = domainRepository.findByClusterIdAndDeletedAtIsNullOrderBySortOrderAsc(entity.getId())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        cluster.setDomains(domains);

        return cluster;
    }

    /**
     * 集群统计信息
     */
    public static class ClusterStatistics {
        private final String clusterId;
        private final long warehouseCount;
        private final long domainCount;

        public ClusterStatistics(String clusterId, long warehouseCount, long domainCount) {
            this.clusterId = clusterId;
            this.warehouseCount = warehouseCount;
            this.domainCount = domainCount;
        }

        public String getClusterId() {
            return clusterId;
        }

        public long getWarehouseCount() {
            return warehouseCount;
        }

        public long getDomainCount() {
            return domainCount;
        }
    }
}
