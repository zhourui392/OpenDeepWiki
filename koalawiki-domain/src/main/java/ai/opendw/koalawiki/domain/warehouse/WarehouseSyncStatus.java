package ai.opendw.koalawiki.domain.warehouse;

/**
 * 仓库同步状态枚举
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
public enum WarehouseSyncStatus {

    /**
     * 待处理
     */
    PENDING("Pending", "待处理"),

    /**
     * 同步中
     */
    IN_PROGRESS("InProgress", "同步中"),

    /**
     * 同步成功
     */
    SUCCESS("Success", "同步成功"),

    /**
     * 已取消
     */
    CANCELLED("Cancelled", "已取消"),

    /**
     * 同步失败
     */
    FAILED("Failed", "同步失败");

    private final String code;
    private final String description;

    WarehouseSyncStatus(String code, String description) {
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
    public static WarehouseSyncStatus fromCode(String code) {
        for (WarehouseSyncStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown WarehouseSyncStatus code: " + code);
    }
}