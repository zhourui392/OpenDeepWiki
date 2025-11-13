package ai.opendw.koalawiki.domain.warehouse;

/**
 * 仓库同步触发方式枚举
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
public enum WarehouseSyncTrigger {

    /**
     * 自动触发
     */
    AUTO("Auto", "自动触发"),

    /**
     * 手动触发
     */
    MANUAL("Manual", "手动触发");

    private final String code;
    private final String description;

    WarehouseSyncTrigger(String code, String description) {
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
     * 根据代码获取枚举值
     */
    public static WarehouseSyncTrigger fromCode(String code) {
        for (WarehouseSyncTrigger trigger : values()) {
            if (trigger.code.equals(code)) {
                return trigger;
            }
        }
        throw new IllegalArgumentException("Unknown WarehouseSyncTrigger code: " + code);
    }
}