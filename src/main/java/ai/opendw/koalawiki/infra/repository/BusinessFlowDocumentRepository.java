package ai.opendw.koalawiki.infra.repository;

import ai.opendw.koalawiki.infra.entity.BusinessFlowDocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 业务流程文档Repository
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-23
 */
@Repository
public interface BusinessFlowDocumentRepository extends JpaRepository<BusinessFlowDocumentEntity, String> {

    /**
     * 按关键词查询
     */
    Page<BusinessFlowDocumentEntity> findByKeyword(String keyword, Pageable pageable);

    /**
     * 按API路径查询
     */
    Optional<BusinessFlowDocumentEntity> findByApiPath(String apiPath);

    /**
     * 按类名和方法名查询
     */
    Optional<BusinessFlowDocumentEntity> findByClassNameAndMethodName(String className, String methodName);

    /**
     * 按关键词和仓库版本和API路径查询（去重检查）
     */
    Optional<BusinessFlowDocumentEntity> findByKeywordAndRepositoryVersionAndApiPath(
            String keyword,
            String repositoryVersion,
            String apiPath
    );

    /**
     * 按服务名查询（JSON LIKE查询）
     */
    @Query("SELECT d FROM BusinessFlowDocumentEntity d WHERE d.relatedServices LIKE %:serviceName%")
    Page<BusinessFlowDocumentEntity> findByRelatedServiceContaining(
            @Param("serviceName") String serviceName,
            Pageable pageable
    );

    /**
     * 按关键词统计数量
     */
    long countByKeyword(String keyword);

    /**
     * 查询所有关键词及其数量（用于统计）
     */
    @Query("SELECT d.keyword, COUNT(d) FROM BusinessFlowDocumentEntity d GROUP BY d.keyword ORDER BY COUNT(d) DESC")
    List<Object[]> countByKeywordGrouped();

    /**
     * 查询最近的流程（用于首页展示）
     */
    Page<BusinessFlowDocumentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
