package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 调用节点
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Data
public class CallNode {

    /**
     * 服务名
     */
    private String service;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法签名
     */
    private String method;

    /**
     * 调用类型
     */
    private CallType type;

    /**
     * 调用深度
     */
    private int depth;

    /**
     * 子调用列表
     */
    private List<CallNode> children = new ArrayList<>();

    /**
     * 添加子调用
     */
    public void addChild(CallNode child) {
        if (child != null) {
            this.children.add(child);
        }
    }
}
