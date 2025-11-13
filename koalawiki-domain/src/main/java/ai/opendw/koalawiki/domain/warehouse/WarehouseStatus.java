package ai.opendw.koalawiki.domain.warehouse;

/**
 * 仓库状态枚举
 */
public enum WarehouseStatus {
    /**
     * 待处理
     */
    PENDING((byte) 0, "待处理"),

    /**
     * 处理中
     */
    PROCESSING((byte) 1, "处理中"),

    /**
     * 已完成
     */
    COMPLETED((byte) 2, "已完成"),

    /**
     * 就绪
     */
    READY((byte) 3, "就绪"),

    /**
     * 已取消
     */
    CANCELED((byte) 4, "已取消"),

    /**
     * 未授权
     */
    UNAUTHORIZED((byte) 5, "未授权"),

    /**
     * 错误
     */
    ERROR((byte) 98, "错误"),

    /**
     * 已失败
     */
    FAILED((byte) 99, "已失败");

    private final byte code;
    private final String description;

    WarehouseStatus(byte code, String description) {
        this.code = code;
        this.description = description;
    }

    public byte getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static WarehouseStatus fromCode(byte code) {
        for (WarehouseStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown warehouse status code: " + code);
    }
}
