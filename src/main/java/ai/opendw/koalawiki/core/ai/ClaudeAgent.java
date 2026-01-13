package ai.opendw.koalawiki.core.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
        return execute(prompt, null);
    }

    @Override
    public String execute(String prompt, String workingDirectory) {
        log.info("ClaudeAgent开始执行，提示词长度: {}, 工作目录: {}",
            prompt.length(), workingDirectory);

        try {
            String[] command = {
                claudeCommand,
                "-p",
                "--output-format", "text"
            };

            String result = cliExecutor.executeWithInput(command, prompt, workingDirectory);
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
