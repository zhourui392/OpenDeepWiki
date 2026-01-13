package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.DomainInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 领域仓储
 *
 * @author zhourui(V33215020)
 * @since 2025/01/13
 */
@Repository
public interface DomainInfoRepository extends JpaRepository<DomainInfoEntity, String> {

    List<DomainInfoEntity> findByWarehouseId(String warehouseId);
}
