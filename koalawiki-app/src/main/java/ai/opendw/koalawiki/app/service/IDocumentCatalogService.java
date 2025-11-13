package ai.opendw.koalawiki.app.service;

import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import ai.opendw.koalawiki.domain.document.DocumentCatalogI18n;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 文档目录服务接口
 * 提供文档目录的CRUD操作、排序、过滤和搜索功能
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
public interface IDocumentCatalogService {

    /**
     * 创建文档目录
     *
     * @param catalog 目录对象
     * @return 创建后的目录
     */
    DocumentCatalog createCatalog(DocumentCatalog catalog);

    /**
     * 更新文档目录
     *
     * @param catalogId 目录ID
     * @param catalog 更新的目录信息
     * @return 更新后的目录
     */
    DocumentCatalog updateCatalog(String catalogId, DocumentCatalog catalog);

    /**
     * 删除文档目录（软删除）
     *
     * @param catalogId 目录ID
     */
    void deleteCatalog(String catalogId);

    /**
     * 物理删除文档目录
     *
     * @param catalogId 目录ID
     */
    void deleteCatalogPermanently(String catalogId);

    /**
     * 根据ID获取目录
     *
     * @param catalogId 目录ID
     * @return 目录对象
     */
    DocumentCatalog getCatalog(String catalogId);

    /**
     * 根据仓库ID获取目录树
     *
     * @param warehouseId 仓库ID
     * @return 目录树结构
     */
    DocumentCatalog getCatalogTree(String warehouseId);

    /**
     * 分页查询目录列表
     *
     * @param pageable 分页参数
     * @return 目录分页结果
     */
    Page<DocumentCatalog> listCatalogs(Pageable pageable);

    /**
     * 根据仓库ID分页查询目录
     *
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 目录分页结果
     */
    Page<DocumentCatalog> listCatalogsByWarehouse(String warehouseId, Pageable pageable);

    /**
     * 根据父级ID查询子目录
     *
     * @param parentId 父级ID
     * @return 子目录列表
     */
    List<DocumentCatalog> listCatalogsByParent(String parentId);

    /**
     * 获取目录的国际化翻译
     *
     * @param catalogId 目录ID
     * @return 翻译列表
     */
    List<DocumentCatalogI18n> getCatalogI18n(String catalogId);

    /**
     * 添加或更新目录翻译
     *
     * @param catalogId 目录ID
     * @param i18n 翻译对象
     * @return 翻译对象
     */
    DocumentCatalogI18n saveCatalogI18n(String catalogId, DocumentCatalogI18n i18n);

    /**
     * 搜索目录
     *
     * @param warehouseId 仓库ID
     * @param keyword 关键词
     * @return 搜索结果
     */
    List<DocumentCatalog> searchCatalogs(String warehouseId, String keyword);

    /**
     * 排序目录
     *
     * @param warehouseId 仓库ID
     * @param sortStrategy 排序策略 (alphabetical, priority, custom)
     * @return 排序后的目录列表
     */
    List<DocumentCatalog> sortCatalogs(String warehouseId, String sortStrategy);

    /**
     * 刷新仓库目录（重新从Git解析）
     *
     * @param warehouseId 仓库ID
     * @return 刷新后的目录树
     */
    DocumentCatalog refreshCatalog(String warehouseId);

    /**
     * 批量创建目录
     *
     * @param catalogs 目录列表
     * @return 创建的目录列表
     */
    List<DocumentCatalog> batchCreateCatalogs(List<DocumentCatalog> catalogs);

    /**
     * 批量更新目录排序
     *
     * @param catalogIds 目录ID列表（按顺序）
     */
    void batchUpdateOrder(List<String> catalogIds);

    /**
     * 统计仓库的目录数量
     *
     * @param warehouseId 仓库ID
     * @return 目录数量
     */
    long countByWarehouse(String warehouseId);

    /**
     * 检查目录是否存在
     *
     * @param catalogId 目录ID
     * @return 是否存在
     */
    boolean exists(String catalogId);
}
