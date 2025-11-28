package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.core.cluster.BusinessDomainService;
import ai.opendw.koalawiki.domain.cluster.BusinessDomain;
import ai.opendw.koalawiki.domain.cluster.ServiceInfo;
import ai.opendw.koalawiki.domain.cluster.ServiceType;
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
 * 业务领域管理控制器
 * 提供领域CRUD和服务关联相关的REST API
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clusters/{clusterId}/domains")
@RequiredArgsConstructor
@Validated
public class BusinessDomainController {

    private final BusinessDomainService domainService;

    /**
     * 创建领域
     *
     * @param clusterId 集群ID
     * @param request   创建请求
     * @return 创建的领域
     */
    @PostMapping
    public ResponseEntity<Result<DomainResponse>> createDomain(
            @PathVariable String clusterId,
            @Valid @RequestBody CreateDomainRequest request) {

        log.info("创建业务领域: clusterId={}, name={}, code={}",
                clusterId, request.getName(), request.getCode());

        try {
            BusinessDomain domain = new BusinessDomain();
            domain.setClusterId(clusterId);
            domain.setName(request.getName());
            domain.setCode(request.getCode());
            domain.setDescription(request.getDescription());
            domain.setOwner(request.getOwner());
            domain.setColor(request.getColor() != null ? request.getColor() : "#1890ff");
            domain.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

            BusinessDomain created = domainService.createDomain(domain);
            DomainResponse response = convertToResponse(created);

            return ResponseEntity.ok(Result.success(response, "领域创建成功"));

        } catch (IllegalArgumentException e) {
            log.warn("创建领域参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("创建领域失败", e);
            return ResponseEntity.ok(Result.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取集群下的领域列表
     *
     * @param clusterId 集群ID
     * @param page      页码
     * @param pageSize  每页大小
     * @return 领域列表
     */
    @GetMapping
    public ResponseEntity<Result<DomainListResponse>> listDomains(
            @PathVariable String clusterId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize) {

        log.debug("获取领域列表: clusterId={}, page={}, pageSize={}", clusterId, page, pageSize);

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<BusinessDomain> domains = domainService.listByClusterId(clusterId, pageable);

            List<DomainResponse> items = domains.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            DomainListResponse response = DomainListResponse.builder()
                    .items(items)
                    .total(domains.getTotalElements())
                    .page(page)
                    .pageSize(pageSize)
                    .totalPages(domains.getTotalPages())
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取领域列表失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取集群下所有领域（不分页）
     *
     * @param clusterId 集群ID
     * @return 领域列表
     */
    @GetMapping("/all")
    public ResponseEntity<Result<List<DomainResponse>>> getAllDomains(@PathVariable String clusterId) {
        log.debug("获取所有领域: clusterId={}", clusterId);

        try {
            List<DomainResponse> items = domainService.listByClusterId(clusterId).stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(items));

        } catch (Exception e) {
            log.error("获取所有领域失败: clusterId={}", clusterId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取领域详情
     *
     * @param clusterId 集群ID
     * @param domainId  领域ID
     * @return 领域详情
     */
    @GetMapping("/{domainId}")
    public ResponseEntity<Result<DomainResponse>> getDomain(
            @PathVariable String clusterId,
            @PathVariable String domainId) {

        log.debug("获取领域详情: clusterId={}, domainId={}", clusterId, domainId);

        try {
            return domainService.getById(domainId)
                    .filter(d -> clusterId.equals(d.getClusterId()))
                    .map(domain -> ResponseEntity.ok(Result.success(convertToResponse(domain))))
                    .orElse(ResponseEntity.ok(Result.error(404, "领域不存在")));

        } catch (Exception e) {
            log.error("获取领域详情失败: domainId={}", domainId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据编码获取领域
     *
     * @param clusterId 集群ID
     * @param code      领域编码
     * @return 领域详情
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Result<DomainResponse>> getDomainByCode(
            @PathVariable String clusterId,
            @PathVariable String code) {

        log.debug("根据编码获取领域: clusterId={}, code={}", clusterId, code);

        try {
            return domainService.getByClusterIdAndCode(clusterId, code)
                    .map(domain -> ResponseEntity.ok(Result.success(convertToResponse(domain))))
                    .orElse(ResponseEntity.ok(Result.error(404, "领域不存在")));

        } catch (Exception e) {
            log.error("根据编码获取领域失败: code={}", code, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 更新领域
     *
     * @param clusterId 集群ID
     * @param domainId  领域ID
     * @param request   更新请求
     * @return 更新后的领域
     */
    @PutMapping("/{domainId}")
    public ResponseEntity<Result<DomainResponse>> updateDomain(
            @PathVariable String clusterId,
            @PathVariable String domainId,
            @Valid @RequestBody UpdateDomainRequest request) {

        log.info("更新业务领域: clusterId={}, domainId={}", clusterId, domainId);

        try {
            BusinessDomain domain = new BusinessDomain();
            domain.setId(domainId);
            domain.setClusterId(clusterId);
            domain.setName(request.getName());
            domain.setCode(request.getCode());
            domain.setDescription(request.getDescription());
            domain.setOwner(request.getOwner());
            domain.setColor(request.getColor());
            domain.setSortOrder(request.getSortOrder());

            BusinessDomain updated = domainService.updateDomain(domain);
            DomainResponse response = convertToResponse(updated);

            return ResponseEntity.ok(Result.success(response, "领域更新成功"));

        } catch (IllegalArgumentException e) {
            log.warn("更新领域参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("更新领域失败: domainId={}", domainId, e);
            return ResponseEntity.ok(Result.error("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 删除领域
     *
     * @param clusterId 集群ID
     * @param domainId  领域ID
     * @return 删除结果
     */
    @DeleteMapping("/{domainId}")
    public ResponseEntity<Result<Void>> deleteDomain(
            @PathVariable String clusterId,
            @PathVariable String domainId) {

        log.info("删除业务领域: clusterId={}, domainId={}", clusterId, domainId);

        try {
            domainService.deleteDomain(domainId);
            return ResponseEntity.ok(Result.success(null, "领域删除成功"));

        } catch (IllegalArgumentException e) {
            log.warn("删除领域参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("删除领域失败: domainId={}", domainId, e);
            return ResponseEntity.ok(Result.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 添加服务到领域
     *
     * @param clusterId 集群ID
     * @param domainId  领域ID
     * @param request   添加请求
     * @return 操作结果
     */
    @PostMapping("/{domainId}/services")
    public ResponseEntity<Result<Void>> addServiceToDomain(
            @PathVariable String clusterId,
            @PathVariable String domainId,
            @Valid @RequestBody AddServiceRequest request) {

        log.info("添加服务到领域: domainId={}, warehouseId={}", domainId, request.getWarehouseId());

        try {
            ServiceType serviceType = request.getServiceType() != null
                    ? request.getServiceType()
                    : ServiceType.PROVIDER;
            boolean isPrimary = request.getIsPrimary() != null && request.getIsPrimary();

            domainService.addServiceToDomain(
                    domainId,
                    request.getWarehouseId(),
                    request.getServiceName(),
                    serviceType,
                    isPrimary);

            return ResponseEntity.ok(Result.success(null, "服务添加成功"));

        } catch (IllegalArgumentException e) {
            log.warn("添加服务参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("添加服务到领域失败", e);
            return ResponseEntity.ok(Result.error("添加失败: " + e.getMessage()));
        }
    }

    /**
     * 从领域移除服务
     *
     * @param clusterId   集群ID
     * @param domainId    领域ID
     * @param warehouseId 仓库ID
     * @return 操作结果
     */
    @DeleteMapping("/{domainId}/services/{warehouseId}")
    public ResponseEntity<Result<Void>> removeServiceFromDomain(
            @PathVariable String clusterId,
            @PathVariable String domainId,
            @PathVariable String warehouseId) {

        log.info("从领域移除服务: domainId={}, warehouseId={}", domainId, warehouseId);

        try {
            domainService.removeServiceFromDomain(domainId, warehouseId);
            return ResponseEntity.ok(Result.success(null, "服务移除成功"));

        } catch (Exception e) {
            log.error("从领域移除服务失败", e);
            return ResponseEntity.ok(Result.error("移除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取领域内的服务列表
     *
     * @param clusterId 集群ID
     * @param domainId  领域ID
     * @return 服务列表
     */
    @GetMapping("/{domainId}/services")
    public ResponseEntity<Result<List<DomainResponse.ServiceInfoResponse>>> getServicesInDomain(
            @PathVariable String clusterId,
            @PathVariable String domainId) {

        log.debug("获取领域内服务列表: domainId={}", domainId);

        try {
            List<ServiceInfo> services = domainService.getServicesInDomain(domainId);
            List<DomainResponse.ServiceInfoResponse> items = services.stream()
                    .map(this::convertToServiceResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(items));

        } catch (Exception e) {
            log.error("获取领域服务列表失败: domainId={}", domainId, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 转换为响应DTO
     */
    private DomainResponse convertToResponse(BusinessDomain domain) {
        List<DomainResponse.ServiceInfoResponse> serviceResponses = null;

        if (domain.getServices() != null) {
            serviceResponses = domain.getServices().stream()
                    .map(this::convertToServiceResponse)
                    .collect(Collectors.toList());
        }

        return DomainResponse.builder()
                .id(domain.getId())
                .clusterId(domain.getClusterId())
                .name(domain.getName())
                .code(domain.getCode())
                .description(domain.getDescription())
                .owner(domain.getOwner())
                .color(domain.getColor())
                .sortOrder(domain.getSortOrder())
                .serviceCount(domain.getServiceCount())
                .services(serviceResponses)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * 转换为服务信息响应
     */
    private DomainResponse.ServiceInfoResponse convertToServiceResponse(ServiceInfo serviceInfo) {
        return DomainResponse.ServiceInfoResponse.builder()
                .id(serviceInfo.getId())
                .warehouseId(serviceInfo.getWarehouseId())
                .serviceName(serviceInfo.getServiceName())
                .type(serviceInfo.getType())
                .description(serviceInfo.getDescription())
                .isPrimary(serviceInfo.getIsPrimary())
                .sortOrder(serviceInfo.getSortOrder())
                .build();
    }
}
