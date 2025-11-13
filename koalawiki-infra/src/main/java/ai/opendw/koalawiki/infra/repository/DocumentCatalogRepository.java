package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.DocumentCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档目录仓储接口
 */
@Repository
public interface DocumentCatalogRepository extends JpaRepository<DocumentCatalogEntity, String> {

    /**
     * 按仓库ID查询目录
     */
    List<DocumentCatalogEntity> findByWarehouseId(String warehouseId);

    /**
     * 按仓库ID和父级ID查询目录
     */
    List<DocumentCatalogEntity> findByWarehouseIdAndParentId(String warehouseId, String parentId);

    /**
     * 按文档ID查询目录
     */
    List<DocumentCatalogEntity> findByDocumentId(String documentId);

    /**
     * 按仓库ID查询未完成的目录
     */
    List<DocumentCatalogEntity> findByWarehouseIdAndIsCompleted(String warehouseId, Boolean isCompleted);

    /**
     * 按仓库ID和删除状态查询目录
     */
    List<DocumentCatalogEntity> findByWarehouseIdAndIsDeleted(String warehouseId, Boolean isDeleted);

    /**
     * 查询顶级目录（parentId为null）
     */
    @Query("SELECT c FROM DocumentCatalogEntity c WHERE c.warehouseId = :warehouseId AND c.parentId IS NULL AND c.isDeleted = false ORDER BY c.order")
    List<DocumentCatalogEntity> findTopLevelCatalogs(@Param("warehouseId") String warehouseId);

    /**
     * 递归查询目录树
     */
    @Query("SELECT c FROM DocumentCatalogEntity c WHERE c.warehouseId = :warehouseId AND c.isDeleted = false ORDER BY c.parentId, c.order")
    List<DocumentCatalogEntity> findCatalogTree(@Param("warehouseId") String warehouseId);
}