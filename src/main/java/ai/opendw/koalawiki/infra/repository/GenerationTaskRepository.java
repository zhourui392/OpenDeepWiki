package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.GenerationTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档生成任务仓储
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Repository
public interface GenerationTaskRepository extends JpaRepository<GenerationTaskEntity, String> {

    /**
     * 根据仓库ID查询任务列表
     */
    Page<GenerationTaskEntity> findByWarehouseId(String warehouseId, Pageable pageable);

    /**
     * 根据仓库与服务查询任务
     */
    Page<GenerationTaskEntity> findByWarehouseIdAndServiceId(String warehouseId, String serviceId, Pageable pageable);

    /**
     * 查询最新的任务
     */
    Optional<GenerationTaskEntity> findFirstByWarehouseIdOrderByCreatedAtDesc(String warehouseId);

    /**
     * 根据状态查询任务
     */
    List<GenerationTaskEntity> findByStatus(String status);

    /**
     * 统计运行中的任务数量
     */
    long countByStatus(String status);
}
