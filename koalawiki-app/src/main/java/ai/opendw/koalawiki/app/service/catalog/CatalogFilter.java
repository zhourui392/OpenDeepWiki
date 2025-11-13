package ai.opendw.koalawiki.app.service.catalog;

import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 目录过滤器
 * 根据过滤条件筛选目录节点
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Slf4j
@Component
public class CatalogFilter {

    /**
     * 根据过滤条件筛选目录
     *
     * @param nodes 目录节点列表
     * @param criteria 过滤条件
     * @return 过滤后的目录列表
     */
    public List<DocumentCatalog> filter(List<DocumentCatalog> nodes, FilterCriteria criteria) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        if (criteria == null) {
            return nodes;
        }

        log.debug("Filtering catalogs with criteria: {}", criteria);

        return nodes.stream()
                .filter(node -> matchesCriteria(node, criteria))
                .collect(Collectors.toList());
    }

    /**
     * 检查节点是否匹配过滤条件
     */
    private boolean matchesCriteria(DocumentCatalog node, FilterCriteria criteria) {
        // 关键词过滤
        if (StringUtils.hasText(criteria.getSearchKeyword())) {
            if (!matchesKeyword(node, criteria.getSearchKeyword())) {
                return false;
            }
        }

        // 文件类型过滤
        if (criteria.getIncludeTypes() != null && !criteria.getIncludeTypes().isEmpty()) {
            if (!matchesIncludeTypes(node, criteria.getIncludeTypes())) {
                return false;
            }
        }

        if (criteria.getExcludeTypes() != null && !criteria.getExcludeTypes().isEmpty()) {
            if (matchesExcludeTypes(node, criteria.getExcludeTypes())) {
                return false;
            }
        }

        // 完成状态过滤
        if (criteria.getCompletedOnly() != null && criteria.getCompletedOnly()) {
            if (node.getIsCompleted() == null || !node.getIsCompleted()) {
                return false;
            }
        }

        // 删除状态过滤
        if (criteria.getIncludeDeleted() == null || !criteria.getIncludeDeleted()) {
            if (node.getIsDeleted() != null && node.getIsDeleted()) {
                return false;
            }
        }

        // 父级ID过滤
        if (StringUtils.hasText(criteria.getParentId())) {
            if (!criteria.getParentId().equals(node.getParentId())) {
                return false;
            }
        }

        // order范围过滤
        if (criteria.getMinOrder() != null) {
            if (node.getOrder() == null || node.getOrder() < criteria.getMinOrder()) {
                return false;
            }
        }

        if (criteria.getMaxOrder() != null) {
            if (node.getOrder() == null || node.getOrder() > criteria.getMaxOrder()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否匹配关键词
     */
    private boolean matchesKeyword(DocumentCatalog node, String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        if (node.getName() != null && node.getName().toLowerCase().contains(lowerKeyword)) {
            return true;
        }

        if (node.getDescription() != null && node.getDescription().toLowerCase().contains(lowerKeyword)) {
            return true;
        }

        if (node.getUrl() != null && node.getUrl().toLowerCase().contains(lowerKeyword)) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否匹配包含的文件类型
     */
    private boolean matchesIncludeTypes(DocumentCatalog node, List<String> includeTypes) {
        if (node.getUrl() == null || node.getUrl().isEmpty()) {
            return false;
        }

        String url = node.getUrl().toLowerCase();
        return includeTypes.stream()
                .anyMatch(type -> url.endsWith(type.toLowerCase()));
    }

    /**
     * 检查是否匹配排除的文件类型
     */
    private boolean matchesExcludeTypes(DocumentCatalog node, List<String> excludeTypes) {
        if (node.getUrl() == null || node.getUrl().isEmpty()) {
            return false;
        }

        String url = node.getUrl().toLowerCase();
        return excludeTypes.stream()
                .anyMatch(type -> url.endsWith(type.toLowerCase()));
    }
}
