package ai.opendw.koalawiki.core.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI Agent 工厂
 * 负责选择和管理不同的Agent
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Component
public class AIAgentFactory {

    private final ClaudeAgent claudeAgent;
    private final CodexAgent codexAgent;

    @Value("${ai.default-agent:claude}")
    private String defaultAgentName;

    public AIAgentFactory(ClaudeAgent claudeAgent, CodexAgent codexAgent) {
        this.claudeAgent = claudeAgent;
        this.codexAgent = codexAgent;
    }

    /**
     * 获取指定的Agent
     *
     * @param agentName agent名称: claude, codex, null(使用默认)
     * @return AI Agent实例
     */
    public AIAgent getAgent(String agentName) {
        String name = agentName != null ? agentName.toLowerCase() : defaultAgentName;

        AIAgent agent;
        switch (name) {
            case "claude":
                agent = claudeAgent;
                break;
            case "codex":
                agent = codexAgent;
                break;
            default:
                log.warn("未知的Agent: {}, 使用默认Agent: {}", name, defaultAgentName);
                agent = getDefaultAgent();
                break;
        }

        // 检查可用性
        if (!agent.isAvailable()) {
            log.warn("Agent {} 不可用,尝试使用备用Agent", agent.getName());
            return getFallbackAgent(agent);
        }

        return agent;
    }

    /**
     * 获取默认Agent
     */
    private AIAgent getDefaultAgent() {
        if ("codex".equals(defaultAgentName)) {
            return codexAgent;
        }
        return claudeAgent;
    }

    /**
     * 获取备用Agent
     */
    private AIAgent getFallbackAgent(AIAgent failedAgent) {
        // 如果Claude失败,尝试Codex
        if (failedAgent == claudeAgent && codexAgent.isAvailable()) {
            log.info("使用Codex作为备用Agent");
            return codexAgent;
        }

        // 如果Codex失败,尝试Claude
        if (failedAgent == codexAgent && claudeAgent.isAvailable()) {
            log.info("使用Claude作为备用Agent");
            return claudeAgent;
        }

        // 都不可用,抛出异常
        throw new RuntimeException("所有Agent都不可用,请检查CLI安装");
    }

    /**
     * 检查Agent是否可用
     */
    public boolean isAgentAvailable(String agentName) {
        try {
            AIAgent agent = getAgent(agentName);
            return agent.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
