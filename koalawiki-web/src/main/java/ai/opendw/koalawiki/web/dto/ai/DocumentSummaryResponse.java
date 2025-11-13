package ai.opendw.koalawiki.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档摘要响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummaryResponse {

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 摘要内容
     */
    private String summary;

    /**
     * 关键词列表
     */
    private List<String> keywords;

    /**
     * 原始长度
     */
    private Integer originalLength;

    /**
     * 摘要长度
     */
    private Integer summaryLength;

    /**
     * 生成方式（AI或降级）
     */
    private String generationMethod;

    /**
     * 生成耗时（毫秒）
     */
    private Long duration;

    /**
     * 提示信息
     */
    private String message;
}
