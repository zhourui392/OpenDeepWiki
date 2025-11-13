package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.domain.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档仓储接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    /**
     * 按仓库ID查询文档
     */
    List<Document> findByWarehouseId(String warehouseId);

    /**
     * 按状态查询文档
     */
    List<Document> findByStatus(String status);

    /**
     * 按仓库ID和状态查询文档
     */
    List<Document> findByWarehouseIdAndStatus(String warehouseId, String status);
}