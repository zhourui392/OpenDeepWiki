package ai.opendw.koalawiki.core.ai.context;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 领域摘要（L2层）
 * 提供领域级别的服务和接口概览，约5KB
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class DomainSummary {

    /**
     * 领域ID
     */
    private String domainId;

    /**
     * 领域名称
     */
    private String domainName;

    /**
     * 领域编码
     */
    private String domainCode;

    /**
     * 领域描述
     */
    private String description;

    /**
     * 领域负责人
     */
    private String owner;

    /**
     * 职责边界说明
     */
    private String responsibilityBoundary;

    /**
     * 领域内服务列表
     */
    private List<ServiceBrief> services = new ArrayList<>();

    /**
     * 领域核心接口清单
     */
    private List<InterfaceBrief> coreInterfaces = new ArrayList<>();

    /**
     * 对外依赖的领域
     */
    private List<DomainDependency> outboundDependencies = new ArrayList<>();

    /**
     * 被依赖的领域
     */
    private List<DomainDependency> inboundDependencies = new ArrayList<>();

    /**
     * 核心业务流程摘要
     */
    private List<String> coreBusinessFlows = new ArrayList<>();

    /**
     * 估算大小
     *
     * @return 字符数
     */
    public int estimateSize() {
        int baseSize = 300;
        int serviceSize = services.size() * 150;
        int interfaceSize = coreInterfaces.size() * 120;
        int depSize = (outboundDependencies.size() + inboundDependencies.size()) * 50;
        int flowSize = coreBusinessFlows.size() * 100;
        return baseSize + serviceSize + interfaceSize + depSize + flowSize;
    }

    /**
     * 转换为Markdown格式
     *
     * @return Markdown文本
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();

        sb.append("## 领域: ").append(domainName).append("\n\n");
        sb.append("- **编码**: ").append(domainCode).append("\n");
        sb.append("- **负责人**: ").append(owner != null ? owner : "未指定").append("\n");

        if (description != null && !description.isEmpty()) {
            sb.append("- **描述**: ").append(description).append("\n");
        }
        if (responsibilityBoundary != null && !responsibilityBoundary.isEmpty()) {
            sb.append("- **职责边界**: ").append(responsibilityBoundary).append("\n");
        }
        sb.append("\n");

        if (!services.isEmpty()) {
            sb.append("### 领域内服务\n\n");
            sb.append("| 服务名 | 类型 | 接口数 | 说明 |\n");
            sb.append("|--------|------|--------|------|\n");
            for (ServiceBrief service : services) {
                sb.append("| ").append(service.getName())
                        .append(" | ").append(service.getServiceType())
                        .append(" | ").append(service.getInterfaceCount())
                        .append(" | ").append(truncate(service.getDescription(), 40))
                        .append(" |\n");
            }
            sb.append("\n");
        }

        if (!coreInterfaces.isEmpty()) {
            sb.append("### 核心接口\n\n");
            for (InterfaceBrief iface : coreInterfaces) {
                sb.append("- `").append(iface.getSimpleName()).append("`");
                if (iface.getDescription() != null) {
                    sb.append(" - ").append(truncate(iface.getDescription(), 60));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!outboundDependencies.isEmpty()) {
            sb.append("### 对外依赖\n\n");
            for (DomainDependency dep : outboundDependencies) {
                sb.append("- ").append(dep.getDomainName())
                        .append(" (").append(dep.getInterfaceCount()).append("个接口)\n");
            }
            sb.append("\n");
        }

        if (!coreBusinessFlows.isEmpty()) {
            sb.append("### 核心业务流程\n\n");
            for (String flow : coreBusinessFlows) {
                sb.append("- ").append(flow).append("\n");
            }
            sb.append("\n");
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
     * 领域依赖关系
     */
    @Data
    public static class DomainDependency {
        private String domainId;
        private String domainName;
        private int interfaceCount;
        private List<String> interfaceNames = new ArrayList<>();
    }
}
