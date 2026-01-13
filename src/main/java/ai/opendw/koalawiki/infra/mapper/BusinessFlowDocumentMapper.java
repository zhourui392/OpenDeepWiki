package ai.opendw.koalawiki.infra.mapper;

import ai.opendw.koalawiki.domain.businessflow.BusinessFlowDocument;
import ai.opendw.koalawiki.infra.entity.BusinessFlowDocumentEntity;
import org.springframework.stereotype.Component;

/**
 * 业务流程文档实体映射器
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-23
 */
@Component
public class BusinessFlowDocumentMapper {

    /**
     * Entity转Domain
     */
    public BusinessFlowDocument toDomain(BusinessFlowDocumentEntity entity) {
        if (entity == null) {
            return null;
        }

        BusinessFlowDocument domain = new BusinessFlowDocument();
        domain.setId(entity.getId());
        domain.setKeyword(entity.getKeyword());
        domain.setRelevanceScore(entity.getRelevanceScore());
        domain.setEntryType(entity.getEntryType());
        domain.setApiPath(entity.getApiPath());
        domain.setClassName(entity.getClassName());
        domain.setMethodName(entity.getMethodName());
        domain.setMethodSignature(entity.getMethodSignature());
        domain.setCallChainJson(entity.getCallChainJson());
        domain.setMermaidDiagram(entity.getMermaidDiagram());
        domain.setNodeCount(entity.getNodeCount());
        domain.setMaxDepth(entity.getMaxDepth());
        domain.setRelatedServices(entity.getRelatedServices());
        domain.setPrimaryRepository(entity.getPrimaryRepository());
        domain.setRepositoryVersion(entity.getRepositoryVersion());
        domain.setDependencyRepositories(entity.getDependencyRepositories());
        domain.setDescription(entity.getDescription());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());

        return domain;
    }

    /**
     * Domain转Entity
     */
    public BusinessFlowDocumentEntity toEntity(BusinessFlowDocument domain) {
        if (domain == null) {
            return null;
        }

        BusinessFlowDocumentEntity entity = new BusinessFlowDocumentEntity();
        entity.setId(domain.getId());
        entity.setKeyword(domain.getKeyword());
        entity.setRelevanceScore(domain.getRelevanceScore());
        entity.setEntryType(domain.getEntryType());
        entity.setApiPath(domain.getApiPath());
        entity.setClassName(domain.getClassName());
        entity.setMethodName(domain.getMethodName());
        entity.setMethodSignature(domain.getMethodSignature());
        entity.setCallChainJson(domain.getCallChainJson());
        entity.setMermaidDiagram(domain.getMermaidDiagram());
        entity.setNodeCount(domain.getNodeCount());
        entity.setMaxDepth(domain.getMaxDepth());
        entity.setRelatedServices(domain.getRelatedServices());
        entity.setPrimaryRepository(domain.getPrimaryRepository());
        entity.setRepositoryVersion(domain.getRepositoryVersion());
        entity.setDependencyRepositories(domain.getDependencyRepositories());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
