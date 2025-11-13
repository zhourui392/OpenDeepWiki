package ai.opendw.koalawiki.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 目录优化响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogOptimizationResponse {

    /**
     * 优化后的目录数据
     */
    private String optimizedCatalog;

    /**
     * 原始文件数量
     */
    private Integer originalFileCount;

    /**
     * 优化后文件数量
     */
    private Integer optimizedFileCount;

    /**
     * 减少百分比
     */
    private Double reductionPercentage;

    /**
     * 优化方式（RULE_BASED或AI_ENHANCED）
     */
    private String optimizationMethod;

    /**
     * 优化耗时（毫秒）
     */
    private Long duration;

    /**
     * 提示信息
     */
    private String message;
}
