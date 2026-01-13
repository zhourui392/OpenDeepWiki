package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

/**
 * 模块信息
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
public class ModuleInfo {

    /**
     * 模块名称
     */
    private String name;

    /**
     * 模块路径
     */
    private String path;

    /**
     * 包数量
     */
    private int packageCount;

    /**
     * 类数量
     */
    private int classCount;

    /**
     * 描述
     */
    private String description;
}
