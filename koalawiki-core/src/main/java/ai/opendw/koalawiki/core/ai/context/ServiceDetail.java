package ai.opendw.koalawiki.core.ai.context;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务详情（L3层）
 * 提供服务级别的完整信息，约10KB
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class ServiceDetail {

    /**
     * 服务ID（仓库ID）
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
     * 技术栈
     */
    private String techStack;

    /**
     * 负责人
     */
    private String owner;

    /**
     * Git仓库地址
     */
    private String repositoryUrl;

    /**
     * 提供的Dubbo接口
     */
    private List<InterfaceInfo> providedInterfaces = new ArrayList<>();

    /**
     * 依赖的Dubbo接口
     */
    private List<InterfaceInfo> consumedInterfaces = new ArrayList<>();

    /**
     * 核心领域模型
     */
    private List<DomainModelInfo> domainModels = new ArrayList<>();

    /**
     * 数据库表摘要
     */
    private List<TableInfo> tables = new ArrayList<>();

    /**
     * 配置项说明
     */
    private List<ConfigItem> configItems = new ArrayList<>();

    /**
     * REST API端点
     */
    private List<EndpointInfo> restEndpoints = new ArrayList<>();

    /**
     * 估算大小
     *
     * @return 字符数
     */
    public int estimateSize() {
        int baseSize = 500;
        int providedSize = providedInterfaces.size() * 300;
        int consumedSize = consumedInterfaces.size() * 150;
        int modelSize = domainModels.size() * 200;
        int tableSize = tables.size() * 100;
        int configSize = configItems.size() * 80;
        int endpointSize = restEndpoints.size() * 120;
        return baseSize + providedSize + consumedSize + modelSize + tableSize + configSize + endpointSize;
    }

    /**
     * 转换为Markdown格式
     *
     * @return Markdown文本
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();

        sb.append("## 服务: ").append(serviceName).append("\n\n");
        sb.append("- **所属领域**: ").append(domainName).append("\n");
        sb.append("- **负责人**: ").append(owner != null ? owner : "未指定").append("\n");

        if (description != null && !description.isEmpty()) {
            sb.append("- **描述**: ").append(description).append("\n");
        }
        if (techStack != null && !techStack.isEmpty()) {
            sb.append("- **技术栈**: ").append(techStack).append("\n");
        }
        sb.append("\n");

        if (!providedInterfaces.isEmpty()) {
            sb.append("### 提供的Dubbo接口\n\n");
            for (InterfaceInfo iface : providedInterfaces) {
                sb.append("#### ").append(iface.getSimpleName()).append("\n\n");
                if (iface.getDescription() != null) {
                    sb.append(iface.getDescription()).append("\n\n");
                }
                sb.append("```java\n");
                sb.append("public interface ").append(iface.getSimpleName()).append(" {\n");
                for (MethodSignature method : iface.getMethods()) {
                    sb.append("    ").append(method.toSignature()).append(";\n");
                }
                sb.append("}\n");
                sb.append("```\n\n");
            }
        }

        if (!consumedInterfaces.isEmpty()) {
            sb.append("### 依赖的Dubbo接口\n\n");
            for (InterfaceInfo iface : consumedInterfaces) {
                sb.append("- `").append(iface.getInterfaceName()).append("`");
                sb.append(" (来自 ").append(iface.getProviderServiceName()).append(")\n");
            }
            sb.append("\n");
        }

        if (!domainModels.isEmpty()) {
            sb.append("### 核心领域模型\n\n");
            for (DomainModelInfo model : domainModels) {
                sb.append("- **").append(model.getClassName()).append("**: ")
                        .append(model.getDescription()).append("\n");
            }
            sb.append("\n");
        }

        if (!tables.isEmpty()) {
            sb.append("### 数据库表\n\n");
            sb.append("| 表名 | 说明 | 字段数 |\n");
            sb.append("|------|------|--------|\n");
            for (TableInfo table : tables) {
                sb.append("| ").append(table.getTableName())
                        .append(" | ").append(table.getComment())
                        .append(" | ").append(table.getColumnCount())
                        .append(" |\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 接口信息
     */
    @Data
    public static class InterfaceInfo {
        private String interfaceName;
        private String simpleName;
        private String description;
        private String providerServiceName;
        private List<MethodSignature> methods = new ArrayList<>();
        private boolean deprecated;

        public String getSimpleName() {
            if (simpleName != null) {
                return simpleName;
            }
            if (interfaceName != null && interfaceName.contains(".")) {
                return interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
            }
            return interfaceName;
        }
    }

    /**
     * 方法签名
     */
    @Data
    public static class MethodSignature {
        private String name;
        private String returnType;
        private List<String> parameterTypes = new ArrayList<>();
        private String description;

        public String toSignature() {
            StringBuilder sb = new StringBuilder();
            sb.append(returnType != null ? returnType : "void")
                    .append(" ")
                    .append(name)
                    .append("(");

            if (parameterTypes.size() > 3) {
                sb.append(parameterTypes.get(0)).append(", ")
                        .append(parameterTypes.get(1)).append(", ...");
            } else {
                sb.append(String.join(", ", parameterTypes));
            }

            sb.append(")");
            return sb.toString();
        }
    }

    /**
     * 领域模型信息
     */
    @Data
    public static class DomainModelInfo {
        private String className;
        private String description;
        private List<String> fields = new ArrayList<>();
    }

    /**
     * 表信息
     */
    @Data
    public static class TableInfo {
        private String tableName;
        private String comment;
        private int columnCount;
    }

    /**
     * 配置项
     */
    @Data
    public static class ConfigItem {
        private String key;
        private String defaultValue;
        private String description;
    }

    /**
     * REST端点信息
     */
    @Data
    public static class EndpointInfo {
        private String method;
        private String path;
        private String description;
    }
}
