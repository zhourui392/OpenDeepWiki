package ai.opendw.koalawiki.app.service.catalog;

import ai.opendw.koalawiki.domain.document.DocumentCatalog;

import java.util.List;

/**
 * 目录排序策略接口
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
public interface CatalogSortStrategy {

    /**
     * 对目录节点进行排序
     *
     * @param nodes 目录节点列表
     * @return 排序后的目录节点列表
     */
    List<DocumentCatalog> sort(List<DocumentCatalog> nodes);

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();
}
