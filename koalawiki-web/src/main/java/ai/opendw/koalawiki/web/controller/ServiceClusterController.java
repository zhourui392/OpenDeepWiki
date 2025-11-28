package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.core.cluster.ServiceClusterService;
import ai.opendw.koalawiki.domain.cluster.BusinessDomain;
import ai.opendw.koalawiki.domain.cluster.ClusterStatus;
import ai.opendw.koalawiki.domain.cluster.ServiceCluster;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.cluster.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务集群管理控制器
 * 提供集群CRUD和仓库关联相关的REST API
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clusters")
@RequiredArgsConstructor
@Validated
public class ServiceClusterController {

    private final ServiceClusterService clusterService;

    /**
     * 创建集群
     *
     * @param request 创建请求
     * @return 创建的集群
     */
    @PostMapping
    public ResponseEntity<Result<ClusterResponse>> createCluster(
            @Valid @RequestBody CreateClusterRequest request) {

        log.info("创建服务集群: name={}, code={}", request.getName(), request.getCode());

        try {
            ServiceCluster cluster = new ServiceCluster();
            cluster.setName(request.getName());
            cluster.setCode(request.getCode());
            cluster.setDescription(request.getDescription());
            cluster.setTechStack(request.getTechStack());
            cluster.setOwner(request.getOwner());

            ServiceCluster created = clusterService.createCluster(cluster);
            ClusterResponse response = convertToResponse(created);

            return ResponseEntity.ok(Result.success(response, "集群创建成功"));

        } catch (IllegalArgumentException e) {
            log.warn("创建集群参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("创建集群失败", e);
            return ResponseEntity.ok(Result.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取集群列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param status   状态过滤
     * @return 集群列表
     */
    @GetMapping
    public ResponseEntity<Result<ClusterListResponse>> listClusters(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(required = false) ClusterStatus status) {

        log.debug("获取集群列表: page={}, pageSize={}, status={}", page, pageSize, status);

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<ServiceCluster> clusters;

            if (status != null) {
                clusters = clusterService.listClustersByStatus(status, pageable);
            } else {
                clusters = clusterService.listClusters(pageable);
            }

            List<ClusterResponse> items = clusters.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            ClusterListResponse response = ClusterListResponse.builder()
                    .items(items)
                    .total(clusters.getTotalElements())
                    .page(page)
                    .pageSize(pageSize)
                    .totalPages(clusters.getTotalPages())
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取集群列表失败", e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取集群详情
     *
     * @param clusterId 集群ID
     * @return 集群详情
     */
    @GetMapping("/{clusterId}")
    public ResponseEntity<Result<ClusterResponse>> getCluster(@PathVariable String clusterId) {
        log.debug("获取集群详情: clusterId={}", clusterId);

        try {
            return clusterService.getById(clusterId)
                    .map(cluster -> ResponseEntity.ok(Result.success(convertToResponse(cluster))))
                    .orElse(ResponseEntity.ok(Result.error(404, "集群不存在")));

        } catch (Exception e) {
            log.error("获取集群详情失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据编码获取集群
     *
     * @param code 集群编码
     * @return 集群详情
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Result<ClusterResponse>> getClusterByCode(@PathVariable String code) {
        log.debug("根据编码获取集群: code={}", code);

        try {
            return clusterService.getByCode(code)
                    .map(cluster -> ResponseEntity.ok(Result.success(convertToResponse(cluster))))
                    .orElse(ResponseEntity.ok(Result.error(404, "集群不存在")));

        } catch (Exception e) {
            log.error("根据编码获取集群失败: code={}", code, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 更新集群
     *
     * @param clusterId 集群ID
     * @param request   更新请求
     * @return 更新后的集群
     */
    @PutMapping("/{clusterId}")
    public ResponseEntity<Result<ClusterResponse>> updateCluster(
            @PathVariable String clusterId,
            @Valid @RequestBody UpdateClusterRequest request) {

        log.info("更新服务集群: clusterId={}", clusterId);

        try {
            ServiceCluster cluster = new ServiceCluster();
            cluster.setId(clusterId);
            cluster.setName(request.getName());
            cluster.setCode(request.getCode());
            cluster.setDescription(request.getDescription());
            cluster.setTechStack(request.getTechStack());
            cluster.setOwner(request.getOwner());
            cluster.setStatus(request.getStatus());

            ServiceCluster updated = clusterService.updateCluster(cluster);
            ClusterResponse response = convertToResponse(updated);

            return ResponseEntity.ok(Result.success(response, "集群更新成功"));

        } catch (IllegalArgumentException e) {
            log.warn("更新集群参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("更新集群失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 删除集群
     *
     * @param clusterId 集群ID
     * @return 删除结果
     */
    @DeleteMapping("/{clusterId}")
    public ResponseEntity<Result<Void>> deleteCluster(@PathVariable String clusterId) {
        log.info("删除服务集群: clusterId={}", clusterId);

        try {
            clusterService.deleteCluster(clusterId);
            return ResponseEntity.ok(Result.success(null, "集群删除成功"));

        } catch (IllegalArgumentException e) {
            log.warn("删除集群参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("删除集群失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 添加仓库到集群
     *
     * @param clusterId 集群ID
     * @param request   添加请求
     * @return 操作结果
     */
    @PostMapping("/{clusterId}/warehouses")
    public ResponseEntity<Result<Void>> addWarehouseToCluster(
            @PathVariable String clusterId,
            @Valid @RequestBody AddWarehouseRequest request) {

        log.info("添加仓库到集群: clusterId={}, warehouseId={}", clusterId, request.getWarehouseId());

        try {
            clusterService.addWarehouseToCluster(clusterId, request.getWarehouseId(), request.getSortOrder());
            return ResponseEntity.ok(Result.success(null, "仓库添加成功"));

        } catch (IllegalArgumentException e) {
            log.warn("添加仓库参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("添加仓库到集群失败", e);
            return ResponseEntity.ok(Result.error("添加失败: " + e.getMessage()));
        }
    }

    /**
     * 从集群移除仓库
     *
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     * @return 操作结果
     */
    @DeleteMapping("/{clusterId}/warehouses/{warehouseId}")
    public ResponseEntity<Result<Void>> removeWarehouseFromCluster(
            @PathVariable String clusterId,
            @PathVariable String warehouseId) {

        log.info("从集群移除仓库: clusterId={}, warehouseId={}", clusterId, warehouseId);

        try {
            clusterService.removeWarehouseFromCluster(clusterId, warehouseId);
            return ResponseEntity.ok(Result.success(null, "仓库移除成功"));

        } catch (Exception e) {
            log.error("从集群移除仓库失败", e);
            return ResponseEntity.ok(Result.error("移除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取集群内仓库ID列表
     *
     * @param clusterId 集群ID
     * @return 仓库ID列表
     */
    @GetMapping("/{clusterId}/warehouses")
    public ResponseEntity<Result<List<String>>> getWarehouseIds(@PathVariable String clusterId) {
        log.debug("获取集群内仓库ID列表: clusterId={}", clusterId);

        try {
            List<String> warehouseIds = clusterService.getWarehouseIds(clusterId);
            return ResponseEntity.ok(Result.success(warehouseIds));

        } catch (Exception e) {
            log.error("获取集群仓库列表失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取集群统计信息
     *
     * @param clusterId 集群ID
     * @return 统计信息
     */
    @GetMapping("/{clusterId}/statistics")
    public ResponseEntity<Result<ClusterStatisticsResponse>> getStatistics(@PathVariable String clusterId) {
        log.debug("获取集群统计信息: clusterId={}", clusterId);

        try {
            ServiceClusterService.ClusterStatistics stats = clusterService.getStatistics(clusterId);

            ClusterStatisticsResponse response = ClusterStatisticsResponse.builder()
                    .clusterId(stats.getClusterId())
                    .warehouseCount(stats.getWarehouseCount())
                    .domainCount(stats.getDomainCount())
                    .interfaceCount(0)
                    .documentCount(0)
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取集群统计信息失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有活跃集群
     *
     * @return 活跃集群列表
     */
    @GetMapping("/active")
    public ResponseEntity<Result<List<ClusterResponse>>> getActiveClusters() {
        log.debug("获取所有活跃集群");

        try {
            List<ClusterResponse> items = clusterService.listActiveClusters().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(items));

        } catch (Exception e) {
            log.error("获取活跃集群列表失败", e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 转换为响应DTO
     */
    private ClusterResponse convertToResponse(ServiceCluster cluster) {
        List<DomainBriefResponse> domainResponses = null;

        if (cluster.getDomains() != null) {
            domainResponses = cluster.getDomains().stream()
                    .map(this::convertToBriefResponse)
                    .collect(Collectors.toList());
        }

        return ClusterResponse.builder()
                .id(cluster.getId())
                .name(cluster.getName())
                .code(cluster.getCode())
                .description(cluster.getDescription())
                .techStack(cluster.getTechStack())
                .owner(cluster.getOwner())
                .status(cluster.getStatus())
                .warehouseCount(cluster.getWarehouseCount())
                .domainCount(cluster.getDomainCount())
                .domains(domainResponses)
                .createdAt(cluster.getCreatedAt())
                .updatedAt(cluster.getUpdatedAt())
                .build();
    }

    /**
     * 转换为领域简要响应
     */
    private DomainBriefResponse convertToBriefResponse(BusinessDomain domain) {
        return DomainBriefResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .code(domain.getCode())
                .color(domain.getColor())
                .serviceCount(domain.getServiceCount())
                .build();
    }
}
