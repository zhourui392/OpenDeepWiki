package ai.opendw.koalawiki.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问答响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {

    /**
     * 答案文本
     */
    private String answer;

    /**
     * 置信度（0-1）
     */
    private Double confidence;

    /**
     * 引用来源
     */
    private List<Reference> references;

    /**
     * 相关问题推荐
     */
    private List<String> relatedQuestions;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 生成耗时（毫秒）
     */
    private Long duration;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 引用来源
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reference {
        /**
         * 文档ID
         */
        private String documentId;

        /**
         * 文档标题
         */
        private String documentTitle;

        /**
         * 相关片段
         */
        private String snippet;

        /**
         * 相关度评分（0-1）
         */
        private Double relevanceScore;
    }
}
