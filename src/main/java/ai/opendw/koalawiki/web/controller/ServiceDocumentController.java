package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.ai.ServiceDocumentLibraryService;
import ai.opendw.koalawiki.domain.ai.ServiceDocumentLibrary;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务文档库配置Controller
 *
 * @author zhourui(V33215020)
 * @since 2025/11/21
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/warehouses/{warehouseId}/services")
@RequiredArgsConstructor
public class ServiceDocumentController {

    private final ServiceDocumentLibraryService libraryService;

    /**
     * 获取仓库下的所有服务配置
     *
     * @param warehouseId 仓库ID
     * @return 服务配置列表
     */
    @GetMapping
    public ApiResponse<List<ServiceDocumentLibrary>> listServices(@PathVariable String warehouseId) {
        List<ServiceDocumentLibrary> services = libraryService.listByWarehouse(warehouseId);
        return ApiResponse.success(services);
    }

    /**
     * 获取单个服务配置
     *
     * @param warehouseId 仓库ID
     * @param serviceId 服务ID
     * @return 服务配置
     */
    @GetMapping("/{serviceId}")
    public ApiResponse<ServiceDocumentLibrary> getService(
            @PathVariable String warehouseId,
            @PathVariable String serviceId) {
        ServiceDocumentLibrary service = libraryService.getByServiceId(warehouseId, serviceId);
        if (service == null) {
            return ApiResponse.error(404, "服务配置不存在");
        }
        return ApiResponse.success(service);
    }

    /**
     * 创建服务配置
     *
     * @param warehouseId 仓库ID
     * @param request 创建请求
     * @return 创建后的服务配置
     */
    @PostMapping
    public ApiResponse<ServiceDocumentLibrary> createService(
            @PathVariable String warehouseId,
            @RequestBody CreateServiceRequest request) {
        try {
            ServiceDocumentLibrary library = new ServiceDocumentLibrary();
            library.setWarehouseId(warehouseId);
            library.setServiceId(request.getServiceId());
            library.setServiceName(request.getServiceName());
            library.setDescription(request.getDescription());
            library.setDocType(request.getDocType());
            library.setPromptTemplateId(request.getPromptTemplateId());
            library.setAgentType(request.getAgentType());
            library.setSourceGlobs(request.getSourceGlobs());
            library.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

            ServiceDocumentLibrary created = libraryService.create(library);
            return ApiResponse.success(created);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建服务配置失败", e);
            return ApiResponse.error(500, "创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新服务配置
     *
     * @param warehouseId 仓库ID
     * @param serviceId 服务ID
     * @param request 更新请求
     * @return 更新后的服务配置
     */
    @PutMapping("/{serviceId}")
    public ApiResponse<ServiceDocumentLibrary> updateService(
            @PathVariable String warehouseId,
            @PathVariable String serviceId,
            @RequestBody UpdateServiceRequest request) {
        try {
            ServiceDocumentLibrary existing = libraryService.getByServiceId(warehouseId, serviceId);
            if (existing == null) {
                return ApiResponse.error(404, "服务配置不存在");
            }

            existing.setServiceName(request.getServiceName());
            existing.setDescription(request.getDescription());
            existing.setDocType(request.getDocType());
            existing.setPromptTemplateId(request.getPromptTemplateId());
            existing.setAgentType(request.getAgentType());
            existing.setSourceGlobs(request.getSourceGlobs());
            existing.setEnabled(request.getEnabled());

            ServiceDocumentLibrary updated = libraryService.update(existing);
            return ApiResponse.success(updated);
        } catch (Exception e) {
            log.error("更新服务配置失败", e);
            return ApiResponse.error(500, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除服务配置
     *
     * @param warehouseId 仓库ID
     * @param serviceId 服务ID
     * @return 删除结果
     */
    @DeleteMapping("/{serviceId}")
    public ApiResponse<Void> deleteService(
            @PathVariable String warehouseId,
            @PathVariable String serviceId) {
        try {
            ServiceDocumentLibrary existing = libraryService.getByServiceId(warehouseId, serviceId);
            if (existing == null) {
                return ApiResponse.error(404, "服务配置不存在");
            }

            libraryService.delete(existing.getId());
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除服务配置失败", e);
            return ApiResponse.error(500, "删除失败: " + e.getMessage());
        }
    }

    @Data
    public static class CreateServiceRequest {
        private String serviceId;
        private String serviceName;
        private String description;
        private String docType;
        private String promptTemplateId;
        private String agentType;
        private List<String> sourceGlobs;
        private Boolean enabled;
    }

    @Data
    public static class UpdateServiceRequest {
        private String serviceName;
        private String description;
        private String docType;
        private String promptTemplateId;
        private String agentType;
        private List<String> sourceGlobs;
        private Boolean enabled;
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
