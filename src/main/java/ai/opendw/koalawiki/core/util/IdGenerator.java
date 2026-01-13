package ai.opendw.koalawiki.core.util;

import java.util.UUID;

/**
 * ID生成工具类
 */
public class IdGenerator {

    /**
     * 生成UUID（去除横杠）
     *
     * @return 32位UUID字符串
     */
    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成标准UUID
     *
     * @return 36位UUID字符串（含横杠）
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
