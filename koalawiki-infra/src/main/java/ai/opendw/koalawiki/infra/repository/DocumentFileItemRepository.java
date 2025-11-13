package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.DocumentFileItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档文件项仓储接口
 */
@Repository
public interface DocumentFileItemRepository extends JpaRepository<DocumentFileItemEntity, String> {

    /**
     * 按目录ID查询文件项
     */
    List<DocumentFileItemEntity> findByDocumentCatalogId(String documentCatalogId);

    /**
     * 按目录ID分页查询文件项
     */
    Page<DocumentFileItemEntity> findByDocumentCatalogId(String documentCatalogId, Pageable pageable);

    /**
     * 按标题模糊查询
     */
    @Query("SELECT f FROM DocumentFileItemEntity f WHERE f.title LIKE %:keyword%")
    List<DocumentFileItemEntity> searchByTitle(@Param("keyword") String keyword);

    /**
     * 按内容模糊查询
     */
    @Query("SELECT f FROM DocumentFileItemEntity f WHERE f.content LIKE %:keyword%")
    List<DocumentFileItemEntity> searchByContent(@Param("keyword") String keyword);

    /**
     * 查询未嵌入的文件项
     */
    List<DocumentFileItemEntity> findByIsEmbedded(Boolean isEmbedded);

    /**
     * 统计目录下的文件项数量
     */
    Long countByDocumentCatalogId(String documentCatalogId);

    /**
     * 批量删除某目录下的所有文件项
     */
    void deleteByDocumentCatalogId(String documentCatalogId);

    /**
     * 查询token消耗最多的文件项
     */
    @Query("SELECT f FROM DocumentFileItemEntity f ORDER BY (f.requestToken + f.responseToken) DESC")
    Page<DocumentFileItemEntity> findTopTokenConsumers(Pageable pageable);
}