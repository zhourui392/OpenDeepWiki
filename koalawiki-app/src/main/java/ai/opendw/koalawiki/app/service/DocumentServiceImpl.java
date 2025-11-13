package ai.opendw.koalawiki.app.service;

import ai.opendw.koalawiki.core.document.DocumentHelper;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingContext;
import ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult;
import ai.opendw.koalawiki.core.document.pipeline.IDocumentProcessingOrchestrator;
import ai.opendw.koalawiki.domain.document.Document;
import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import ai.opendw.koalawiki.domain.document.DocumentFileItem;
import ai.opendw.koalawiki.domain.warehouse.Warehouse;
import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import ai.opendw.koalawiki.infra.entity.DocumentCatalogEntity;
import ai.opendw.koalawiki.infra.entity.DocumentFileItemEntity;
import ai.opendw.koalawiki.infra.repository.DocumentCatalogRepository;
import ai.opendw.koalawiki.infra.repository.DocumentFileItemRepository;
import ai.opendw.koalawiki.infra.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档服务实现类
 * 提供文档处理、管理和查询的核心服务实现
 *
 * 参考C#实现: DocumentsService.cs
 */
@Slf4j
@Service
public class DocumentServiceImpl implements IDocumentService {

    private final IDocumentProcessingOrchestrator orchestrator;
    private final DocumentRepository documentRepository;
    private final DocumentCatalogRepository catalogRepository;
    private final DocumentFileItemRepository fileItemRepository;

    // 存储文档处理状态
    private final Map<String, DocumentProcessingStatus> processingStatusMap = new ConcurrentHashMap<>();

    @Autowired
    public DocumentServiceImpl(
            IDocumentProcessingOrchestrator orchestrator,
            DocumentRepository documentRepository,
            DocumentCatalogRepository catalogRepository,
            DocumentFileItemRepository fileItemRepository) {
        this.orchestrator = orchestrator;
        this.documentRepository = documentRepository;
        this.catalogRepository = catalogRepository;
        this.fileItemRepository = fileItemRepository;
    }

    @Override
    @Transactional
    public IDocumentService.DocumentProcessingResult processDocument(Document document, Warehouse warehouse, String gitRepository) {
        log.info("开始处理文档: documentId={}, warehouseId={}, gitRepository={}",
                document.getId(), warehouse.getId(), gitRepository);

        long startTime = System.currentTimeMillis();
        String documentId = document.getId();

        try {
            // 更新处理状态
            processingStatusMap.put(documentId, DocumentProcessingStatus.PROCESSING);

            // 构建处理上下文
            DocumentProcessingContext context = buildProcessingContext(document, warehouse, gitRepository);

            // 调用处理编排器（返回core包的DocumentProcessingResult列表）
            List<ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult> results = orchestrator.process(context);

            // 检查是否所有处理都成功
            boolean allSuccess = results != null && !results.isEmpty() &&
                                results.stream().allMatch(ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult::isSuccess);

            if (allSuccess) {
                // 保存处理结果
                saveProcessingResults(document, results.get(0));

                // 更新文档状态
                document.setStatus(WarehouseStatus.COMPLETED);
                documentRepository.save(document);

                processingStatusMap.put(documentId, DocumentProcessingStatus.COMPLETED);

                long processingTime = System.currentTimeMillis() - startTime;
                log.info("文档处理成功: documentId={}, 耗时={}ms", documentId, processingTime);

                return IDocumentService.DocumentProcessingResult.success(documentId);
            } else {
                processingStatusMap.put(documentId, DocumentProcessingStatus.FAILED);
                String errorMsg = results != null && !results.isEmpty() ?
                                 results.get(0).getMessage() : "处理器执行失败";
                log.error("文档处理失败: documentId={}, 错误={}", documentId, errorMsg);
                return IDocumentService.DocumentProcessingResult.failure(documentId, errorMsg, null);
            }

        } catch (Exception e) {
            processingStatusMap.put(documentId, DocumentProcessingStatus.FAILED);
            log.error("文档处理异常: documentId={}", documentId, e);
            return IDocumentService.DocumentProcessingResult.failure(documentId, "文档处理异常: " + e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void processDocumentAsync(Document document, Warehouse warehouse, String gitRepository) {
        processDocument(document, warehouse, gitRepository);
    }

    @Override
    public List<DocumentCatalog> generateCatalogue(String warehouseId, String path) {
        log.info("生成文档目录: warehouseId={}, path={}", warehouseId, path);

        try {
            // 获取目录文件列表
            List<DocumentHelper.PathInfo> pathInfos = DocumentHelper.getCatalogueFiles(path);

            // 构建目录结构
            List<DocumentCatalog> catalogues = new ArrayList<>();
            Map<String, DocumentCatalog> pathToCatalogMap = new HashMap<>();

            for (DocumentHelper.PathInfo pathInfo : pathInfos) {
                DocumentCatalog catalog = new DocumentCatalog();
                catalog.setId(UUID.randomUUID().toString());
                catalog.setWarehouseId(warehouseId);
                catalog.setName(pathInfo.getName());
                catalog.setUrl(pathInfo.getPath().replace(path, ""));
                catalog.setDescription(pathInfo.getName());
                catalog.setIsCompleted(false);
                catalog.setIsDeleted(false);
                catalog.setCreatedAt(new Date());

                // 处理父子关系
                Path filePath = Paths.get(pathInfo.getPath());
                if (filePath.getParent() != null) {
                    String parentPath = filePath.getParent().toString();
                    DocumentCatalog parentCatalog = pathToCatalogMap.get(parentPath);
                    if (parentCatalog != null) {
                        catalog.setParentId(parentCatalog.getId());
                    }
                }

                catalogues.add(catalog);
                pathToCatalogMap.put(pathInfo.getPath(), catalog);
            }

            return catalogues;

        } catch (Exception e) {
            log.error("生成目录失败: warehouseId={}, path={}", warehouseId, path, e);
            throw new RuntimeException("生成目录失败", e);
        }
    }

    @Override
    public String optimizeCatalogue(String path, String readme) {
        log.info("优化目录结构: path={}", path);

        try {
            String catalogue = DocumentHelper.getCatalogue(path);
            List<DocumentHelper.PathInfo> pathInfos = DocumentHelper.getCatalogueFiles(path);

            // 如果文件数量小于800，直接返回
            if (pathInfos.size() < 800) {
                return catalogue;
            }

            // TODO: 调用AI进行智能优化（Phase 3实现）
            log.info("目录优化功能将在Phase 3（AI集成）后完成");

            // 临时实现：简单过滤
            StringBuilder optimized = new StringBuilder();
            for (String line : catalogue.split("\n")) {
                if (!line.contains("node_modules") &&
                    !line.contains("target") &&
                    !line.contains("build") &&
                    !line.contains("dist") &&
                    !line.contains(".git")) {
                    optimized.append(line).append("\n");
                }
            }

            return optimized.toString();

        } catch (Exception e) {
            log.error("优化目录失败: path={}", path, e);
            return "";
        }
    }

    @Override
    public String generateReadme(Warehouse warehouse, String path) {
        log.info("生成README文档: warehouseId={}, path={}", warehouse.getId(), path);

        try {
            // 先尝试读取现有的README
            String readme = DocumentHelper.readMeFile(path);
            if (!readme.isEmpty()) {
                log.info("找到现有README文件");
                return readme;
            }

            // 如果不存在，生成新的README
            String catalogue = DocumentHelper.getCatalogue(path);

            // TODO: 调用AI生成README（Phase 3实现）
            log.info("README生成功能将在Phase 3（AI集成）后完成");

            // 临时实现：生成基础README
            StringBuilder generatedReadme = new StringBuilder();
            generatedReadme.append("# ").append(warehouse.getName()).append("\n\n");
            generatedReadme.append("## 项目描述\n\n");
            generatedReadme.append(warehouse.getDescription()).append("\n\n");
            generatedReadme.append("## 项目结构\n\n");
            generatedReadme.append("```\n");
            generatedReadme.append(catalogue.length() > 1000 ?
                catalogue.substring(0, 1000) + "...\n" : catalogue);
            generatedReadme.append("```\n\n");
            generatedReadme.append("## 开始使用\n\n");
            generatedReadme.append("请查看项目文档了解详细信息。\n\n");
            generatedReadme.append("---\n");
            generatedReadme.append("*此README由系统自动生成*\n");

            return generatedReadme.toString();

        } catch (IOException e) {
            log.error("生成README失败: warehouseId={}, path={}", warehouse.getId(), path, e);
            return "暂无仓库说明文档";
        }
    }

    @Override
    @Transactional
    public DocumentFileItem createDocumentFileItem(String catalogId, String title, String content) {
        log.info("创建文档文件项: catalogId={}, title={}", catalogId, title);

        DocumentFileItemEntity entity = new DocumentFileItemEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setDocumentCatalogId(catalogId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setDescription(title);
        entity.setSize((long) content.getBytes().length);
        entity.setCommentCount(0L);
        entity.setRequestToken(0);
        entity.setResponseToken(0);
        entity.setIsEmbedded(false);
        entity.setCreatedAt(new Date());

        entity = fileItemRepository.save(entity);
        return convertToDocumentFileItem(entity);
    }

    @Override
    @Transactional
    public DocumentFileItem updateDocumentFileItem(String fileItemId, String title, String content) {
        log.info("更新文档文件项: fileItemId={}", fileItemId);

        DocumentFileItemEntity entity = fileItemRepository.findById(fileItemId)
                .orElseThrow(() -> new RuntimeException("文档文件项不存在: " + fileItemId));

        entity.setTitle(title);
        entity.setContent(content);
        entity.setSize((long) content.getBytes().length);

        entity = fileItemRepository.save(entity);
        return convertToDocumentFileItem(entity);
    }

    @Override
    @Transactional
    public boolean deleteDocumentFileItem(String fileItemId) {
        log.info("删除文档文件项: fileItemId={}", fileItemId);

        try {
            fileItemRepository.deleteById(fileItemId);
            return true;
        } catch (Exception e) {
            log.error("删除文档文件项失败: fileItemId={}", fileItemId, e);
            return false;
        }
    }

    @Override
    public List<DocumentFileItem> findDocumentFileItemsByCatalogId(String catalogId) {
        List<DocumentFileItemEntity> entities = fileItemRepository.findByDocumentCatalogId(catalogId);
        return convertToDocumentFileItems(entities);
    }

    @Override
    public List<DocumentCatalog> findDocumentCatalogsByWarehouseId(String warehouseId) {
        List<DocumentCatalogEntity> entities = catalogRepository.findByWarehouseId(warehouseId);
        return convertToDocumentCatalogs(entities);
    }

    @Override
    public DocumentProcessingStatus getProcessingStatus(String documentId) {
        return processingStatusMap.getOrDefault(documentId, DocumentProcessingStatus.PENDING);
    }

    @Override
    public boolean cancelProcessing(String documentId) {
        log.info("取消文档处理: documentId={}", documentId);

        DocumentProcessingStatus status = processingStatusMap.get(documentId);
        if (status == DocumentProcessingStatus.PROCESSING) {
            processingStatusMap.put(documentId, DocumentProcessingStatus.CANCELLED);
            // TODO: 实现实际的取消逻辑
            return true;
        }
        return false;
    }

    @Override
    public boolean reprocessDocument(String documentId) {
        log.info("重新处理文档: documentId={}", documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));

        // 重置状态
        document.setStatus(WarehouseStatus.PENDING);
        documentRepository.save(document);
        processingStatusMap.put(documentId, DocumentProcessingStatus.PENDING);

        // TODO: 触发重新处理
        return true;
    }

    /**
     * 构建处理上下文
     */
    private DocumentProcessingContext buildProcessingContext(Document document, Warehouse warehouse, String gitRepository) {
        return DocumentProcessingContext.builder()
                .warehouseId(warehouse.getId())
                .documentPath(document.getGitPath())
                .localPath(Paths.get(gitRepository))
                .documentType(DocumentProcessingContext.DocumentType.DIRECTORY)
                .parameters(new HashMap<String, Object>() {{
                    put("generateReadme", true);
                    put("generateChangelog", true);
                    put("enableSmartFilter", true);
                    put("isGitRepo", true);
                    put("projectName", warehouse.getName());
                }})
                .build();
    }

    /**
     * 保存处理结果
     */
    private void saveProcessingResults(Document document, ai.opendw.koalawiki.core.document.pipeline.DocumentProcessingResult result) {
        // TODO: 根据result保存目录结构、文件项等
        log.info("保存处理结果: documentId={}", document.getId());
    }

    /**
     * 实体转换方法
     */
    private DocumentFileItem convertToDocumentFileItem(DocumentFileItemEntity entity) {
        DocumentFileItem item = new DocumentFileItem();
        item.setId(entity.getId());
        item.setTitle(entity.getTitle());
        item.setDescription(entity.getDescription());
        item.setContent(entity.getContent());
        item.setDocumentCatalogId(entity.getDocumentCatalogId());
        item.setSize(entity.getSize());
        item.setCommentCount(entity.getCommentCount());
        item.setRequestToken(entity.getRequestToken());
        item.setResponseToken(entity.getResponseToken());
        item.setIsEmbedded(entity.getIsEmbedded());
        item.setCreatedAt(entity.getCreatedAt());
        // Note: BaseJpaEntity doesn't have updatedAt, using createdAt instead
        return item;
    }

    private List<DocumentFileItem> convertToDocumentFileItems(List<DocumentFileItemEntity> entities) {
        List<DocumentFileItem> items = new ArrayList<>();
        for (DocumentFileItemEntity entity : entities) {
            items.add(convertToDocumentFileItem(entity));
        }
        return items;
    }

    private DocumentCatalog convertToDocumentCatalog(DocumentCatalogEntity entity) {
        DocumentCatalog catalog = new DocumentCatalog();
        catalog.setId(entity.getId());
        catalog.setName(entity.getName());
        catalog.setUrl(entity.getUrl());
        catalog.setDescription(entity.getDescription());
        catalog.setParentId(entity.getParentId());
        catalog.setOrder(entity.getOrder());
        catalog.setDocumentId(entity.getDocumentId());
        catalog.setWarehouseId(entity.getWarehouseId());
        catalog.setIsCompleted(entity.getIsCompleted());
        catalog.setPrompt(entity.getPrompt());
        catalog.setIsDeleted(entity.getIsDeleted());
        catalog.setDeletedTime(entity.getDeletedTime());
        catalog.setCreatedAt(entity.getCreatedAt());
        // Note: BaseJpaEntity doesn't have updatedAt, using createdAt instead
        return catalog;
    }

    private List<DocumentCatalog> convertToDocumentCatalogs(List<DocumentCatalogEntity> entities) {
        List<DocumentCatalog> catalogs = new ArrayList<>();
        for (DocumentCatalogEntity entity : entities) {
            catalogs.add(convertToDocumentCatalog(entity));
        }
        return catalogs;
    }
}
