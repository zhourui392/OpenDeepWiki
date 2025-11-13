package ai.opendw.koalawiki.web.dto.ai;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 文档摘要请求
 */
@Data
public class SummarizeDocumentRequest {

    /**
     * 文档ID
     */
    @NotBlank(message = "文档ID不能为空")
    private String documentId;

    /**
     * 摘要最大长度
     */
    @Min(value = 50, message = "摘要长度不能少于50字")
    @Max(value = 1000, message = "摘要长度不能超过1000字")
    private Integer maxLength = 200;

    /**
     * 是否生成关键词
     */
    private Boolean generateKeywords = true;

    /**
     * 最大关键词数量
     */
    @Min(value = 1, message = "关键词数量不能少于1")
    @Max(value = 20, message = "关键词数量不能超过20")
    private Integer maxKeywords = 5;

    /**
     * 语言（可选）
     */
    private String language;
}
