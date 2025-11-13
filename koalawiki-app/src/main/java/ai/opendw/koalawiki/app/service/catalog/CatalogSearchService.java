package ai.opendw.koalawiki.app.service.catalog;

import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import ai.opendw.koalawiki.infra.entity.DocumentCatalogEntity;
import ai.opendw.koalawiki.infra.repository.DocumentCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 目录搜索服务
 * 提供文档目录的搜索功能
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogSearchService {

    private final DocumentCatalogRepository catalogRepository;

    /**
     * 搜索目录
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    public SearchResult search(SearchRequest request) {
        log.info("Searching catalogs: keyword={}, warehouseId={}, scope={}",
                request.getKeyword(), request.getWarehouseId(), request.getScope());

        if (!StringUtils.hasText(request.getKeyword())) {
            return SearchResult.builder()
                    .items(Collections.emptyList())
                    .totalCount(0)
                    .suggestion("请输入搜索关键词")
                    .build();
        }

        // Get all catalogs for the warehouse
        List<DocumentCatalogEntity> allCatalogs = catalogRepository
                .findByWarehouseIdAndIsDeleted(request.getWarehouseId(), false);

        // Filter by search scope
        List<SearchItem> items = allCatalogs.stream()
                .filter(catalog -> matchesSearch(catalog, request))
                .map(catalog -> buildSearchItem(catalog, request.getKeyword()))
                .sorted(Comparator.comparingDouble(SearchItem::getRelevanceScore).reversed())
                .limit(request.getMaxResults() != null ? request.getMaxResults() : 100)
                .collect(Collectors.toList());

        // Generate suggestion if no results
        String suggestion = items.isEmpty()
                ? "未找到匹配的目录，请尝试其他关键词"
                : String.format("找到 %d 个匹配的目录", items.size());

        return SearchResult.builder()
                .items(items)
                .totalCount(items.size())
                .suggestion(suggestion)
                .build();
    }

    /**
     * 检查目录是否匹配搜索条件
     */
    private boolean matchesSearch(DocumentCatalogEntity catalog, SearchRequest request) {
        String keyword = request.getKeyword().toLowerCase();
        SearchScope scope = request.getScope();

        if (scope == null) {
            scope = SearchScope.ALL;
        }

        switch (scope) {
            case TITLE:
                return catalog.getName() != null &&
                       catalog.getName().toLowerCase().contains(keyword);

            case CONTENT:
                return (catalog.getDescription() != null &&
                        catalog.getDescription().toLowerCase().contains(keyword)) ||
                       (catalog.getPrompt() != null &&
                        catalog.getPrompt().toLowerCase().contains(keyword));

            case ALL:
            default:
                return (catalog.getName() != null &&
                        catalog.getName().toLowerCase().contains(keyword)) ||
                       (catalog.getDescription() != null &&
                        catalog.getDescription().toLowerCase().contains(keyword)) ||
                       (catalog.getUrl() != null &&
                        catalog.getUrl().toLowerCase().contains(keyword)) ||
                       (catalog.getPrompt() != null &&
                        catalog.getPrompt().toLowerCase().contains(keyword));
        }
    }

    /**
     * 构建搜索项
     */
    private SearchItem buildSearchItem(DocumentCatalogEntity catalog, String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        // Calculate relevance score
        double score = 0.0;

        // Title match (highest weight)
        if (catalog.getName() != null && catalog.getName().toLowerCase().contains(lowerKeyword)) {
            score += 10.0;
            if (catalog.getName().toLowerCase().equals(lowerKeyword)) {
                score += 20.0; // Exact match
            }
        }

        // URL match
        if (catalog.getUrl() != null && catalog.getUrl().toLowerCase().contains(lowerKeyword)) {
            score += 5.0;
        }

        // Description match
        if (catalog.getDescription() != null && catalog.getDescription().toLowerCase().contains(lowerKeyword)) {
            score += 3.0;
        }

        // Content match
        if (catalog.getPrompt() != null && catalog.getPrompt().toLowerCase().contains(lowerKeyword)) {
            score += 1.0;
        }

        // Highlight the keyword in title and description
        String highlightedTitle = highlightKeyword(catalog.getName(), keyword);
        String highlightedDescription = highlightKeyword(catalog.getDescription(), keyword);

        return SearchItem.builder()
                .catalogId(catalog.getId())
                .name(catalog.getName())
                .highlightedName(highlightedTitle)
                .description(catalog.getDescription())
                .highlightedDescription(highlightedDescription)
                .url(catalog.getUrl())
                .relevanceScore(score)
                .parentId(catalog.getParentId())
                .warehouseId(catalog.getWarehouseId())
                .build();
    }

    /**
     * 高亮关键词
     */
    private String highlightKeyword(String text, String keyword) {
        if (text == null || keyword == null) {
            return text;
        }

        // Simple highlighting: wrap keyword with markers
        // Frontend can replace these markers with actual highlight styling
        return text.replaceAll("(?i)(" + keyword + ")", "<mark>$1</mark>");
    }
}
