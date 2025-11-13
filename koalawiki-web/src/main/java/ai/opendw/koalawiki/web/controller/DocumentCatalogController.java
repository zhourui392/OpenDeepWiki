package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.IDocumentCatalogService;
import ai.opendw.koalawiki.app.service.catalog.CatalogSearchService;
import ai.opendw.koalawiki.app.service.catalog.SearchRequest;
import ai.opendw.koalawiki.app.service.catalog.SearchResult;
import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.catalog.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档目录控制器
 * 提供文档目录管理的REST API
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Validated
public class DocumentCatalogController {

    private final IDocumentCatalogService catalogService;
    private final CatalogSearchService searchService;

    /**
     * 获取仓库的目录树
     *
     * @param warehouseId 仓库ID
     * @return 目录树
     */
    @GetMapping("/{warehouseId}")
    public ResponseEntity<Result<CatalogResponse>> getCatalog(@PathVariable String warehouseId) {
        log.info("Getting catalog tree: warehouseId={}", warehouseId);

        long startTime = System.currentTimeMillis();

        try {
            DocumentCatalog catalog = catalogService.getCatalogTree(warehouseId);

            if (catalog == null) {
                return ResponseEntity.ok(Result.error("目录不存在"));
            }

            CatalogResponse response = toCatalogResponse(catalog);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Catalog tree retrieved successfully: warehouseId={}, duration={}ms",
                    warehouseId, duration);

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("Failed to get catalog tree: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("获取目录失败: " + e.getMessage()));
        }
    }

    /**
     * 刷新仓库目录
     *
     * @param warehouseId 仓库ID
     * @return 刷新结果
     */
    @PostMapping("/{warehouseId}/refresh")
    public ResponseEntity<Result<CatalogResponse>> refreshCatalog(@PathVariable String warehouseId) {
        log.info("Refreshing catalog: warehouseId={}", warehouseId);

        long startTime = System.currentTimeMillis();

        try {
            DocumentCatalog catalog = catalogService.refreshCatalog(warehouseId);
            CatalogResponse response = toCatalogResponse(catalog);

            long duration = System.currentTimeMillis() - startTime;

            log.info("Catalog refreshed successfully: warehouseId={}, duration={}ms",
                    warehouseId, duration);

            return ResponseEntity.ok(Result.success(response, "目录刷新成功"));

        } catch (Exception e) {
            log.error("Failed to refresh catalog: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("刷新目录失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索目录
     *
     * @param warehouseId 仓库ID
     * @param request 搜索请求
     * @return 搜索结果
     */
    @PostMapping("/{warehouseId}/search")
    public ResponseEntity<Result<SearchResult>> search(
            @PathVariable String warehouseId,
            @Valid @RequestBody CatalogSearchRequest request) {

        log.info("Searching catalog: warehouseId={}, keyword={}",
                warehouseId, request.getKeyword());

        long startTime = System.currentTimeMillis();

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .keyword(request.getKeyword())
                    .warehouseId(warehouseId)
                    .scope(request.getScope())
                    .maxResults(request.getMaxResults())
                    .build();

            SearchResult result = searchService.search(searchRequest);

            long duration = System.currentTimeMillis() - startTime;

            log.info("Search completed: warehouseId={}, found={}, duration={}ms",
                    warehouseId, result.getTotalCount(), duration);

            return ResponseEntity.ok(Result.success(result));

        } catch (Exception e) {
            log.error("Search failed: warehouseId={}, keyword={}",
                    warehouseId, request.getKeyword(), e);
            return ResponseEntity.ok(Result.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 排序目录
     *
     * @param warehouseId 仓库ID
     * @param request 排序请求
     * @return 排序后的目录列表
     */
    @PostMapping("/{warehouseId}/sort")
    public ResponseEntity<Result<List<CatalogResponse>>> sort(
            @PathVariable String warehouseId,
            @Valid @RequestBody CatalogSortRequest request) {

        log.info("Sorting catalog: warehouseId={}, strategy={}",
                warehouseId, request.getStrategy());

        try {
            List<DocumentCatalog> catalogs = catalogService.sortCatalogs(
                    warehouseId, request.getStrategy());

            List<CatalogResponse> responses = catalogs.stream()
                    .map(this::toCatalogResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(responses,
                    "排序成功，共" + responses.size() + "个目录"));

        } catch (Exception e) {
            log.error("Sort failed: warehouseId={}, strategy={}",
                    warehouseId, request.getStrategy(), e);
            return ResponseEntity.ok(Result.error("排序失败: " + e.getMessage()));
        }
    }

    /**
     * 创建目录
     *
     * @param request 创建请求
     * @return 创建的目录
     */
    @PostMapping
    public ResponseEntity<Result<CatalogResponse>> createCatalog(
            @Valid @RequestBody CreateCatalogRequest request) {

        log.info("Creating catalog: warehouseId={}, name={}",
                request.getWarehouseId(), request.getName());

        try {
            DocumentCatalog catalog = new DocumentCatalog();
            catalog.setName(request.getName());
            catalog.setUrl(request.getUrl());
            catalog.setDescription(request.getDescription());
            catalog.setParentId(request.getParentId());
            catalog.setOrder(request.getOrder());
            catalog.setDocumentId(request.getDocumentId());
            catalog.setWarehouseId(request.getWarehouseId());
            catalog.setPrompt(request.getPrompt());

            DocumentCatalog created = catalogService.createCatalog(catalog);
            CatalogResponse response = toCatalogResponse(created);

            return ResponseEntity.ok(Result.success(response, "目录创建成功"));

        } catch (Exception e) {
            log.error("Failed to create catalog: name={}", request.getName(), e);
            return ResponseEntity.ok(Result.error("创建目录失败: " + e.getMessage()));
        }
    }

    /**
     * 更新目录
     *
     * @param catalogId 目录ID
     * @param request 更新请求
     * @return 更新后的目录
     */
    @PutMapping("/{catalogId}")
    public ResponseEntity<Result<CatalogResponse>> updateCatalog(
            @PathVariable String catalogId,
            @Valid @RequestBody UpdateCatalogRequest request) {

        log.info("Updating catalog: catalogId={}", catalogId);

        try {
            DocumentCatalog catalog = new DocumentCatalog();
            catalog.setName(request.getName());
            catalog.setUrl(request.getUrl());
            catalog.setDescription(request.getDescription());
            catalog.setOrder(request.getOrder());
            catalog.setIsCompleted(request.getIsCompleted());
            catalog.setPrompt(request.getPrompt());

            DocumentCatalog updated = catalogService.updateCatalog(catalogId, catalog);
            CatalogResponse response = toCatalogResponse(updated);

            return ResponseEntity.ok(Result.success(response, "目录更新成功"));

        } catch (Exception e) {
            log.error("Failed to update catalog: catalogId={}", catalogId, e);
            return ResponseEntity.ok(Result.error("更新目录失败: " + e.getMessage()));
        }
    }

    /**
     * 删除目录（软删除）
     *
     * @param catalogId 目录ID
     * @return 删除结果
     */
    @DeleteMapping("/{catalogId}")
    public ResponseEntity<Result<Void>> deleteCatalog(@PathVariable String catalogId) {
        log.info("Deleting catalog: catalogId={}", catalogId);

        try {
            catalogService.deleteCatalog(catalogId);
            return ResponseEntity.ok(Result.success(null, "目录删除成功"));

        } catch (Exception e) {
            log.error("Failed to delete catalog: catalogId={}", catalogId, e);
            return ResponseEntity.ok(Result.error("删除目录失败: " + e.getMessage()));
        }
    }

    /**
     * 分页查询仓库的目录列表
     *
     * @param warehouseId 仓库ID
     * @param page 页码
     * @param size 每页大小
     * @return 目录列表
     */
    @GetMapping("/{warehouseId}/list")
    public ResponseEntity<Result<Page<CatalogResponse>>> listCatalogs(
            @PathVariable String warehouseId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("Listing catalogs: warehouseId={}, page={}, size={}", warehouseId, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DocumentCatalog> catalogPage = catalogService.listCatalogsByWarehouse(
                    warehouseId, pageable);

            Page<CatalogResponse> responsePage = catalogPage.map(this::toCatalogResponse);

            return ResponseEntity.ok(Result.success(responsePage));

        } catch (Exception e) {
            log.error("Failed to list catalogs: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("查询目录列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取子目录
     *
     * @param parentId 父级ID
     * @return 子目录列表
     */
    @GetMapping("/children/{parentId}")
    public ResponseEntity<Result<List<CatalogResponse>>> getChildren(@PathVariable String parentId) {
        log.info("Getting children catalogs: parentId={}", parentId);

        try {
            List<DocumentCatalog> children = catalogService.listCatalogsByParent(parentId);

            List<CatalogResponse> responses = children.stream()
                    .map(this::toCatalogResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Result.success(responses));

        } catch (Exception e) {
            log.error("Failed to get children: parentId={}", parentId, e);
            return ResponseEntity.ok(Result.error("查询子目录失败: " + e.getMessage()));
        }
    }

    /**
     * 统计仓库的目录数量
     *
     * @param warehouseId 仓库ID
     * @return 目录数量
     */
    @GetMapping("/{warehouseId}/count")
    public ResponseEntity<Result<Long>> countCatalogs(@PathVariable String warehouseId) {
        log.info("Counting catalogs: warehouseId={}", warehouseId);

        try {
            long count = catalogService.countByWarehouse(warehouseId);
            return ResponseEntity.ok(Result.success(count));

        } catch (Exception e) {
            log.error("Failed to count catalogs: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("统计目录失败: " + e.getMessage()));
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * 转换Domain对象为Response DTO
     */
    private CatalogResponse toCatalogResponse(DocumentCatalog catalog) {
        if (catalog == null) {
            return null;
        }

        return CatalogResponse.builder()
                .id(catalog.getId())
                .name(catalog.getName())
                .url(catalog.getUrl())
                .description(catalog.getDescription())
                .parentId(catalog.getParentId())
                .order(catalog.getOrder())
                .documentId(catalog.getDocumentId())
                .warehouseId(catalog.getWarehouseId())
                .isCompleted(catalog.getIsCompleted())
                .isDeleted(catalog.getIsDeleted())
                .createdAt(catalog.getCreatedAt())
                .deletedTime(catalog.getDeletedTime())
                .build();
    }
}
