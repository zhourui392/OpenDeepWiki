package ai.opendw.koalawiki.core.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码工具类
 */
public class PasswordUtil {

    /**
     * 使用MD5加密密码（简单实现，生产环境建议使用BCrypt）
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String encryptPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证密码
     *
     * @param rawPassword      原始密码
     * @param encodedPassword  加密后的密码
     * @return true-密码正确，false-密码错误
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encryptPassword(rawPassword).equals(encodedPassword);
    }
}
