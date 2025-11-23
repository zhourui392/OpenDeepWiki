package ai.opendw.koalawiki.app.service.flow;

import ai.opendw.koalawiki.core.analysis.model.CallChain;
import ai.opendw.koalawiki.core.analysis.model.EntryPoint;
import lombok.Data;

/**
 * 业务流程结果
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Data
public class BusinessFlowResult {

    /**
     * 流程ID
     */
    private String flowId;

    /**
     * 入口点
     */
    private EntryPoint entryPoint;

    /**
     * 调用链
     */
    private CallChain callChain;

    /**
     * Mermaid时序图
     */
    private String mermaidDiagram;

    /**
     * 节点数量
     */
    private int nodeCount;

    /**
     * 最大深度
     */
    private int maxDepth;

    /**
     * AI生成的业务描述（可选）
     */
    private String description;
}
