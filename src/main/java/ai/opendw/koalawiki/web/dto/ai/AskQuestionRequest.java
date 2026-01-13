package ai.opendw.koalawiki.web.dto.ai;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 问答请求
 */
@Data
public class AskQuestionRequest {

    /**
     * 问题文本
     */
    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题长度不能超过1000字符")
    private String question;

    /**
     * 仓库ID
     */
    @NotBlank(message = "仓库ID不能为空")
    private String warehouseId;

    /**
     * 会话ID（可选，用于多轮对话）
     */
    private String sessionId;

    /**
     * 限定文档ID列表（可选）
     */
    private List<String> documentIds;

    /**
     * 语言（可选）
     */
    private String language;
}
