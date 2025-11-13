package ai.opendw.koalawiki.web.dto.ai;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 生成README请求
 */
@Data
public class GenerateReadmeRequest {

    /**
     * 仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * 语言（可选，默认自动检测）
     */
    private String language;

    /**
     * 模板类型（可选）
     */
    private String templateType;

    /**
     * 是否包含徽章
     */
    private Boolean includeBadges = true;

    /**
     * 是否包含目录结构
     */
    private Boolean includeStructure = true;
}
