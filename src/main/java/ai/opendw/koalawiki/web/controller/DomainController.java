package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.ai.DomainDocumentService;
import ai.opendw.koalawiki.domain.ai.DomainInfo;
import ai.opendw.koalawiki.domain.ai.ServiceDocumentLibrary;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 领域管理Controller
 * 按领域管理服务，支持文档生成
 *
 * @author zhourui(V33215020)
 * @since 2025/01/13
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/warehouses/{warehouseId}/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainDocumentService domainService;

    // ==================== 领域管理 ====================

    /**
     * 获取仓库下的所有领域
     */
    @GetMapping
    public ApiResponse<List<DomainInfo>> listDomains(@PathVariable String warehouseId) {
        List<DomainInfo> domains = domainService.listDomains(warehouseId);
        return ApiResponse.success(domains);
    }

    /**
     * 获取领域详情（包含服务列表）
     */
    @GetMapping("/{domainId}")
    public ApiResponse<DomainInfo> getDomain(
            @PathVariable String warehouseId,
            @PathVariable String domainId) {
        DomainInfo domain = domainService.getDomainWithServices(domainId);
        if (domain == null) {
            return ApiResponse.error(404, "领域不存在");
        }
        return ApiResponse.success(domain);
    }

    /**
     * 创建领域
     */
    @PostMapping
    public ApiResponse<DomainInfo> createDomain(
            @PathVariable String warehouseId,
            @RequestBody CreateDomainRequest request) {
        try {
            DomainInfo domain = domainService.createDomain(
                    warehouseId, request.getName(), request.getDescription(), request.getCode());
            return ApiResponse.success(domain);
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
            @PathVariable String warehouseId,
            @PathVariable String domainId,
            @RequestBody UpdateDomainRequest request) {
        try {
            DomainInfo domain = domainService.updateDomain(
                    domainId, request.getName(), request.getDescription(), request.getCode());
            return ApiResponse.success(domain);
        } catch (Exception e) {
            log.error("更新领域失败", e);
            return ApiResponse.error(500, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除领域
     */
    @DeleteMapping("/{domainId}")
    public ApiResponse<Void> deleteDomain(
            @PathVariable String warehouseId,
            @PathVariable String domainId) {
        try {
            domainService.deleteDomain(domainId);
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
    public ApiResponse<String> generateDomainDocument(
            @PathVariable String warehouseId,
            @PathVariable String domainId,
            @RequestBody(required = false) GenerateDocRequest request) {
        try {
            String sourceCodeSummary = request != null ? request.getSourceCodeSummary() : "";
            String document = domainService.generateDomainDocument(domainId, sourceCodeSummary);
            return ApiResponse.success(document);
        } catch (Exception e) {
            log.error("生成领域文档失败", e);
            return ApiResponse.error(500, "生成失败: " + e.getMessage());
        }
    }

    // ==================== 服务管理 ====================

    /**
     * 获取领域下的服务列表
     */
    @GetMapping("/{domainId}/services")
    public ApiResponse<List<ServiceDocumentLibrary>> listServices(
            @PathVariable String warehouseId,
            @PathVariable String domainId) {
        List<ServiceDocumentLibrary> services = domainService.listServicesByDomain(domainId);
        return ApiResponse.success(services);
    }

    /**
     * 创建服务
     */
    @PostMapping("/{domainId}/services")
    public ApiResponse<ServiceDocumentLibrary> createService(
            @PathVariable String warehouseId,
            @PathVariable String domainId,
            @RequestBody CreateServiceRequest request) {
        try {
            ServiceDocumentLibrary service = domainService.createService(
                    warehouseId, domainId,
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
     * 更新服务
     */
    @PutMapping("/{domainId}/services/{serviceId}")
    public ApiResponse<ServiceDocumentLibrary> updateService(
            @PathVariable String warehouseId,
            @PathVariable String domainId,
            @PathVariable String serviceId,
            @RequestBody UpdateServiceRequest request) {
        try {
            ServiceDocumentLibrary service = domainService.updateService(
                    serviceId, request.getServiceName(),
                    request.getDescription(), request.getSourceGlobs());
            return ApiResponse.success(service);
        } catch (Exception e) {
            log.error("更新服务失败", e);
            return ApiResponse.error(500, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除服务
     */
    @DeleteMapping("/{domainId}/services/{serviceId}")
    public ApiResponse<Void> deleteService(
            @PathVariable String warehouseId,
            @PathVariable String domainId,
            @PathVariable String serviceId) {
        try {
            domainService.deleteService(serviceId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除服务失败", e);
            return ApiResponse.error(500, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 生成服务文档
     */
    @PostMapping("/{domainId}/services/{serviceId}/generate-doc")
    public ApiResponse<String> generateServiceDocument(
            @PathVariable String warehouseId,
            @PathVariable String domainId,
            @PathVariable String serviceId,
            @RequestBody(required = false) GenerateDocRequest request) {
        try {
            String sourceCode = request != null ? request.getSourceCode() : "";
            String document = domainService.generateServiceDocument(serviceId, sourceCode);
            return ApiResponse.success(document);
        } catch (Exception e) {
            log.error("生成服务文档失败", e);
            return ApiResponse.error(500, "生成失败: " + e.getMessage());
        }
    }

    // ==================== DTO ====================

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
        private String serviceId;
        private String serviceName;
        private String description;
        private List<String> sourceGlobs;
    }

    @Data
    public static class UpdateServiceRequest {
        private String serviceName;
        private String description;
        private List<String> sourceGlobs;
    }

    @Data
    public static class GenerateDocRequest {
        private String sourceCode;
        private String sourceCodeSummary;
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
