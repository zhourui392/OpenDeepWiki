package ai.opendw.koalawiki.app.service;

import ai.opendw.koalawiki.app.ai.IAIService;
import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import ai.opendw.koalawiki.domain.document.DocumentCatalogI18n;
import ai.opendw.koalawiki.infra.entity.DocumentCatalogEntity;
import ai.opendw.koalawiki.infra.repository.DocumentCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档目录服务实现
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCatalogServiceImpl implements IDocumentCatalogService {

    private final DocumentCatalogRepository catalogRepository;
    private final IAIService aiService;

    @Override
    @Transactional
    @CacheEvict(value = "catalogCache", key = "#catalog.warehouseId")
    public DocumentCatalog createCatalog(DocumentCatalog catalog) {
        log.info("Creating catalog: warehouseId={}, name={}",
                catalog.getWarehouseId(), catalog.getName());

        if (!StringUtils.hasText(catalog.getId())) {
            catalog.setId(UUID.randomUUID().toString());
        }

        if (catalog.getOrder() == null) {
            catalog.setOrder(0);
        }

        catalog.setIsCompleted(false);
        catalog.setIsDeleted(false);
        catalog.setCreatedAt(new Date());

        DocumentCatalogEntity entity = toEntity(catalog);
        DocumentCatalogEntity saved = catalogRepository.save(entity);

        log.info("Catalog created successfully: id={}", saved.getId());
        return toDomain(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogCache", allEntries = true)
    public DocumentCatalog updateCatalog(String catalogId, DocumentCatalog catalog) {
        log.info("Updating catalog: catalogId={}", catalogId);

        DocumentCatalogEntity existing = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("Catalog not found: " + catalogId));

        // Update fields
        if (StringUtils.hasText(catalog.getName())) {
            existing.setName(catalog.getName());
        }
        if (StringUtils.hasText(catalog.getUrl())) {
            existing.setUrl(catalog.getUrl());
        }
        if (StringUtils.hasText(catalog.getDescription())) {
            existing.setDescription(catalog.getDescription());
        }
        if (catalog.getOrder() != null) {
            existing.setOrder(catalog.getOrder());
        }
        if (catalog.getIsCompleted() != null) {
            existing.setIsCompleted(catalog.getIsCompleted());
        }
        if (StringUtils.hasText(catalog.getPrompt())) {
            existing.setPrompt(catalog.getPrompt());
        }

        DocumentCatalogEntity updated = catalogRepository.save(existing);
        log.info("Catalog updated successfully: id={}", updated.getId());

        return toDomain(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogCache", allEntries = true)
    public void deleteCatalog(String catalogId) {
        log.info("Soft deleting catalog: catalogId={}", catalogId);

        DocumentCatalogEntity entity = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("Catalog not found: " + catalogId));

        entity.setIsDeleted(true);
        entity.setDeletedTime(new Date());
        catalogRepository.save(entity);

        log.info("Catalog soft deleted successfully: id={}", catalogId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogCache", allEntries = true)
    public void deleteCatalogPermanently(String catalogId) {
        log.info("Permanently deleting catalog: catalogId={}", catalogId);
        catalogRepository.deleteById(catalogId);
        log.info("Catalog permanently deleted: id={}", catalogId);
    }

    @Override
    public DocumentCatalog getCatalog(String catalogId) {
        log.debug("Getting catalog: catalogId={}", catalogId);

        return catalogRepository.findById(catalogId)
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Catalog not found: " + catalogId));
    }

    @Override
    @Cacheable(value = "catalogCache", key = "#warehouseId")
    public DocumentCatalog getCatalogTree(String warehouseId) {
        log.info("Getting catalog tree: warehouseId={}", warehouseId);

        List<DocumentCatalogEntity> allCatalogs = catalogRepository.findCatalogTree(warehouseId);

        if (allCatalogs.isEmpty()) {
            log.warn("No catalogs found for warehouse: {}", warehouseId);
            return null;
        }

        // Build tree structure
        Map<String, DocumentCatalog> catalogMap = allCatalogs.stream()
                .map(this::toDomain)
                .collect(Collectors.toMap(DocumentCatalog::getId, c -> c));

        // Root catalog (virtual)
        DocumentCatalog root = new DocumentCatalog();
        root.setId("root");
        root.setName("Root");
        root.setWarehouseId(warehouseId);
        List<DocumentCatalog> children = new ArrayList<>();

        // Organize tree
        for (DocumentCatalog catalog : catalogMap.values()) {
            if (catalog.getParentId() == null || catalog.getParentId().isEmpty()) {
                children.add(catalog);
            }
        }

        // Sort by order
        children.sort(Comparator.comparing(DocumentCatalog::getOrder));

        log.info("Catalog tree built: warehouseId={}, totalNodes={}", warehouseId, catalogMap.size());

        // Return first top-level catalog or root if multiple
        return children.isEmpty() ? root : children.get(0);
    }

    @Override
    public Page<DocumentCatalog> listCatalogs(Pageable pageable) {
        log.debug("Listing all catalogs: page={}", pageable.getPageNumber());

        Page<DocumentCatalogEntity> entityPage = catalogRepository.findAll(pageable);
        List<DocumentCatalog> catalogs = entityPage.getContent().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        return new PageImpl<>(catalogs, pageable, entityPage.getTotalElements());
    }

    @Override
    public Page<DocumentCatalog> listCatalogsByWarehouse(String warehouseId, Pageable pageable) {
        log.debug("Listing catalogs by warehouse: warehouseId={}, page={}",
                warehouseId, pageable.getPageNumber());

        List<DocumentCatalogEntity> entities = catalogRepository
                .findByWarehouseIdAndIsDeleted(warehouseId, false);

        List<DocumentCatalog> catalogs = entities.stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(DocumentCatalog::getOrder))
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), catalogs.size());
        List<DocumentCatalog> pageContent = catalogs.subList(start, end);

        return new PageImpl<>(pageContent, pageable, catalogs.size());
    }

    @Override
    public List<DocumentCatalog> listCatalogsByParent(String parentId) {
        log.debug("Listing catalogs by parent: parentId={}", parentId);

        // Find all catalogs and filter by parent
        List<DocumentCatalogEntity> allCatalogs = catalogRepository.findAll();

        return allCatalogs.stream()
                .filter(e -> !e.getIsDeleted())
                .filter(e -> parentId.equals(e.getParentId()))
                .map(this::toDomain)
                .sorted(Comparator.comparing(DocumentCatalog::getOrder))
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentCatalogI18n> getCatalogI18n(String catalogId) {
        log.debug("Getting catalog i18n: catalogId={}", catalogId);

        DocumentCatalog catalog = getCatalog(catalogId);
        return catalog.getI18nTranslations() != null
                ? catalog.getI18nTranslations()
                : Collections.emptyList();
    }

    @Override
    @Transactional
    public DocumentCatalogI18n saveCatalogI18n(String catalogId, DocumentCatalogI18n i18n) {
        log.info("Saving catalog i18n: catalogId={}, language={}",
                catalogId, i18n.getLanguageCode());

        // Verify catalog exists
        DocumentCatalog catalog = getCatalog(catalogId);

        i18n.setDocumentCatalogId(catalogId);
        if (!StringUtils.hasText(i18n.getId())) {
            i18n.setId(UUID.randomUUID().toString());
        }

        // TODO: Save to i18n repository when implemented
        log.warn("DocumentCatalogI18n repository not implemented yet");

        return i18n;
    }

    @Override
    public List<DocumentCatalog> searchCatalogs(String warehouseId, String keyword) {
        log.info("Searching catalogs: warehouseId={}, keyword={}", warehouseId, keyword);

        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        List<DocumentCatalogEntity> allCatalogs = catalogRepository
                .findByWarehouseIdAndIsDeleted(warehouseId, false);

        String lowerKeyword = keyword.toLowerCase();

        return allCatalogs.stream()
                .filter(e -> e.getName().toLowerCase().contains(lowerKeyword) ||
                            (e.getDescription() != null &&
                             e.getDescription().toLowerCase().contains(lowerKeyword)) ||
                            (e.getUrl() != null &&
                             e.getUrl().toLowerCase().contains(lowerKeyword)))
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentCatalog> sortCatalogs(String warehouseId, String sortStrategy) {
        log.info("Sorting catalogs: warehouseId={}, strategy={}", warehouseId, sortStrategy);

        List<DocumentCatalogEntity> entities = catalogRepository
                .findByWarehouseIdAndIsDeleted(warehouseId, false);

        List<DocumentCatalog> catalogs = entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        // Apply sorting strategy
        switch (sortStrategy.toLowerCase()) {
            case "alphabetical":
                catalogs.sort(Comparator.comparing(DocumentCatalog::getName));
                break;
            case "priority":
                // Priority: README first, then docs, then others
                catalogs.sort((c1, c2) -> {
                    int p1 = getPriority(c1.getName());
                    int p2 = getPriority(c2.getName());
                    if (p1 != p2) {
                        return Integer.compare(p2, p1); // Higher priority first
                    }
                    return c1.getName().compareToIgnoreCase(c2.getName());
                });
                break;
            case "custom":
            default:
                // Use order field
                catalogs.sort(Comparator.comparing(DocumentCatalog::getOrder)
                        .thenComparing(DocumentCatalog::getName));
                break;
        }

        return catalogs;
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogCache", key = "#warehouseId")
    public DocumentCatalog refreshCatalog(String warehouseId) {
        log.info("Refreshing catalog: warehouseId={}", warehouseId);

        // TODO: Implement catalog refresh from Git repository
        // This would involve:
        // 1. Parse repository file structure
        // 2. Delete existing catalogs
        // 3. Create new catalogs based on file structure
        // 4. Use AI to generate descriptions

        log.warn("Catalog refresh not fully implemented yet");

        return getCatalogTree(warehouseId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogCache", allEntries = true)
    public List<DocumentCatalog> batchCreateCatalogs(List<DocumentCatalog> catalogs) {
        log.info("Batch creating catalogs: count={}", catalogs.size());

        List<DocumentCatalogEntity> entities = catalogs.stream()
                .map(catalog -> {
                    if (!StringUtils.hasText(catalog.getId())) {
                        catalog.setId(UUID.randomUUID().toString());
                    }
                    catalog.setIsCompleted(false);
                    catalog.setIsDeleted(false);
                    catalog.setCreatedAt(new Date());
                    return toEntity(catalog);
                })
                .collect(Collectors.toList());

        List<DocumentCatalogEntity> saved = catalogRepository.saveAll(entities);

        log.info("Batch created catalogs successfully: count={}", saved.size());

        return saved.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogCache", allEntries = true)
    public void batchUpdateOrder(List<String> catalogIds) {
        log.info("Batch updating catalog order: count={}", catalogIds.size());

        for (int i = 0; i < catalogIds.size(); i++) {
            final int order = i;  // Make it effectively final for lambda
            String catalogId = catalogIds.get(i);
            catalogRepository.findById(catalogId).ifPresent(entity -> {
                entity.setOrder(order);
                catalogRepository.save(entity);
            });
        }

        log.info("Batch update order completed");
    }

    @Override
    public long countByWarehouse(String warehouseId) {
        log.debug("Counting catalogs: warehouseId={}", warehouseId);

        return catalogRepository.findByWarehouseIdAndIsDeleted(warehouseId, false).size();
    }

    @Override
    public boolean exists(String catalogId) {
        return catalogRepository.existsById(catalogId);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Get priority for sorting
     */
    private int getPriority(String name) {
        String lowerName = name.toLowerCase();
        if (lowerName.contains("readme")) {
            return 100;
        } else if (lowerName.contains("doc") || lowerName.contains("guide")) {
            return 50;
        } else if (lowerName.contains("api") || lowerName.contains("reference")) {
            return 40;
        } else if (lowerName.contains("tutorial") || lowerName.contains("example")) {
            return 30;
        }
        return 0;
    }

    /**
     * Convert Entity to Domain
     */
    private DocumentCatalog toDomain(DocumentCatalogEntity entity) {
        DocumentCatalog domain = new DocumentCatalog();
        domain.setId(entity.getId());
        domain.setName(entity.getName());
        domain.setUrl(entity.getUrl());
        domain.setDescription(entity.getDescription());
        domain.setParentId(entity.getParentId());
        domain.setOrder(entity.getOrder());
        domain.setDocumentId(entity.getDocumentId());
        domain.setWarehouseId(entity.getWarehouseId());
        domain.setIsCompleted(entity.getIsCompleted());
        domain.setPrompt(entity.getPrompt());
        domain.setIsDeleted(entity.getIsDeleted());
        domain.setDeletedTime(entity.getDeletedTime());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    /**
     * Convert Domain to Entity
     */
    private DocumentCatalogEntity toEntity(DocumentCatalog domain) {
        DocumentCatalogEntity entity = new DocumentCatalogEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setUrl(domain.getUrl());
        entity.setDescription(domain.getDescription());
        entity.setParentId(domain.getParentId());
        entity.setOrder(domain.getOrder());
        entity.setDocumentId(domain.getDocumentId());
        entity.setWarehouseId(domain.getWarehouseId());
        entity.setIsCompleted(domain.getIsCompleted());
        entity.setPrompt(domain.getPrompt());
        entity.setIsDeleted(domain.getIsDeleted());
        entity.setDeletedTime(domain.getDeletedTime());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
