package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.ai.DocumentGenerationService;
import ai.opendw.koalawiki.domain.ai.AIDocument;
import ai.opendw.koalawiki.infra.entity.AIDocumentEntity;
import ai.opendw.koalawiki.infra.repository.AIDocumentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI文档Controller
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AIDocumentController {

    private final DocumentGenerationService generationService;
    private final AIDocumentRepository documentRepository;

    /**
     * 获取文档列表
     */
    @GetMapping("/warehouses/{warehouseId}/ai-documents")
    public ApiResponse<PageData<AIDocument>> listDocuments(
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String serviceId) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AIDocumentEntity> entities;
        if (serviceId != null && !serviceId.trim().isEmpty()) {
            entities = documentRepository.findByWarehouseIdAndServiceId(warehouseId, serviceId, pageable);
        } else {
            entities = documentRepository.findByWarehouseId(warehouseId, pageable);
        }

        List<AIDocument> documents = entities.getContent().stream()
                .map(this::toDocument)
                .collect(Collectors.toList());

        PageData<AIDocument> pageData = new PageData<>();
        pageData.setContent(documents);
        pageData.setPage(page);
        pageData.setSize(size);
        pageData.setTotal(entities.getTotalElements());
        pageData.setTotalPages(entities.getTotalPages());

        return ApiResponse.success(pageData);
    }

    /**
     * 获取单个文档
     */
    @GetMapping("/ai-documents/{id}")
    public ApiResponse<AIDocument> getDocument(@PathVariable String id) {
        try {
            AIDocument document = generationService.getDocument(id);
            return ApiResponse.success(document);
        } catch (Exception e) {
            return ApiResponse.error(404, "文档不存在");
        }
    }

    /**
     * 获取文档统计
     */
    @GetMapping("/warehouses/{warehouseId}/doc-stats")
    public ApiResponse<Map<String, Object>> getDocStats(@PathVariable String warehouseId) {
        long total = documentRepository.countByWarehouseId(warehouseId);
        long completed = documentRepository.countByWarehouseIdAndStatus(warehouseId, "COMPLETED");
        long failed = documentRepository.countByWarehouseIdAndStatus(warehouseId, "FAILED");
        long generating = total - completed - failed;

        // 计算成功率
        double successRate = 0.0;
        if (total > 0) {
            successRate = (completed * 100.0) / total;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", total);
        stats.put("completedCount", completed);
        stats.put("failedCount", failed);
        stats.put("generatingCount", generating);
        stats.put("successRate", successRate);
        stats.put("agentUsage", new HashMap<>()); // TODO: 实现agent使用统计

        return ApiResponse.success(stats);
    }

    /**
     * Entity转Domain
     */
    private AIDocument toDocument(AIDocumentEntity entity) {
        AIDocument doc = new AIDocument();
        doc.setId(entity.getId());
        doc.setWarehouseId(entity.getWarehouseId());
        doc.setServiceId(entity.getServiceId());
        doc.setServiceName(entity.getServiceName());
        doc.setDocType(entity.getDocType());
        doc.setPromptTemplateId(entity.getPromptTemplateId());
        doc.setSourceFile(entity.getSourceFile());
        doc.setTitle(entity.getTitle());
        doc.setContent(entity.getContent());
        doc.setStatus(entity.getStatus());
        doc.setAgentType(entity.getAgentType());
        doc.setErrorMessage(entity.getErrorMessage());
        doc.setMetadata(entity.getMetadata());
        doc.setCreatedAt(entity.getCreatedAt());
        doc.setUpdatedAt(entity.getUpdatedAt());
        return doc;
    }

    /**
     * 统一响应
     */
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

    /**
     * 分页数据
     */
    @Data
    public static class PageData<T> {
        private List<T> content;
        private int page;
        private int size;
        private long total;
        private int totalPages;
    }
}
