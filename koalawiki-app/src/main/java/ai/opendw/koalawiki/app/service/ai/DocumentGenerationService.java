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

    private final AIAgentFactory agentFactory;
    private final DocumentPromptBuilder promptBuilder;
    private final AIDocumentRepository documentRepository;
    private final GenerationTaskRepository taskRepository;
    private final ProjectScanner projectScanner;

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

            // 5. 构建提示词（根据Agent类型选择中文或英文）
            String prompt = agent.getName().equals("claude")
                ? promptBuilder.buildChinesePrompt(code, context)
                : promptBuilder.buildEnglishPrompt(code, context);

            // 6. 执行生成
            String content = agent.execute(prompt);

            // 7. 保存到数据库
            AIDocumentEntity entity = new AIDocumentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setWarehouseId(warehouseId);
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
     * 为整个项目生成架构文档
     *
     * @param warehouseId 仓库ID
     * @param projectPath 项目路径
     * @param agentType Agent类型 (可选)
     * @return 生成的文档
     */
    @Transactional
    public AIDocument generateForProject(String warehouseId, String projectPath, String agentType) {
        log.info("开始生成项目架构文档: warehouse={}, path={}", warehouseId, projectPath);

        try {
            // 1. 扫描项目结构
            ProjectStructure structure = projectScanner.scanProject(projectPath);
            log.info("项目扫描完成: 入口点数={}", structure.getEntryPoints().size());

            // 2. 获取Agent
            AIAgent agent = agentFactory.getAgent(agentType);
            log.info("使用Agent: {}", agent.getName());

            // 3. 构建提示词
            String prompt = promptBuilder.buildProjectAnalysisPrompt(structure);
            log.info("提示词长度: {}", prompt.length());

            // 4. 执行生成
            String content = agent.execute(prompt);

            // 5. 保存到数据库（存在则更新）
            AIDocumentEntity entity = documentRepository.findByWarehouseIdAndSourceFile(warehouseId, projectPath)
                    .orElse(new AIDocumentEntity());

            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID().toString());
                entity.setWarehouseId(warehouseId);
                entity.setSourceFile(projectPath);
            }

            entity.setTitle(structure.getProjectName() + " - 架构文档");
            entity.setContent(content);
            entity.setStatus("COMPLETED");
            entity.setAgentType(agent.getName());
            entity.setUpdatedAt(new Date());

            documentRepository.save(entity);

            log.info("项目架构文档生成成功: documentId={}", entity.getId());
            return toDocument(entity);

        } catch (Exception e) {
            log.error("项目架构文档生成失败: {}", projectPath, e);

            // 保存失败记录
            AIDocumentEntity entity = new AIDocumentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setWarehouseId(warehouseId);
            entity.setSourceFile(projectPath);
            entity.setTitle("架构文档(失败)");
            entity.setContent("");
            entity.setStatus("FAILED");
            entity.setAgentType(agentType);
            entity.setErrorMessage(e.getMessage());

            documentRepository.save(entity);

            throw new RuntimeException("项目架构文档生成失败: " + e.getMessage(), e);
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
     * Entity转Domain
     */
    private GenerationTask toTask(GenerationTaskEntity entity) {
        GenerationTask task = new GenerationTask();
        task.setId(entity.getId());
        task.setWarehouseId(entity.getWarehouseId());
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
}
