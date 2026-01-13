package ai.opendw.koalawiki.domain.log;

/**
 * 访问动作类型枚举
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
public enum ActionType {

    /**
     * 查看文档
     */
    VIEW("VIEW", "查看"),

    /**
     * 下载文档
     */
    DOWNLOAD("DOWNLOAD", "下载"),

    /**
     * 搜索
     */
    SEARCH("SEARCH", "搜索"),

    /**
     * 创建
     */
    CREATE("CREATE", "创建"),

    /**
     * 更新
     */
    UPDATE("UPDATE", "更新"),

    /**
     * 删除
     */
    DELETE("DELETE", "删除"),

    /**
     * 同步仓库
     */
    SYNC("SYNC", "同步"),

    /**
     * 登录
     */
    LOGIN("LOGIN", "登录"),

    /**
     * 登出
     */
    LOGOUT("LOGOUT", "登出");

    private final String code;
    private final String description;

    ActionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static ActionType fromCode(String code) {
        for (ActionType type : ActionType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
