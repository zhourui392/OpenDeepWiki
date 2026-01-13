package ai.opendw.koalawiki.app.service.ai;

import ai.opendw.koalawiki.core.ai.AIAgent;
import ai.opendw.koalawiki.core.ai.AIAgentFactory;
import ai.opendw.koalawiki.core.ai.DocumentPromptBuilder;
import ai.opendw.koalawiki.core.analysis.ProjectScanner;
import ai.opendw.koalawiki.core.analysis.model.ProjectStructure;
import ai.opendw.koalawiki.domain.ai.AIDocument;
import ai.opendw.koalawiki.domain.ai.GenerationTask;
import ai.opendw.koalawiki.infra.entity.AIDocumentEntity;
import ai.opendw.koalawiki.infra.entity.GenerationTaskEntity;
import ai.opendw.koalawiki.infra.repository.AIDocumentRepository;
import ai.opendw.koalawiki.infra.repository.GenerationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * AI文档生成服务
 *
 * <p>职责: 编排文档生成流程，协调Agent、PromptBuilder和Repository</p>
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private static final String DEFAULT_SERVICE_ID = "default";
    private static final String DEFAULT_SERVICE_NAME = "Default Service";
    private static final String DEFAULT_DOC_TYPE = "ARCHITECTURE";

    private final AIAgentFactory agentFactory;
    private final DocumentPromptBuilder promptBuilder;
    private final AIDocumentRepository documentRepository;
    private final GenerationTaskRepository taskRepository;
    private final ProjectScanner projectScanner;
    private final ServiceDocumentLibraryService libraryService;
    private final ai.opendw.koalawiki.core.analysis.ProjectReadmeScanner readmeScanner;

    /**
     * 为单个Java文件生成文档
     *
     * @param warehouseId 仓库ID
     * @param javaFile Java源文件
     * @param agentType Agent类型 (可选)
     * @return 生成的文档
     */
    @Transactional
    public AIDocument generateForFile(String warehouseId, File javaFile, String agentType) {
        log.info("开始生成文档: warehouse={}, file={}", warehouseId, javaFile.getPath());

        try {
            // 1. 读取代码 (Java 8兼容)
            String code = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);

            // 2. 解析代码(提取类名等信息)
            String className = extractClassName(javaFile);
            String packageName = extractPackageName(code);

            // 3. 构建上下文
            Map<String, Object> context = new HashMap<>();
            context.put("className", className);
            context.put("packageName", packageName);
            context.put("language", "java");

            // 4. 获取Agent
            AIAgent agent = agentFactory.getAgent(agentType);
            log.info("使用Agent: {}", agent.getName());

            // 5. 构建提示词
            String prompt = promptBuilder.buildChinesePrompt(code, context);

            // 6. 执行生成
            String content = agent.execute(prompt);

            // 7. 保存到数据库
            AIDocumentEntity entity = new AIDocumentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setWarehouseId(warehouseId);
            applyDefaultServiceContext(entity);
            entity.setSourceFile(javaFile.getPath());
            entity.setTitle(className);
            entity.setContent(content);
            entity.setStatus("COMPLETED");
            entity.setAgentType(agent.getName());

            documentRepository.save(entity);

            log.info("文档生成成功: documentId={}", entity.getId());
            return toDocument(entity);

        } catch (Exception e) {
            log.error("文档生成失败: {}", javaFile.getPath(), e);

            // 保存失败记录
            AIDocumentEntity entity = new AIDocumentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setWarehouseId(warehouseId);
            applyDefaultServiceContext(entity);
            entity.setSourceFile(javaFile.getPath());
            entity.setTitle(extractClassName(javaFile));
            entity.setContent("");
            entity.setStatus("FAILED");
            entity.setAgentType(agentType);
            entity.setErrorMessage(e.getMessage());

            documentRepository.save(entity);

            throw new RuntimeException("文档生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量生成文档(异步)
     *
     * @param warehouseId 仓库ID
     * @param javaFiles Java文件列表
     * @param agentType Agent类型
     * @return 任务ID
     */
    @Async("documentProcessingExecutor")
    public CompletableFuture<String> generateBatch(String warehouseId,
                                                     List<File> javaFiles,
                                                     String agentType) {
        // 创建任务
        String taskId = UUID.randomUUID().toString();
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setId(taskId);
        task.setWarehouseId(warehouseId);
        task.setServiceId(DEFAULT_SERVICE_ID);
        task.setDocType(DEFAULT_DOC_TYPE);
        task.setStatus("RUNNING");
        task.setTotalFiles(javaFiles.size());
        task.setCompletedFiles(0);
        task.setFailedFiles(0);
        task.setAgentType(agentType);
        task.setStartedAt(new Date());
        taskRepository.save(task);

        log.info("开始批量生成文档: taskId={}, 文件数={}", taskId, javaFiles.size());

        int completed = 0;
        int failed = 0;

        for (File file : javaFiles) {
            try {
                generateForFile(warehouseId, file, agentType);
                completed++;
            } catch (Exception e) {
                failed++;
                log.error("文件处理失败: {}", file.getPath(), e);
            }

            // 更新进度
            task.setCompletedFiles(completed);
            task.setFailedFiles(failed);
            taskRepository.save(task);
        }

        // 完成任务
        task.setStatus("COMPLETED");
        task.setCompletedAt(new Date());
        taskRepository.save(task);

        log.info("批量生成完成: taskId={}, 成功={}, 失败={}", taskId, completed, failed);
        return CompletableFuture.completedFuture(taskId);
    }

    /**
     * 为整个项目生成架构文档和系统说明文档
     *
     * <p>并行生成架构文档和系统说明文档,互不影响</p>
     *
     * @param warehouseId 仓库ID
     * @param projectPath 项目路径
     * @param agentType Agent类型 (可选)
     * @return 生成的架构文档(如果成功)
     * @author zhourui(V33215020)
     * @since 2025/11/21
     */
    @Transactional
    public AIDocument generateForProject(String warehouseId, String projectPath, String agentType) {
        log.info("开始生成项目文档: warehouse={}, path={}", warehouseId, projectPath);

        AIDocument archDoc = null;
        AIDocument readmeDoc = null;
        Exception archException = null;
        Exception readmeException = null;

        // 并行生成架构文档和系统说明文档
        try {
            archDoc = generateArchitectureDoc(warehouseId, projectPath, agentType);
            log.info("架构文档生成成功: documentId={}", archDoc.getId());
        } catch (Exception e) {
            archException = e;
            log.error("架构文档生成失败", e);
        }

        try {
            readmeDoc = generateReadmeDoc(warehouseId, projectPath, agentType);
            log.info("系统说明文档生成成功: documentId={}", readmeDoc.getId());
        } catch (Exception e) {
            readmeException = e;
            log.error("系统说明文档生成失败", e);
        }

        // 如果都失败了，抛出异常
        if (archDoc == null && readmeDoc == null) {
            String errorMsg = String.format(
                "项目文档全部生成失败 - 架构文档: %s, 系统说明文档: %s",
                archException != null ? archException.getMessage() : "未知错误",
                readmeException != null ? readmeException.getMessage() : "未知错误"
            );
            throw new RuntimeException(errorMsg);
        }

        // 返回架构文档（优先），如果架构文档失败则返回系统说明文档
        return archDoc != null ? archDoc : readmeDoc;
    }

    /**
     * 生成架构文档
     * 直接在项目工作空间下执行Claude CLI，让Claude自己读取代码
     */
    private AIDocument generateArchitectureDoc(String warehouseId, String projectPath, String agentType) {
        try {
            AIAgent agent = agentFactory.getAgent(agentType);
            String prompt = promptBuilder.buildSimpleArchitecturePrompt();

            // 在项目目录下执行CLI，让Claude自己读取代码
            String content = agent.execute(prompt, projectPath);

            String projectName = new File(projectPath).getName();
            AIDocumentEntity entity = documentRepository.findByWarehouseIdAndSourceFile(warehouseId, projectPath)
                    .orElse(new AIDocumentEntity());

            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID().toString());
                entity.setWarehouseId(warehouseId);
                entity.setSourceFile(projectPath);
            }

            applyDefaultServiceContext(entity);
            entity.setTitle(projectName + " - 架构文档");
            entity.setContent(content);
            entity.setStatus("COMPLETED");
            entity.setAgentType(agent.getName());
            entity.setUpdatedAt(new Date());

            documentRepository.save(entity);
            log.info("架构文档生成成功: documentId={}", entity.getId());
            return toDocument(entity);

        } catch (Exception e) {
            log.error("架构文档生成失败", e);
            throw new RuntimeException("架构文档生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成系统说明文档
     *
     * <p>使用 projectPath + "/SYSTEM_DOC" 作为 sourceFile 以避免与架构文档冲突</p>
     */
    private AIDocument generateReadmeDoc(String warehouseId, String projectPath, String agentType) {
        try {
            ai.opendw.koalawiki.core.analysis.model.ReadmeContext context = readmeScanner.scan(projectPath);

            AIAgent agent = agentFactory.getAgent(agentType);
            String prompt = promptBuilder.buildReadmePrompt(context, DEFAULT_SERVICE_NAME);

            // 在项目目录下执行CLI
            String content = agent.execute(prompt, projectPath);

            // 使用特殊路径标识系统说明文档
            String sourceFile = projectPath + "/SYSTEM_DOC";
            AIDocumentEntity entity = documentRepository.findByWarehouseIdAndSourceFile(warehouseId, sourceFile)
                    .orElse(new AIDocumentEntity());

            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID().toString());
                entity.setWarehouseId(warehouseId);
                entity.setSourceFile(sourceFile);
            }

            applyDefaultServiceContext(entity);
            entity.setDocType("SYSTEM_DOC");
            entity.setTitle("系统说明文档");
            entity.setContent(content);
            entity.setStatus("COMPLETED");
            entity.setAgentType(agent.getName());
            entity.setUpdatedAt(new Date());

            documentRepository.save(entity);
            log.info("系统说明文档生成成功: documentId={}", entity.getId());
            return toDocument(entity);

        } catch (Exception e) {
            log.error("系统说明文档生成失败", e);
            throw new RuntimeException("系统说明文档生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取任务状态
     */
    public GenerationTask getTaskStatus(String taskId) {
        GenerationTaskEntity entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        return toTask(entity);
    }

    /**
     * 获取文档
     */
    public AIDocument getDocument(String documentId) {
        AIDocumentEntity entity = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));
        return toDocument(entity);
    }

    /**
     * 提取类名
     */
    private String extractClassName(File file) {
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }

    /**
     * 提取包名
     */
    private String extractPackageName(String code) {
        // 简单解析: 查找 package xxx;
        String[] lines = code.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("package ") && line.endsWith(";")) {
                return line.substring(8, line.length() - 1).trim();
            }
        }
        return "";
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
     * Entity转Domain
     */
    private GenerationTask toTask(GenerationTaskEntity entity) {
        GenerationTask task = new GenerationTask();
        task.setId(entity.getId());
        task.setWarehouseId(entity.getWarehouseId());
        task.setServiceId(entity.getServiceId());
        task.setDocType(entity.getDocType());
        task.setStatus(entity.getStatus());
        task.setTotalFiles(entity.getTotalFiles());
        task.setCompletedFiles(entity.getCompletedFiles());
        task.setFailedFiles(entity.getFailedFiles());
        task.setAgentType(entity.getAgentType());
        task.setStartedAt(entity.getStartedAt());
        task.setCompletedAt(entity.getCompletedAt());
        task.setCreatedAt(entity.getCreatedAt());
        return task;
    }

    private void applyDefaultServiceContext(AIDocumentEntity entity) {
        if (entity.getServiceId() == null) {
            entity.setServiceId(DEFAULT_SERVICE_ID);
        }
        if (entity.getServiceName() == null) {
            entity.setServiceName(DEFAULT_SERVICE_NAME);
        }
        if (entity.getDocType() == null) {
            entity.setDocType(DEFAULT_DOC_TYPE);
        }
    }

    /**
     * 按服务批量生成文档
     *
     * @param warehouseId 仓库ID
     * @param serviceId 服务ID
     * @param javaFiles 所有Java文件
     * @param agentType Agent类型
     * @return 任务ID
     */
    @Async("documentProcessingExecutor")
    public CompletableFuture<String> generateByService(String warehouseId,
                                                        String serviceId,
                                                        List<File> javaFiles,
                                                        String agentType) {
        ai.opendw.koalawiki.domain.ai.ServiceDocumentLibrary config =
            libraryService.getByServiceId(warehouseId, serviceId);
        if (config == null) {
            throw new IllegalArgumentException("服务配置不存在: " + serviceId);
        }

        List<String> filePaths = new ArrayList<>();
        for (File f : javaFiles) {
            filePaths.add(f.getPath());
        }
        List<String> matched = libraryService.resolveSources(warehouseId, serviceId, filePaths);

        List<File> targetFiles = new ArrayList<>();
        for (String path : matched) {
            targetFiles.add(new File(path));
        }

        String taskId = UUID.randomUUID().toString();
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setId(taskId);
        task.setWarehouseId(warehouseId);
        task.setServiceId(serviceId);
        task.setDocType(config.getDocType());
        task.setStatus("RUNNING");
        task.setTotalFiles(targetFiles.size());
        task.setCompletedFiles(0);
        task.setFailedFiles(0);
        task.setAgentType(agentType != null ? agentType : config.getAgentType());
        task.setStartedAt(new Date());
        taskRepository.save(task);

        log.info("开始按服务生成文档: taskId={}, serviceId={}, 文件数={}",
            taskId, serviceId, targetFiles.size());

        int completed = 0;
        int failed = 0;

        for (File file : targetFiles) {
            try {
                generateForFileWithService(warehouseId, serviceId, file,
                    agentType != null ? agentType : config.getAgentType(), config);
                completed++;
            } catch (Exception e) {
                failed++;
                log.error("文件处理失败: {}", file.getPath(), e);
            }

            task.setCompletedFiles(completed);
            task.setFailedFiles(failed);
            taskRepository.save(task);
        }

        task.setStatus("COMPLETED");
        task.setCompletedAt(new Date());
        taskRepository.save(task);

        log.info("按服务生成完成: taskId={}, 成功={}, 失败={}", taskId, completed, failed);
        return CompletableFuture.completedFuture(taskId);
    }

    /**
     * 为单个文件生成文档(带服务上下文)
     */
    @Transactional
    public AIDocument generateForFileWithService(String warehouseId, String serviceId,
                                                  File javaFile, String agentType,
                                                  ai.opendw.koalawiki.domain.ai.ServiceDocumentLibrary config) {
        try {
            String code = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
            String className = extractClassName(javaFile);
            String packageName = extractPackageName(code);

            Map<String, Object> context = new HashMap<>();
            context.put("serviceName", config.getServiceName());
            context.put("serviceId", serviceId);
            context.put("docType", config.getDocType());
            context.put("filePath", javaFile.getPath());
            context.put("code", code);
            context.put("className", className);
            context.put("packageName", packageName);

            AIAgent agent = agentFactory.getAgent(agentType);
            String prompt = config.getPromptTemplateId() != null
                ? promptBuilder.buildServicePrompt(config.getPromptTemplateId(), agentType, context)
                : promptBuilder.buildChinesePrompt(code, context);

            String content = agent.execute(prompt);

            AIDocumentEntity entity = new AIDocumentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setWarehouseId(warehouseId);
            entity.setServiceId(serviceId);
            entity.setServiceName(config.getServiceName());
            entity.setDocType(config.getDocType());
            entity.setPromptTemplateId(config.getPromptTemplateId());
            entity.setSourceFile(javaFile.getPath());
            entity.setTitle(className);
            entity.setContent(content);
            entity.setStatus("COMPLETED");
            entity.setAgentType(agent.getName());

            documentRepository.save(entity);
            return toDocument(entity);

        } catch (Exception e) {
            log.error("文档生成失败: {}", javaFile.getPath(), e);

            AIDocumentEntity entity = new AIDocumentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setWarehouseId(warehouseId);
            entity.setServiceId(serviceId);
            entity.setServiceName(config.getServiceName());
            entity.setDocType(config.getDocType());
            entity.setSourceFile(javaFile.getPath());
            entity.setTitle(extractClassName(javaFile));
            entity.setContent("");
            entity.setStatus("FAILED");
            entity.setAgentType(agentType);
            entity.setErrorMessage(e.getMessage());

            documentRepository.save(entity);
            throw new RuntimeException("文档生成失败: " + e.getMessage(), e);
        }
    }
}
