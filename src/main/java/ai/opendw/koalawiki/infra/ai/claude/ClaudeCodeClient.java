package ai.opendw.koalawiki.infra.ai.claude;

import ai.opendw.koalawiki.core.ai.CLIExecutor;
import ai.opendw.koalawiki.infra.ai.ChatMessage;
import ai.opendw.koalawiki.infra.ai.IAIClient;
import ai.opendw.koalawiki.infra.ai.config.AIProperties;
import ai.opendw.koalawiki.infra.ai.exception.AIException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Claude Code CLI客户端实现
 *
 * @author zhourui(V33215020)
 * @since 2026/01/13
 */
@Slf4j
@Component
public class ClaudeCodeClient implements IAIClient {

    private final AIProperties aiProperties;
    private final CLIExecutor cliExecutor;

    public ClaudeCodeClient(AIProperties aiProperties, CLIExecutor cliExecutor) {
        this.aiProperties = aiProperties;
        this.cliExecutor = cliExecutor;
    }

    @Override
    public String complete(String prompt) {
        return complete(prompt, null);
    }

    @Override
    public String complete(String prompt, Map<String, Object> options) {
        List<ChatMessage> messages = Collections.singletonList(ChatMessage.user(prompt));
        return chat(messages, options);
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        return chat(messages, null);
    }

    @Override
    @Async("aiTaskExecutor")
    public CompletableFuture<String> chatAsync(List<ChatMessage> messages) {
        return CompletableFuture.completedFuture(chat(messages));
    }

    @Override
    public String chat(List<ChatMessage> messages, Map<String, Object> options) {
        String prompt = buildPrompt(messages);
        log.debug("调用Claude Code CLI, prompt长度: {}", prompt.length());

        try {
            String[] command = {
                aiProperties.getClaudeCliPath(),
                "-p",
                "--output-format", "text"
            };

            String result = cliExecutor.executeWithInput(command, prompt, null);
            log.debug("Claude Code CLI调用成功, 输出长度: {}", result.length());
            return result;
        } catch (CLIExecutor.CLIExecutionException e) {
            log.error("Claude Code CLI调用失败", e);
            throw new AIException("Claude Code CLI调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return cliExecutor.isAvailable(aiProperties.getClaudeCliPath());
    }

    @Override
    public String getModelName() {
        return "claude-code-cli";
    }

    private String buildPrompt(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            if ("system".equals(msg.getRole())) {
                sb.append("[System]\n").append(msg.getContent()).append("\n\n");
            } else if ("user".equals(msg.getRole())) {
                sb.append("[User]\n").append(msg.getContent()).append("\n\n");
            } else if ("assistant".equals(msg.getRole())) {
                sb.append("[Assistant]\n").append(msg.getContent()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }
}
