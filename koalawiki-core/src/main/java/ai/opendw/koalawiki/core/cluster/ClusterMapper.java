package ai.opendw.koalawiki.core.cluster;

import ai.opendw.koalawiki.domain.cluster.*;
import ai.opendw.koalawiki.infra.entity.cluster.*;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 集群相关实体转换器
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Component
public class ClusterMapper {

    /**
     * 将领域模型转换为JPA实体
     *
     * @param cluster 集群领域模型
     * @return JPA实体
     */
    public ServiceClusterEntity toEntity(ServiceCluster cluster) {
        if (cluster == null) {
            return null;
        }

        ServiceClusterEntity entity = new ServiceClusterEntity();
        entity.setId(cluster.getId());
        entity.setName(cluster.getName());
        entity.setCode(cluster.getCode());
        entity.setDescription(cluster.getDescription());
        entity.setTechStack(cluster.getTechStack());
        entity.setOwner(cluster.getOwner());
        entity.setStatus(cluster.getStatus());
        entity.setCreatedAt(cluster.getCreatedAt());
        entity.setUpdatedAt(cluster.getUpdatedAt());

        return entity;
    }

    /**
     * 将JPA实体转换为领域模型
     *
     * @param entity JPA实体
     * @return 集群领域模型
     */
    public ServiceCluster toDomain(ServiceClusterEntity entity) {
        if (entity == null) {
            return null;
        }

        ServiceCluster cluster = new ServiceCluster();
        cluster.setId(entity.getId());
        cluster.setName(entity.getName());
        cluster.setCode(entity.getCode());
        cluster.setDescription(entity.getDescription());
        cluster.setTechStack(entity.getTechStack());
        cluster.setOwner(entity.getOwner());
        cluster.setStatus(entity.getStatus());
        cluster.setCreatedAt(entity.getCreatedAt());
        cluster.setUpdatedAt(entity.getUpdatedAt());

        return cluster;
    }

    /**
     * 将领域模型转换为JPA实体
     *
     * @param domain 业务领域模型
     * @return JPA实体
     */
    public BusinessDomainEntity toEntity(BusinessDomain domain) {
        if (domain == null) {
            return null;
        }

        BusinessDomainEntity entity = new BusinessDomainEntity();
        entity.setId(domain.getId());
        entity.setClusterId(domain.getClusterId());
        entity.setName(domain.getName());
        entity.setCode(domain.getCode());
        entity.setDescription(domain.getDescription());
        entity.setOwner(domain.getOwner());
        entity.setColor(domain.getColor());
        entity.setSortOrder(domain.getSortOrder());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }

    /**
     * 将JPA实体转换为领域模型
     *
     * @param entity JPA实体
     * @return 业务领域模型
     */
    public BusinessDomain toDomain(BusinessDomainEntity entity) {
        if (entity == null) {
            return null;
        }

        BusinessDomain domain = new BusinessDomain();
        domain.setId(entity.getId());
        domain.setClusterId(entity.getClusterId());
        domain.setName(entity.getName());
        domain.setCode(entity.getCode());
        domain.setDescription(entity.getDescription());
        domain.setOwner(entity.getOwner());
        domain.setColor(entity.getColor());
        domain.setSortOrder(entity.getSortOrder());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());

        return domain;
    }

    /**
     * 将领域模型转换为JPA实体
     *
     * @param serviceInfo 服务信息
     * @return JPA实体
     */
    public ServiceDomainMappingEntity toEntity(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return null;
        }

        ServiceDomainMappingEntity entity = new ServiceDomainMappingEntity();
        entity.setId(serviceInfo.getId());
        entity.setDomainId(serviceInfo.getDomainId());
        entity.setWarehouseId(serviceInfo.getWarehouseId());
        entity.setServiceName(serviceInfo.getServiceName());
        entity.setServiceType(serviceInfo.getType());
        entity.setDescription(serviceInfo.getDescription());
        entity.setSortOrder(serviceInfo.getSortOrder());
        entity.setIsPrimary(serviceInfo.getIsPrimary());
        entity.setCreatedAt(serviceInfo.getCreatedAt());

        return entity;
    }

    /**
     * 将JPA实体转换为领域模型
     *
     * @param entity JPA实体
     * @return 服务信息
     */
    public ServiceInfo toDomain(ServiceDomainMappingEntity entity) {
        if (entity == null) {
            return null;
        }

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setId(entity.getId());
        serviceInfo.setDomainId(entity.getDomainId());
        serviceInfo.setWarehouseId(entity.getWarehouseId());
        serviceInfo.setServiceName(entity.getServiceName());
        serviceInfo.setType(entity.getServiceType());
        serviceInfo.setDescription(entity.getDescription());
        serviceInfo.setSortOrder(entity.getSortOrder());
        serviceInfo.setIsPrimary(entity.getIsPrimary());
        serviceInfo.setCreatedAt(entity.getCreatedAt());

        return serviceInfo;
    }

    /**
     * 将领域模型转换为JPA实体
     *
     * @param clusterWarehouse 集群仓库关联
     * @return JPA实体
     */
    public ClusterWarehouseEntity toEntity(ClusterWarehouse clusterWarehouse) {
        if (clusterWarehouse == null) {
            return null;
        }

        ClusterWarehouseEntity entity = new ClusterWarehouseEntity();
        entity.setId(clusterWarehouse.getId());
        entity.setClusterId(clusterWarehouse.getClusterId());
        entity.setWarehouseId(clusterWarehouse.getWarehouseId());
        entity.setSortOrder(clusterWarehouse.getSortOrder());
        entity.setCreatedAt(clusterWarehouse.getCreatedAt());

        return entity;
    }

    /**
     * 将JPA实体转换为领域模型
     *
     * @param entity JPA实体
     * @return 集群仓库关联
     */
    public ClusterWarehouse toDomain(ClusterWarehouseEntity entity) {
        if (entity == null) {
            return null;
        }

        ClusterWarehouse clusterWarehouse = new ClusterWarehouse();
        clusterWarehouse.setId(entity.getId());
        clusterWarehouse.setClusterId(entity.getClusterId());
        clusterWarehouse.setWarehouseId(entity.getWarehouseId());
        clusterWarehouse.setSortOrder(entity.getSortOrder());
        clusterWarehouse.setCreatedAt(entity.getCreatedAt());

        return clusterWarehouse;
    }

    /**
     * 将领域模型转换为JPA实体
     *
     * @param graph 依赖图
     * @return JPA实体
     */
    public ServiceDependencyGraphEntity toEntity(ServiceDependencyGraph graph) {
        if (graph == null) {
            return null;
        }

        ServiceDependencyGraphEntity entity = new ServiceDependencyGraphEntity();
        entity.setId(graph.getId());
        entity.setClusterId(graph.getClusterId());
        entity.setGraphType(graph.getGraphType());
        entity.setScopeId(graph.getScopeId());
        entity.setGraphData(graph.getGraphData());
        entity.setNodeCount(graph.getNodeCount());
        entity.setEdgeCount(graph.getEdgeCount());
        entity.setMermaidCode(graph.getMermaidCode());
        entity.setGeneratedAt(graph.getGeneratedAt());
        entity.setExpiresAt(graph.getExpiresAt());
        entity.setCreatedAt(graph.getCreatedAt());

        return entity;
    }

    /**
     * 将JPA实体转换为领域模型
     *
     * @param entity JPA实体
     * @return 依赖图
     */
    public ServiceDependencyGraph toDomain(ServiceDependencyGraphEntity entity) {
        if (entity == null) {
            return null;
        }

        ServiceDependencyGraph graph = new ServiceDependencyGraph();
        graph.setId(entity.getId());
        graph.setClusterId(entity.getClusterId());
        graph.setGraphType(entity.getGraphType());
        graph.setScopeId(entity.getScopeId());
        graph.setGraphData(entity.getGraphData());
        graph.setNodeCount(entity.getNodeCount());
        graph.setEdgeCount(entity.getEdgeCount());
        graph.setMermaidCode(entity.getMermaidCode());
        graph.setGeneratedAt(entity.getGeneratedAt());
        graph.setExpiresAt(entity.getExpiresAt());
        graph.setCreatedAt(entity.getCreatedAt());

        return graph;
    }
}
