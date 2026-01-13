package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.ai.DocumentGenerationService;
import ai.opendw.koalawiki.core.git.*;
import ai.opendw.koalawiki.domain.ai.AIDocument;
import ai.opendw.koalawiki.domain.ai.GenerationTask;
import ai.opendw.koalawiki.infra.entity.AIDocumentEntity;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.repository.AIDocumentRepository;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final WarehouseRepository warehouseRepository;
    private final GitPathResolver gitPathResolver;
    private final GitRepositoryManager gitRepositoryManager;
    private final GitService gitService;

    @Value("${koalawiki.git.default-username:}")
    private String defaultGitUsername;

    @Value("${koalawiki.git.default-password:}")
    private String defaultGitPassword;

    /**
     * 触发项目架构文档生成(新)
     */
    @PostMapping("/warehouses/{warehouseId}/generate-project-doc")
    public ApiResponse<Map<String, Object>> generateProjectDoc(
            @PathVariable String warehouseId,
            @RequestBody(required = false) GenerateRequest request) {

        log.info("触发项目架构文档生成: warehouseId={}", warehouseId);

        try {
            // 1. 获取仓库信息
            WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new RuntimeException("仓库不存在: " + warehouseId));

            // 2. 克隆或更新仓库
            String gitAddress = warehouse.getAddress();
            if (gitAddress == null || gitAddress.trim().isEmpty()) {
                return ApiResponse.error(400, "仓库地址为空");
            }

            GitCredentials credentials = buildCredentials(warehouse);
            GitRepositoryInfo repoInfo = gitService.cloneRepository(gitAddress, credentials);
            String localPath = repoInfo.getLocalPath();

            // 3. 生成项目架构文档
            String agentType = request != null ? request.getAgentType() : null;
            AIDocument document = generationService.generateForProject(warehouseId, localPath, agentType);

            // 4. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("documentId", document.getId());
            result.put("title", document.getTitle());
            result.put("message", "项目架构文档生成成功");

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("项目架构文档生成失败", e);
            return ApiResponse.error(500, "文档生成失败: " + e.getMessage());
        }
    }

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
     * 按服务生成文档
     */
    @PostMapping("/warehouses/{warehouseId}/services/{serviceId}/generate-docs")
    public ApiResponse<Map<String, Object>> generateDocsByService(
            @PathVariable String warehouseId,
            @PathVariable String serviceId,
            @RequestBody(required = false) GenerateRequest request) {

        log.info("按服务生成文档: warehouseId={}, serviceId={}", warehouseId, serviceId);

        try {
            List<File> javaFiles = scanJavaFiles(warehouseId);
            if (javaFiles.isEmpty()) {
                return ApiResponse.error(400, "未找到Java文件");
            }

            String agentType = request != null ? request.getAgentType() : null;
            generationService.generateByService(warehouseId, serviceId, javaFiles, agentType);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "服务文档生成任务已启动");
            result.put("serviceId", serviceId);

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("按服务生成文档失败", e);
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
     * 扫描仓库中的Java文件
     */
    private List<File> scanJavaFiles(String warehouseId) {
        List<File> javaFiles = new ArrayList<>();

        try {
            // 1. 查询仓库信息
            WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new RuntimeException("仓库不存在: " + warehouseId));

            // 2. 获取Git地址
            String gitAddress = warehouse.getAddress();
            if (gitAddress == null || gitAddress.trim().isEmpty()) {
                log.error("仓库地址为空: warehouseId={}", warehouseId);
                return javaFiles;
            }

            // 3. 使用GitPathResolver获取本地路径
            String localPath = gitPathResolver.getLocalPath(gitAddress);
            Path repoPath = Paths.get(localPath);

            log.info("扫描Java文件: warehouseId={}, gitAddress={}, localPath={}",
                    warehouseId, gitAddress, localPath);

            // 4. 检查路径是否存在，不存在则自动克隆
            if (!Files.exists(repoPath)) {
                log.warn("仓库路径不存在: {}, 开始自动克隆仓库", localPath);

                // 4.1 构建Git凭证（使用通用方法）
                GitCredentials credentials = buildCredentials(warehouse);

                // 4.2 自动克隆仓库
                try {
                    log.info("开始克隆仓库: {}", gitAddress);
                    GitRepositoryInfo repoInfo = gitRepositoryManager.getOrCloneRepository(gitAddress, credentials);

                    if (repoInfo.getStatus() == GitRepositoryInfo.RepositoryStatus.READY) {
                        log.info("仓库克隆成功: {}", repoInfo.getLocalPath());
                        // 更新repoPath为实际克隆的路径
                        repoPath = Paths.get(repoInfo.getLocalPath());
                    } else {
                        String errorMsg = "仓库克隆失败，状态: " + repoInfo.getStatus();
                        log.error(errorMsg);
                        throw new RuntimeException(errorMsg);
                    }
                } catch (Exception cloneEx) {
                    log.error("自动克隆仓库失败: {}", gitAddress, cloneEx);
                    throw new RuntimeException("仓库尚未克隆且自动克隆失败: " + cloneEx.getMessage(), cloneEx);
                }
            }

            // 5. 再次检查路径是否存在
            if (!Files.exists(repoPath)) {
                throw new RuntimeException("仓库路径仍然不存在: " + localPath);
            }

            // 6. 扫描Java文件
            Files.walk(repoPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/test/"))  // 排除测试
                    .filter(p -> !p.toString().contains("\\test\\"))  // 排除测试(Windows)
                    .forEach(p -> javaFiles.add(p.toFile()));

            log.info("扫描到 {} 个Java文件", javaFiles.size());

        } catch (Exception e) {
            log.error("扫描Java文件失败: warehouseId={}", warehouseId, e);
            throw new RuntimeException("扫描Java文件失败: " + e.getMessage(), e);
        }

        return javaFiles;
    }

    /**
     * 构建Git凭证
     *
     * <p>优先使用仓库配置的用户名密码，如果未配置则使用系统默认凭据</p>
     *
     * @param warehouse 仓库实体
     * @return Git认证信息，如果都未配置则返回null
     * @author zhourui(V33215020)
     * @since 2025/11/21
     */
    private GitCredentials buildCredentials(WarehouseEntity warehouse) {
        String username = warehouse.getGitUserName();
        String password = warehouse.getGitPassword();

        if (username == null || username.trim().isEmpty()) {
            username = defaultGitUsername;
            if (username != null && !username.trim().isEmpty()) {
                log.debug("使用系统默认Git用户名");
            }
        }

        if (password == null || password.trim().isEmpty()) {
            password = defaultGitPassword;
            if (password != null && !password.trim().isEmpty()) {
                log.debug("使用系统默认Git密码");
            }
        }

        // 如果用户名或密码为空，返回null（用于公开仓库）
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            log.debug("未配置Git凭据，将尝试访问公开仓库");
            return null;
        }

        return GitCredentials.httpBasic(username, password);
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

    /**
     * 生成请求
     */
    @Data
    public static class GenerateRequest {
        private String agentType;  // claude
    }
}
