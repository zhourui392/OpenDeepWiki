package ai.opendw.koalawiki.app.service.catalog;

import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优先级排序策略
 * 按文档优先级排序（README优先、文档优先等）
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
@Component
public class PrioritySortStrategy implements CatalogSortStrategy {

    @Override
    public List<DocumentCatalog> sort(List<DocumentCatalog> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return nodes;
        }

        return nodes.stream()
                .sorted(Comparator.comparingInt(this::calculatePriority)
                        .reversed() // Higher priority first
                        .thenComparing(DocumentCatalog::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "priority";
    }

    /**
     * 计算目录的优先级
     * 优先级越高，排序越靠前
     *
     * @param catalog 目录
     * @return 优先级分数
     */
    private int calculatePriority(DocumentCatalog catalog) {
        String name = catalog.getName().toLowerCase();
        String url = catalog.getUrl() != null ? catalog.getUrl().toLowerCase() : "";

        // README相关优先级最高
        if (name.contains("readme") || url.contains("readme")) {
            return 1000;
        }

        // 文档、指南类
        if (name.contains("doc") || name.contains("guide") || name.contains("documentation")) {
            return 500;
        }

        // API参考文档
        if (name.contains("api") || name.contains("reference")) {
            return 400;
        }

        // 教程、示例
        if (name.contains("tutorial") || name.contains("example") || name.contains("sample")) {
            return 300;
        }

        // 快速开始、安装
        if (name.contains("quick") || name.contains("start") || name.contains("install") ||
            name.contains("setup") || name.contains("getting")) {
            return 250;
        }

        // 架构、设计
        if (name.contains("architect") || name.contains("design") || name.contains("structure")) {
            return 200;
        }

        // 开发相关
        if (name.contains("develop") || name.contains("contrib") || name.contains("contribution")) {
            return 150;
        }

        // 配置
        if (name.contains("config") || name.contains("setting")) {
            return 100;
        }

        // FAQ、帮助
        if (name.contains("faq") || name.contains("help") || name.contains("troubleshoot")) {
            return 80;
        }

        // 更新日志、变更记录
        if (name.contains("changelog") || name.contains("release") || name.contains("history")) {
            return 50;
        }

        // License、协议
        if (name.contains("license") || name.contains("legal")) {
            return 20;
        }

        // 默认优先级
        return 0;
    }
}
