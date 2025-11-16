package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.ai.DocumentGenerationService;
import ai.opendw.koalawiki.domain.ai.AIDocument;
import ai.opendw.koalawiki.domain.ai.GenerationTask;
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
     * 触发文档生成
     */
    @PostMapping("/warehouses/{warehouseId}/generate-docs")
    public ApiResponse<Map<String, Object>> generateDocs(
            @PathVariable String warehouseId,
            @RequestBody(required = false) GenerateRequest request) {

        log.info("触发文档生成: warehouseId={}", warehouseId);

        try {
            // 1. 扫描Java文件
            List<File> javaFiles = scanJavaFiles(warehouseId);

            if (javaFiles.isEmpty()) {
                return ApiResponse.error(400, "未找到Java文件");
            }

            // 2. 异步生成
            String agentType = request != null ? request.getAgentType() : null;
            generationService.generateBatch(warehouseId, javaFiles, agentType);

            // 3. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("totalFiles", javaFiles.size());
            result.put("message", "文档生成任务已启动");

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("文档生成失败", e);
            return ApiResponse.error(500, "文档生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/warehouses/{warehouseId}/ai-documents")
    public ApiResponse<PageData<AIDocument>> listDocuments(
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AIDocumentEntity> entities = documentRepository
                .findByWarehouseId(warehouseId, pageable);

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

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("failed", failed);
        stats.put("generating", total - completed - failed);

        return ApiResponse.success(stats);
    }

    /**
     * 扫描仓库中的Java文件
     */
    private List<File> scanJavaFiles(String warehouseId) {
        // TODO: 从仓库路径配置获取实际路径
        // 这里简化处理,假设仓库路径为 /data/koalawiki/git/{warehouseId}
        Path repoPath = Paths.get("/data/koalawiki/git", warehouseId);

        List<File> javaFiles = new ArrayList<>();

        try {
            Files.walk(repoPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/test/"))  // 排除测试
                    .forEach(p -> javaFiles.add(p.toFile()));
        } catch (Exception e) {
            log.error("扫描Java文件失败", e);
        }

        return javaFiles;
    }

    /**
     * Entity转Domain
     */
    private AIDocument toDocument(AIDocumentEntity entity) {
        AIDocument doc = new AIDocument();
        doc.setId(entity.getId());
        doc.setWarehouseId(entity.getWarehouseId());
        doc.setSourceFile(entity.getSourceFile());
        doc.setTitle(entity.getTitle());
        doc.setContent(entity.getContent());
        doc.setStatus(entity.getStatus());
        doc.setAgentType(entity.getAgentType());
        doc.setErrorMessage(entity.getErrorMessage());
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

    /**
     * 生成请求
     */
    @Data
    public static class GenerateRequest {
        private String agentType;  // claude 或 codex
    }
}
