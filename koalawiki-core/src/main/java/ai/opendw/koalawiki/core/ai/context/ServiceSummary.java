package ai.opendw.koalawiki.core.ai.context;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务摘要
 * 用于L4层相关服务扩展，约3KB
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class ServiceSummary {

    /**
     * 服务ID
     */
    private String serviceId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 所属领域
     */
    private String domainName;

    /**
     * 关系类型（上游/下游）
     */
    private RelationType relationType;

    /**
     * 关联的接口
     */
    private List<String> relatedInterfaces = new ArrayList<>();

    /**
     * 调用示例
     */
    private List<CallExample> callExamples = new ArrayList<>();

    /**
     * 估算大小
     *
     * @return 字符数
     */
    public int estimateSize() {
        int baseSize = 200;
        int interfaceSize = relatedInterfaces.size() * 80;
        int exampleSize = callExamples.size() * 300;
        return baseSize + interfaceSize + exampleSize;
    }

    /**
     * 转换为Markdown格式
     *
     * @return Markdown文本
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();

        String relationLabel = relationType == RelationType.UPSTREAM ? "上游" : "下游";
        sb.append("### ").append(relationLabel).append("服务: ").append(serviceName).append("\n\n");

        if (description != null && !description.isEmpty()) {
            sb.append(truncate(description, 200)).append("\n\n");
        }

        if (!relatedInterfaces.isEmpty()) {
            sb.append("**关联接口**:\n");
            for (String iface : relatedInterfaces) {
                sb.append("- `").append(iface).append("`\n");
            }
            sb.append("\n");
        }

        if (!callExamples.isEmpty()) {
            sb.append("**调用示例**:\n\n");
            for (CallExample example : callExamples) {
                sb.append("```java\n");
                sb.append("// ").append(example.getDescription()).append("\n");
                sb.append(example.getCode()).append("\n");
                sb.append("```\n\n");
            }
        }

        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }

    /**
     * 关系类型
     */
    public enum RelationType {
        /**
         * 上游服务（调用当前服务）
         */
        UPSTREAM,

        /**
         * 下游服务（被当前服务调用）
         */
        DOWNSTREAM
    }

    /**
     * 调用示例
     */
    @Data
    public static class CallExample {
        private String interfaceName;
        private String methodName;
        private String description;
        private String code;
    }
}
