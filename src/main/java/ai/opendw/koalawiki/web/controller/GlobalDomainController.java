package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.ai.DomainDocumentService;
import ai.opendw.koalawiki.domain.ai.DomainInfo;
import ai.opendw.koalawiki.domain.ai.ServiceDocumentLibrary;
import ai.opendw.koalawiki.infra.entity.DomainInfoEntity;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.repository.DomainInfoRepository;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局领域管理Controller
 * 管理不依赖仓库的全局领域
 *
 * @author zhourui(V33215020)
 * @since 2026/01/13
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/domains")
@RequiredArgsConstructor
public class GlobalDomainController {

    private final DomainInfoRepository domainRepository;
    private final WarehouseRepository warehouseRepository;
    private final DomainDocumentService domainDocumentService;

    /**
     * 获取所有领域列表
     */
    @GetMapping
    public ApiResponse<List<DomainInfo>> listAllDomains() {
        List<DomainInfo> domains = domainRepository.findAll().stream()
                .map(this::toDomainInfo)
                .collect(Collectors.toList());
        return ApiResponse.success(domains);
    }

    /**
     * 获取领域详情（包含服务列表）
     */
    @GetMapping("/{domainId}")
    public ApiResponse<DomainInfo> getDomain(@PathVariable String domainId) {
        DomainInfo domain = domainDocumentService.getDomainWithServices(domainId);
        if (domain == null) {
            return ApiResponse.error(404, "领域不存在");
        }
        return ApiResponse.success(domain);
    }

    /**
     * 创建全局领域
     */
    @PostMapping
    public ApiResponse<DomainInfo> createDomain(@RequestBody CreateDomainRequest request) {
        try {
            DomainInfoEntity entity = new DomainInfoEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setName(request.getName());
            entity.setDescription(request.getDescription());
            entity.setCode(request.getCode());
            entity = domainRepository.save(entity);
            log.info("创建全局领域成功: name={}, code={}", request.getName(), request.getCode());
            return ApiResponse.success(toDomainInfo(entity));
        } catch (Exception e) {
            log.error("创建领域失败", e);
            return ApiResponse.error(500, "创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新领域
     */
    @PutMapping("/{domainId}")
    public ApiResponse<DomainInfo> updateDomain(
            @PathVariable String domainId,
            @RequestBody UpdateDomainRequest request) {
        try {
            DomainInfoEntity entity = domainRepository.findById(domainId)
                    .orElseThrow(() -> new IllegalArgumentException("领域不存在: " + domainId));
            entity.setName(request.getName());
            entity.setDescription(request.getDescription());
            entity.setCode(request.getCode());
            entity = domainRepository.save(entity);
            return ApiResponse.success(toDomainInfo(entity));
        } catch (Exception e) {
            log.error("更新领域失败", e);
            return ApiResponse.error(500, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除领域
     */
    @DeleteMapping("/{domainId}")
    public ApiResponse<Void> deleteDomain(@PathVariable String domainId) {
        try {
            domainRepository.deleteById(domainId);
            log.info("删除领域成功: domainId={}", domainId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除领域失败", e);
            return ApiResponse.error(500, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 生成领域文档
     */
    @PostMapping("/{domainId}/generate-doc")
    public ApiResponse<String> generateDomainDocument(@PathVariable String domainId) {
        try {
            String document = domainDocumentService.generateDomainDocument(domainId, "");
            return ApiResponse.success(document);
        } catch (Exception e) {
            log.error("生成领域文档失败", e);
            return ApiResponse.error(500, "生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取领域下的服务列表
     */
    @GetMapping("/{domainId}/services")
    public ApiResponse<List<ServiceDocumentLibrary>> listServices(@PathVariable String domainId) {
        List<ServiceDocumentLibrary> services = domainDocumentService.listServicesByDomain(domainId);
        return ApiResponse.success(services);
    }

    /**
     * 在领域下创建服务（关联仓库）
     */
    @PostMapping("/{domainId}/services")
    public ApiResponse<ServiceDocumentLibrary> createService(
            @PathVariable String domainId,
            @RequestBody CreateServiceRequest request) {
        try {
            ServiceDocumentLibrary service = domainDocumentService.createService(
                    request.getWarehouseId(), domainId,
                    request.getServiceId(), request.getServiceName(),
                    request.getDescription(), request.getSourceGlobs());
            return ApiResponse.success(service);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建服务失败", e);
            return ApiResponse.error(500, "创建失败: " + e.getMessage());
        }
    }

    /**
     * 生成服务文档
     */
    @PostMapping("/{domainId}/services/{serviceId}/generate-doc")
    public ApiResponse<String> generateServiceDocument(
            @PathVariable String domainId,
            @PathVariable String serviceId) {
        try {
            String document = domainDocumentService.generateServiceDocument(serviceId, "");
            return ApiResponse.success(document);
        } catch (Exception e) {
            log.error("生成服务文档失败", e);
            return ApiResponse.error(500, "生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有仓库列表（用于服务关联）
     */
    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseInfo>> listWarehouses() {
        List<WarehouseInfo> warehouses = warehouseRepository.findAll().stream()
                .map(w -> {
                    WarehouseInfo info = new WarehouseInfo();
                    info.setId(w.getId());
                    info.setName(w.getName());
                    info.setAddress(w.getAddress());
                    return info;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(warehouses);
    }

    private DomainInfo toDomainInfo(DomainInfoEntity entity) {
        DomainInfo info = new DomainInfo();
        info.setId(entity.getId());
        info.setWarehouseId(entity.getWarehouseId());
        info.setName(entity.getName());
        info.setDescription(entity.getDescription());
        info.setCode(entity.getCode());
        info.setDocumentContent(entity.getDocumentContent());
        info.setCreatedAt(entity.getCreatedAt());
        info.setUpdatedAt(entity.getUpdatedAt());
        return info;
    }

    @Data
    public static class CreateDomainRequest {
        private String name;
        private String description;
        private String code;
    }

    @Data
    public static class UpdateDomainRequest {
        private String name;
        private String description;
        private String code;
    }

    @Data
    public static class CreateServiceRequest {
        private String warehouseId;
        private String serviceId;
        private String serviceName;
        private String description;
        private List<String> sourceGlobs;
    }

    @Data
    public static class WarehouseInfo {
        private String id;
        private String name;
        private String address;
    }

    @Data
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;
        private long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(200);
            response.setMessage("成功");
            response.setData(data);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }

        public static <T> ApiResponse<T> error(int code, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(code);
            response.setMessage(message);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }
    }
}
