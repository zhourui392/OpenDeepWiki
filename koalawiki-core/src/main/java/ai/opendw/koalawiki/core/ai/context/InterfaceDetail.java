package ai.opendw.koalawiki.core.ai.context;

import ai.opendw.koalawiki.domain.dubbo.DubboMethodInfo;
import ai.opendw.koalawiki.domain.dubbo.MethodParameter;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 接口详情
 * 提供Dubbo接口的完整信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class InterfaceDetail {

    /**
     * 接口ID
     */
    private String id;

    /**
     * 接口全限定名
     */
    private String interfaceName;

    /**
     * 接口版本
     */
    private String version;

    /**
     * 接口分组
     */
    private String group;

    /**
     * 接口描述
     */
    private String description;

    /**
     * 提供者服务名
     */
    private String providerServiceName;

    /**
     * 提供者仓库ID
     */
    private String providerWarehouseId;

    /**
     * 方法列表
     */
    private List<DubboMethodInfo> methods = new ArrayList<>();

    /**
     * 消费者服务列表
     */
    private List<ConsumerInfo> consumers = new ArrayList<>();

    /**
     * 是否废弃
     */
    private boolean deprecated;

    /**
     * 废弃原因
     */
    private String deprecatedReason;

    /**
     * 估算大小
     *
     * @return 字符数
     */
    public int estimateSize() {
        int baseSize = 200;
        int methodSize = methods.size() * 200;
        int consumerSize = consumers.size() * 80;
        return baseSize + methodSize + consumerSize;
    }

    /**
     * 获取接口简名
     *
     * @return 简名
     */
    public String getSimpleName() {
        if (interfaceName != null && interfaceName.contains(".")) {
            return interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
        }
        return interfaceName;
    }

    /**
     * 转换为Markdown格式
     *
     * @return Markdown文本
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();

        sb.append("## 接口: ").append(getSimpleName()).append("\n\n");
        sb.append("- **全限定名**: `").append(interfaceName).append("`\n");
        sb.append("- **提供者**: ").append(providerServiceName).append("\n");

        if (version != null && !version.isEmpty()) {
            sb.append("- **版本**: ").append(version).append("\n");
        }
        if (group != null && !group.isEmpty()) {
            sb.append("- **分组**: ").append(group).append("\n");
        }
        if (deprecated) {
            sb.append("- **状态**: 已废弃");
            if (deprecatedReason != null) {
                sb.append(" (").append(deprecatedReason).append(")");
            }
            sb.append("\n");
        }
        sb.append("\n");

        if (description != null && !description.isEmpty()) {
            sb.append(description).append("\n\n");
        }

        if (!methods.isEmpty()) {
            sb.append("### 方法列表\n\n");
            for (DubboMethodInfo method : methods) {
                sb.append("#### ").append(method.getName()).append("\n\n");

                if (method.getDescription() != null) {
                    sb.append(method.getDescription()).append("\n\n");
                }

                sb.append("```java\n");
                sb.append(formatMethodSignature(method)).append("\n");
                sb.append("```\n\n");

                if (method.getParameters() != null && !method.getParameters().isEmpty()) {
                    sb.append("**参数**:\n");
                    for (MethodParameter param : method.getParameters()) {
                        sb.append("- `").append(param.getName()).append("` (")
                                .append(param.getType()).append(")");
                        if (param.getDescription() != null) {
                            sb.append(": ").append(param.getDescription());
                        }
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
            }
        }

        if (!consumers.isEmpty()) {
            sb.append("### 消费者 (").append(consumers.size()).append("个)\n\n");
            for (ConsumerInfo consumer : consumers) {
                sb.append("- ").append(consumer.getServiceName()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatMethodSignature(DubboMethodInfo method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getReturnType() != null ? method.getReturnType() : "void")
                .append(" ")
                .append(method.getName())
                .append("(");

        List<MethodParameter> params = method.getParameters();
        if (params != null && !params.isEmpty()) {
            List<String> paramStrs = new ArrayList<>();
            for (MethodParameter param : params) {
                paramStrs.add(param.getType() + " " + param.getName());
            }
            sb.append(String.join(", ", paramStrs));
        }

        sb.append(")");
        return sb.toString();
    }

    /**
     * 消费者信息
     */
    @Data
    public static class ConsumerInfo {
        private String warehouseId;
        private String serviceName;
        private String sourceClass;
    }
}
