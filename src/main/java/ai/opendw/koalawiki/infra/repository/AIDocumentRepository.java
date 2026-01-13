package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.AIDocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI文档仓储
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Repository
public interface AIDocumentRepository extends JpaRepository<AIDocumentEntity, String> {

    /**
     * 根据仓库ID查询文档列表
     */
    Page<AIDocumentEntity> findByWarehouseId(String warehouseId, Pageable pageable);

    /**
     * 根据仓库与服务查询文档
     */
    Page<AIDocumentEntity> findByWarehouseIdAndServiceId(String warehouseId, String serviceId, Pageable pageable);

    /**
     * 根据仓库ID和源文件查询
     */
    Optional<AIDocumentEntity> findByWarehouseIdAndSourceFile(String warehouseId, String sourceFile);

    /**
     * 根据仓库ID和状态查询
     */
    List<AIDocumentEntity> findByWarehouseIdAndStatus(String warehouseId, String status);

    /**
     * 统计仓库文档数量
     */
    long countByWarehouseId(String warehouseId);

    /**
     * 统计服务下的文档数量
     */
    long countByWarehouseIdAndServiceId(String warehouseId, String serviceId);

    /**
     * 统计指定状态的文档数量
     */
    long countByWarehouseIdAndStatus(String warehouseId, String status);
}
