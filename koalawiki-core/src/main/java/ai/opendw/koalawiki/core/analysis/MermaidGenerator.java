package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.CallChain;
import ai.opendw.koalawiki.core.analysis.model.CallNode;
import ai.opendw.koalawiki.core.analysis.model.CallType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Mermaid时序图生成器
 * 将调用链转换为Mermaid DSL
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Slf4j
@Component
public class MermaidGenerator {

    /**
     * 生成Mermaid时序图
     *
     * @param chain 调用链
     * @return Mermaid DSL字符串
     */
    public String generateSequenceDiagram(CallChain chain) {
        log.info("开始生成Mermaid时序图: {}", chain.getChainId());

        StringBuilder sb = new StringBuilder();
        sb.append("sequenceDiagram\n");

        // 收集所有参与者
        Set<String> participants = collectParticipants(chain);

        // 声明参与者
        sb.append("    participant 用户\n");
        for (String participant : participants) {
            sb.append("    participant ").append(participant).append("\n");
        }
        sb.append("\n");

        // 生成入口调用
        if (chain.getEntryPoint() != null) {
            sb.append("    用户->>").append(chain.getRoot().getService())
              .append(": ").append(chain.getEntryPoint().getPath()).append("\n");
        }

        // 生成调用步骤
        if (chain.getRoot() != null) {
            generateNodeCalls(chain.getRoot(), sb, chain.getRoot().getService());
        }

        // 返回给用户
        if (chain.getRoot() != null) {
            sb.append("    ").append(chain.getRoot().getService())
              .append("-->>用户: 响应\n");
        }

        log.info("Mermaid时序图生成完成，长度: {}", sb.length());
        return sb.toString();
    }

    /**
     * 收集所有参与者（服务）
     */
    private Set<String> collectParticipants(CallChain chain) {
        Set<String> participants = new HashSet<>();
        if (chain.getRoot() != null) {
            collectParticipantsRecursive(chain.getRoot(), participants);
        }
        return participants;
    }

    private void collectParticipantsRecursive(CallNode node, Set<String> participants) {
        if (node.getService() != null) {
            participants.add(node.getService());
        }
        for (CallNode child : node.getChildren()) {
            collectParticipantsRecursive(child, participants);
        }
    }

    /**
     * 生成节点调用
     */
    private void generateNodeCalls(CallNode node, StringBuilder sb, String caller) {
        for (CallNode child : node.getChildren()) {
            String callee = child.getService();

            if (child.getType() == CallType.LOCAL) {
                // 本地调用，使用Note
                sb.append("    Note over ").append(caller)
                  .append(": ").append(extractMethodName(child.getMethod()))
                  .append("\n");

                // 递归处理子调用
                generateNodeCalls(child, sb, caller);

            } else if (child.getType() == CallType.DUBBO || child.getType() == CallType.FEIGN) {
                // 远程调用
                String callType = child.getType() == CallType.DUBBO ? "[Dubbo]" : "[Feign]";
                sb.append("    ").append(caller)
                  .append("->>").append(callee)
                  .append(": ").append(callType).append(" ")
                  .append(extractInterfaceName(child.getClassName()))
                  .append("\n");

                // 返回
                sb.append("    ").append(callee)
                  .append("-->>").append(caller)
                  .append(": 返回结果\n");

            } else if (child.getType() == CallType.MQ) {
                // MQ异步调用
                sb.append("    ").append(caller)
                  .append("->>MQ: 发送消息\n");
                sb.append("    MQ->>").append(callee)
                  .append(": 消费消息\n");
            }
        }
    }

    /**
     * 提取方法名（简化显示）
     */
    private String extractMethodName(String methodSignature) {
        if (methodSignature == null) {
            return "方法调用";
        }
        // 移除参数部分
        int idx = methodSignature.indexOf("(");
        if (idx > 0) {
            return methodSignature.substring(0, idx) + "()";
        }
        return methodSignature;
    }

    /**
     * 提取接口名（简化显示）
     */
    private String extractInterfaceName(String fullClassName) {
        if (fullClassName == null) {
            return "接口调用";
        }
        // 只保留简单类名
        int idx = fullClassName.lastIndexOf(".");
        if (idx > 0) {
            return fullClassName.substring(idx + 1);
        }
        return fullClassName;
    }
}
