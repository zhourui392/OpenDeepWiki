package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 调用链
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Data
public class CallChain {

    /**
     * 调用链ID
     */
    private String chainId;

    /**
     * 入口点
     */
    private EntryPoint entryPoint;

    /**
     * 根节点（入口方法）
     */
    private CallNode root;

    /**
     * 所有节点的扁平列表
     */
    private List<CallNode> nodes = new ArrayList<>();

    /**
     * 添加节点
     */
    public void addNode(CallNode node) {
        this.nodes.add(node);
    }

    /**
     * 获取调用链深度
     */
    public int getMaxDepth() {
        return calculateDepth(root);
    }

    private int calculateDepth(CallNode node) {
        if (node == null || node.getChildren().isEmpty()) {
            return 0;
        }
        int maxChildDepth = 0;
        for (CallNode child : node.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, calculateDepth(child));
        }
        return maxChildDepth + 1;
    }
}
