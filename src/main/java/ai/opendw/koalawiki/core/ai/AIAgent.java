package ai.opendw.koalawiki.core.ai;

/**
 * AI Agent 接口
 *
 * <p>职责: 封装特定AI CLI工具的调用方式</p>
 * <p>Agent只负责构建CLI命令，不包含提示词构建逻辑</p>
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
public interface AIAgent {

    /**
     * 使用指定的提示词执行AI生成
     *
     * @param prompt 提示词文本
     * @return AI生成的结果
     */
    String execute(String prompt);

    /**
     * 在指定工作目录下执行AI生成
     *
     * @param prompt 提示词文本
     * @param workingDirectory 工作目录（CLI执行的上下文目录）
     * @return AI生成的结果
     */
    String execute(String prompt, String workingDirectory);

    /**
     * 获取Agent名称
     *
     * @return Agent名称（如: claude）
     */
    String getName();

    /**
     * 检查Agent是否可用
     *
     * @return true-可用, false-不可用
     */
    boolean isAvailable();
}
