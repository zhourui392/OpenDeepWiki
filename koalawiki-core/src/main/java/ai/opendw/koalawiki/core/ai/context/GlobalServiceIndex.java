package ai.opendw.koalawiki.core.ai.context;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局服务索引（L1层）
 * 提供集群级别的服务概览，约2KB
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class GlobalServiceIndex {

    /**
     * 集群ID
     */
    private String clusterId;

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 技术栈
     */
    private String techStack;

    /**
     * 服务总数
     */
    private int totalServices;

    /**
     * 接口总数
     */
    private int totalInterfaces;

    /**
     * 领域列表
     */
    private List<DomainBrief> domains = new ArrayList<>();

    /**
     * 服务清单
     */
    private List<ServiceBrief> services = new ArrayList<>();

    /**
     * 热门接口 Top N
     */
    private List<InterfaceBrief> hotInterfaces = new ArrayList<>();

    /**
     * 估算大小
     *
     * @return 字符数
     */
    public int estimateSize() {
        int baseSize = 200;
        int domainSize = domains.size() * 50;
        int serviceSize = services.size() * 80;
        int interfaceSize = hotInterfaces.size() * 100;
        return baseSize + domainSize + serviceSize + interfaceSize;
    }

    /**
     * 转换为Markdown格式
     *
     * @return Markdown文本
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();

        sb.append("## 集群概览\n\n");
        sb.append("- **集群名称**: ").append(clusterName).append("\n");
        sb.append("- **技术栈**: ").append(techStack).append("\n");
        sb.append("- **服务总数**: ").append(totalServices).append("\n");
        sb.append("- **接口总数**: ").append(totalInterfaces).append("\n\n");

        if (!domains.isEmpty()) {
            sb.append("### 业务领域\n\n");
            sb.append("| 领域 | 编码 | 服务数 |\n");
            sb.append("|------|------|--------|\n");
            for (DomainBrief domain : domains) {
                sb.append("| ").append(domain.getName())
                        .append(" | ").append(domain.getCode())
                        .append(" | ").append(domain.getServiceCount())
                        .append(" |\n");
            }
            sb.append("\n");
        }

        if (!services.isEmpty()) {
            sb.append("### 服务清单\n\n");
            sb.append("| 服务名 | 领域 | 接口数 | 说明 |\n");
            sb.append("|--------|------|--------|------|\n");
            for (ServiceBrief service : services) {
                sb.append("| ").append(service.getName())
                        .append(" | ").append(service.getDomainName())
                        .append(" | ").append(service.getInterfaceCount())
                        .append(" | ").append(truncate(service.getDescription(), 50))
                        .append(" |\n");
            }
            sb.append("\n");
        }

        if (!hotInterfaces.isEmpty()) {
            sb.append("### 热门接口 Top ").append(hotInterfaces.size()).append("\n\n");
            for (InterfaceBrief iface : hotInterfaces) {
                sb.append("- `").append(iface.getInterfaceName())
                        .append("` (").append(iface.getProviderServiceName())
                        .append(", ").append(iface.getConsumerCount()).append("个消费者)\n");
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
}
