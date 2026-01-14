package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.ServiceDocumentConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服务文档库配置仓储
 *
 * @author zhourui(V33215020)
 * @since 2025/02/14
 */
@Repository
public interface ServiceDocumentConfigRepository extends JpaRepository<ServiceDocumentConfigEntity, String> {

    List<ServiceDocumentConfigEntity> findByWarehouseId(String warehouseId);

    List<ServiceDocumentConfigEntity> findByDomainId(String domainId);

    Optional<ServiceDocumentConfigEntity> findByWarehouseIdAndServiceId(String warehouseId, String serviceId);

    boolean existsByWarehouseIdAndServiceId(String warehouseId, String serviceId);

    List<ServiceDocumentConfigEntity> findByEnabled(Boolean enabled);

    List<ServiceDocumentConfigEntity> findByDomainIdAndEnabled(String domainId, Boolean enabled);

    Optional<ServiceDocumentConfigEntity> findByDomainIdAndServiceId(String domainId, String serviceId);
}
