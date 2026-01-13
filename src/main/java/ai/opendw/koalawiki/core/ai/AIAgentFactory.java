package ai.opendw.koalawiki.core.ai;

import lombok.extern.slf4j.Slf4j;
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

    public AIAgentFactory(ClaudeAgent claudeAgent) {
        this.claudeAgent = claudeAgent;
    }

    /**
     * 获取指定的Agent
     *
     * @param agentName agent名称: claude, null(使用默认)
     * @return AI Agent实例
     */
    public AIAgent getAgent(String agentName) {
        return getDefaultAgent();
    }

    /**
     * 获取默认Agent
     */
    private AIAgent getDefaultAgent() {
        if (!claudeAgent.isAvailable()) {
            throw new RuntimeException("Claude Agent不可用,请检查CLI安装");
        }
        return claudeAgent;
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
