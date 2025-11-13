package ai.opendw.koalawiki.app.service;

import ai.opendw.koalawiki.domain.document.Document;
import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import ai.opendw.koalawiki.domain.document.DocumentFileItem;
import ai.opendw.koalawiki.domain.warehouse.Warehouse;

import java.util.List;

/**
 * 文档服务接口
 * 提供文档处理、管理和查询的核心服务
 *
 * 参考C#实现: DocumentsService.cs
 */
public interface IDocumentService {

    /**
     * 处理文档
     * 触发文档处理流程，包括目录扫描、优化、生成等
     *
     * @param document 文档对象
     * @param warehouse 所属仓库
     * @param gitRepository Git仓库地址
     * @return 处理结果
     */
    DocumentProcessingResult processDocument(Document document, Warehouse warehouse, String gitRepository);

    /**
     * 异步处理文档
     *
     * @param document 文档对象
     * @param warehouse 所属仓库
     * @param gitRepository Git仓库地址
     */
    void processDocumentAsync(Document document, Warehouse warehouse, String gitRepository);

    /**
     * 生成文档目录
     *
     * @param warehouseId 仓库ID
     * @param path 文档路径
     * @return 目录结构
     */
    List<DocumentCatalog> generateCatalogue(String warehouseId, String path);

    /**
     * 智能优化目录结构
     *
     * @param path 扫描路径
     * @param readme README内容
     * @return 优化后的目录结构
     */
    String optimizeCatalogue(String path, String readme);

    /**
     * 生成README文档
     *
     * @param warehouse 仓库
     * @param path 路径
     * @return README内容
     */
    String generateReadme(Warehouse warehouse, String path);

    /**
     * 创建文档文件项
     *
     * @param catalogId 目录ID
     * @param title 标题
     * @param content 内容
     * @return 创建的文档文件项
     */
    DocumentFileItem createDocumentFileItem(String catalogId, String title, String content);

    /**
     * 更新文档文件项
     *
     * @param fileItemId 文件项ID
     * @param title 新标题
     * @param content 新内容
     * @return 更新后的文档文件项
     */
    DocumentFileItem updateDocumentFileItem(String fileItemId, String title, String content);

    /**
     * 删除文档文件项
     *
     * @param fileItemId 文件项ID
     * @return 是否删除成功
     */
    boolean deleteDocumentFileItem(String fileItemId);

    /**
     * 按目录ID查询文档文件项
     *
     * @param catalogId 目录ID
     * @return 文档文件项列表
     */
    List<DocumentFileItem> findDocumentFileItemsByCatalogId(String catalogId);

    /**
     * 按仓库ID查询文档目录
     *
     * @param warehouseId 仓库ID
     * @return 文档目录列表
     */
    List<DocumentCatalog> findDocumentCatalogsByWarehouseId(String warehouseId);

    /**
     * 获取文档处理状态
     *
     * @param documentId 文档ID
     * @return 处理状态
     */
    DocumentProcessingStatus getProcessingStatus(String documentId);

    /**
     * 取消文档处理
     *
     * @param documentId 文档ID
     * @return 是否取消成功
     */
    boolean cancelProcessing(String documentId);

    /**
     * 重新处理文档
     *
     * @param documentId 文档ID
     * @return 是否启动成功
     */
    boolean reprocessDocument(String documentId);

    /**
     * 文档处理结果
     */
    class DocumentProcessingResult {
        private boolean success;
        private String message;
        private String documentId;
        private int totalItems;
        private int processedItems;
        private int failedItems;
        private long processingTime;
        private Exception exception;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

        public int getProcessedItems() { return processedItems; }
        public void setProcessedItems(int processedItems) { this.processedItems = processedItems; }

        public int getFailedItems() { return failedItems; }
        public void setFailedItems(int failedItems) { this.failedItems = failedItems; }

        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }

        public Exception getException() { return exception; }
        public void setException(Exception exception) { this.exception = exception; }

        public static DocumentProcessingResult success(String documentId) {
            DocumentProcessingResult result = new DocumentProcessingResult();
            result.setSuccess(true);
            result.setDocumentId(documentId);
            result.setMessage("文档处理成功");
            return result;
        }

        public static DocumentProcessingResult failure(String documentId, String message, Exception exception) {
            DocumentProcessingResult result = new DocumentProcessingResult();
            result.setSuccess(false);
            result.setDocumentId(documentId);
            result.setMessage(message);
            result.setException(exception);
            return result;
        }
    }

    /**
     * 文档处理状态枚举
     */
    enum DocumentProcessingStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");

        private final String description;

        DocumentProcessingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}