package ai.opendw.koalawiki.app.service.catalog;

/**
 * 搜索范围枚举
 *
 * @author OpenDeepWiki Team
 * @version 1.0
 * @since 2025-11-13
 */
public enum SearchScope {
    /**
     * 仅标题
     */
    TITLE,

    /**
     * 仅内容
     */
    CONTENT,

    /**
     * 全部（标题和内容）
     */
    ALL
}
