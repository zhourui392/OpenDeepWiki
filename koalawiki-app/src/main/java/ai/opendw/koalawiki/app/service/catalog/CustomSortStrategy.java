package ai.opendw.koalawiki.app.service.catalog;

import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义排序策略
 * 按order字段排序，相同order按名称排序
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Component
public class CustomSortStrategy implements CatalogSortStrategy {

    @Override
    public List<DocumentCatalog> sort(List<DocumentCatalog> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return nodes;
        }

        return nodes.stream()
                .sorted(Comparator.comparing(DocumentCatalog::getOrder)
                        .thenComparing(DocumentCatalog::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "custom";
    }
}
