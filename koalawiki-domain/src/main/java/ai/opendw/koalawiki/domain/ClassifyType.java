package ai.opendw.koalawiki.domain;

/**
 * 项目分类类型
 */
public enum ClassifyType {
    /**
     * 应用系统
     */
    APPLICATIONS("Applications", "应用系统"),

    /**
     * 开发框架
     */
    FRAMEWORKS("Frameworks", "开发框架"),

    /**
     * 库和组件
     */
    LIBRARIES("Libraries", "库和组件"),

    /**
     * 开发工具
     */
    DEVELOPMENT_TOOLS("DevelopmentTools", "开发工具"),

    /**
     * 命令行工具
     */
    CLI_TOOLS("CLITools", "命令行工具"),

    /**
     * DevOps配置
     */
    DEVOPS_CONFIGURATION("DevOpsConfiguration", "DevOps配置"),

    /**
     * 文档
     */
    DOCUMENTATION("Documentation", "文档");

    private final String code;
    private final String description;

    ClassifyType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ClassifyType fromCode(String code) {
        for (ClassifyType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown classify type: " + code);
    }
}
