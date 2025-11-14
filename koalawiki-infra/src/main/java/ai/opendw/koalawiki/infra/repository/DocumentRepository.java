package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import ai.opendw.koalawiki.infra.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档仓储接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {

    /**
     * 按仓库ID查询文档
     */
    List<DocumentEntity> findByWarehouseId(String warehouseId);

    /**
     * 按状态查询文档
     */
    List<DocumentEntity> findByStatus(WarehouseStatus status);

    /**
     * 按仓库ID和状态查询文档
     */
    List<DocumentEntity> findByWarehouseIdAndStatus(String warehouseId, WarehouseStatus status);
}