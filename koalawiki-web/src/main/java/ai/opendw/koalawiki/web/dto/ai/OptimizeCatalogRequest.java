package ai.opendw.koalawiki.web.dto.ai;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 优化目录请求
 */
@Data
public class OptimizeCatalogRequest {

    /**
     * 原始目录数据（JSON字符串）
     */
    @NotBlank(message = "目录数据不能为空")
    private String catalogData;

    /**
     * 最大文件数
     */
    @Min(value = 10, message = "最大文件数不能少于10")
    @Max(value = 1000, message = "最大文件数不能超过1000")
    private Integer maxFiles = 100;

    /**
     * 包含模式（可选）
     */
    private List<String> includePatterns;

    /**
     * 排除模式（可选）
     */
    private List<String> excludePatterns;

    /**
     * 语言（可选）
     */
    private String language;
}
