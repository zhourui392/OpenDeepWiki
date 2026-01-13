package ai.opendw.koalawiki.core.context;

/**
 * 用户上下文接口
 * 用于获取当前请求的用户信息
 */
public interface UserContext {

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，未登录返回null
     */
    String getUserId();

    /**
     * 获取当前用户名
     *
     * @return 用户名，未登录返回null
     */
    String getUsername();

    /**
     * 获取当前用户邮箱
     *
     * @return 邮箱，未登录返回null
     */
    String getEmail();

    /**
     * 判断是否已登录
     *
     * @return true-已登录，false-未登录
     */
    boolean isAuthenticated();

    /**
     * 判断是否拥有指定角色
     *
     * @param role 角色名称
     * @return true-拥有该角色，false-不拥有
     */
    boolean hasRole(String role);

    /**
     * 设置当前用户信息
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param email    邮箱
     */
    void setUser(String userId, String username, String email);

    /**
     * 清除当前用户信息
     */
    void clear();
}
