package ai.opendw.koalawiki.core.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Claude Agent 实现
 *
 * <p>职责: 封装 Claude Code CLI 的调用方式</p>
 * <p>只负责构建CLI命令参数，不包含提示词构建逻辑</p>
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeAgent implements AIAgent {

    private final CLIExecutor cliExecutor;

    @Value("${ai.cli.claude:claude}")
    private String claudeCommand;

    @Override
    public String execute(String prompt) {
        log.info("ClaudeAgent开始执行，提示词长度: {}", prompt.length());

        try {
            // 构建Claude CLI命令
            String[] command = {
                claudeCommand,
                "-p",  // print模式，执行后退出
                "--output-format", "text",  // 纯文本输出
                "--tools", "Read",  // 只允许读取工具（安全考虑）
                prompt
            };

            // 执行CLI
            String result = cliExecutor.execute(command);

            log.info("ClaudeAgent执行完成，输出长度: {}", result.length());
            return result;

        } catch (CLIExecutor.CLIExecutionException e) {
            log.error("ClaudeAgent执行失败", e);
            throw new RuntimeException("Claude执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return "claude";
    }

    @Override
    public boolean isAvailable() {
        return cliExecutor.isAvailable(claudeCommand);
    }
}
