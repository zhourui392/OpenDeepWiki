package ai.opendw.koalawiki.app.service.flow;

import ai.opendw.koalawiki.core.ai.AIAgent;
import ai.opendw.koalawiki.core.ai.AIAgentFactory;
import ai.opendw.koalawiki.core.analysis.model.CallChain;
import ai.opendw.koalawiki.core.analysis.model.CallNode;
import ai.opendw.koalawiki.core.analysis.model.CallType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 业务流程语义增强服务
 * 使用AI为调用链生成业务描述
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessFlowSemanticService {

    private final AIAgentFactory agentFactory;

    /**
     * 为业务流程生成语义描述（异步）
     *
     * @param chain 调用链
     * @return 业务描述
     */
    @Async
    public CompletableFuture<String> generateFlowDescription(CallChain chain) {
        log.info("开始生成业务流程描述: flowId={}", chain.getChainId());

        try {
            String prompt = buildFlowAnalysisPrompt(chain);

            // 使用Claude CLI
            AIAgent agent = agentFactory.getAgent("claude");
            String description = agent.execute(prompt);

            log.info("业务流程描述生成完成: flowId={}, 长度={}",
                chain.getChainId(), description.length());

            return CompletableFuture.completedFuture(description);

        } catch (Exception e) {
            log.error("生成业务流程描述失败: flowId={}", chain.getChainId(), e);
            return CompletableFuture.completedFuture("AI分析失败: " + e.getMessage());
        }
    }

    /**
     * 构建流程分析提示词
     */
    private String buildFlowAnalysisPrompt(CallChain chain) {
        StringBuilder sb = new StringBuilder();
        sb.append("分析以下服务调用链路，生成业务流程描述：\n\n");

        // 入口点
        if (chain.getEntryPoint() != null) {
            sb.append("入口：").append(chain.getEntryPoint().getPath()).append("\n");
            sb.append("类型：").append(chain.getEntryPoint().getType()).append("\n\n");
        }

        // 调用链
        sb.append("调用链：\n");
        if (chain.getRoot() != null) {
            buildCallChainText(chain.getRoot(), sb, 1);
        }

        sb.append("\n请回答：\n");
        sb.append("1. 这是什么业务场景？\n");
        sb.append("2. 每个步骤的业务含义是什么？\n");
        sb.append("3. 为Mermaid时序图生成注释\n");
        sb.append("4. 总结业务流程的核心逻辑\n");

        return sb.toString();
    }

    /**
     * 构建调用链文本
     */
    private void buildCallChainText(CallNode node, StringBuilder sb, int index) {
        // Java 8兼容：手动构建缩进
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < node.getDepth(); i++) {
            indentBuilder.append("  ");
        }
        String indent = indentBuilder.toString();

        sb.append(indent).append(index).append(". ");

        if (node.getType() == CallType.DUBBO) {
            sb.append("[Dubbo] ");
        } else if (node.getType() == CallType.FEIGN) {
            sb.append("[Feign] ");
        } else if (node.getType() == CallType.MQ) {
            sb.append("[MQ] ");
        }

        sb.append(node.getClassName()).append(".")
          .append(node.getMethod()).append("\n");

        // 递归处理子调用
        int childIndex = 1;
        for (CallNode child : node.getChildren()) {
            buildCallChainText(child, sb, childIndex++);
        }
    }
}
