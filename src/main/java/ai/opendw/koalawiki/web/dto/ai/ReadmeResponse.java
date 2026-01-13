package ai.opendw.koalawiki.web.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * README生成响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadmeResponse {

    /**
     * 生成的README内容
     */
    private String content;

    /**
     * 生成方式（AI或降级）
     */
    private String generationMethod;

    /**
     * 检测到的语言
     */
    private String detectedLanguage;

    /**
     * 生成耗时（毫秒）
     */
    private Long duration;

    /**
     * 提示信息
     */
    private String message;
}
