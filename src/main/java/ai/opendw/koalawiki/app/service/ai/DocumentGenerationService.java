package ai.opendw.koalawiki.app.service.ai;

import ai.opendw.koalawiki.domain.ai.AIDocument;
import ai.opendw.koalawiki.domain.ai.GenerationTask;
import ai.opendw.koalawiki.infra.entity.AIDocumentEntity;
import ai.opendw.koalawiki.infra.entity.GenerationTaskEntity;
import ai.opendw.koalawiki.infra.repository.AIDocumentRepository;
import ai.opendw.koalawiki.infra.repository.GenerationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI文档生成服务
 *
 * <p>职责: 文档查询服务，文档生成功能已迁移到DomainDocumentService</p>
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private final AIDocumentRepository documentRepository;
    private final GenerationTaskRepository taskRepository;

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    public GenerationTask getTaskStatus(String taskId) {
        GenerationTaskEntity entity = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        return toTask(entity);
    }

    /**
     * 获取文档
     *
     * @param documentId 文档ID
     * @return 文档信息
     */
    public AIDocument getDocument(String documentId) {
        AIDocumentEntity entity = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));
        return toDocument(entity);
    }

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
}
