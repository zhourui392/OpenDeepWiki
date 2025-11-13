package ai.opendw.koalawiki.core.context;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户上下文实现
 * 基于Request Scope，每个请求都有独立的实例
 */
@Component
@RequestScope
public class UserContextImpl implements UserContext {

    private String userId;
    private String username;
    private String email;
    private Set<String> roles = new HashSet<>();

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public boolean isAuthenticated() {
        return userId != null && !userId.isEmpty();
    }

    @Override
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public void setUser(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    /**
     * 添加角色
     *
     * @param role 角色名称
     */
    public void addRole(String role) {
        this.roles.add(role);
    }

    /**
     * 设置角色集合
     *
     * @param roles 角色集合
     */
    public void setRoles(Set<String> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }

    @Override
    public void clear() {
        this.userId = null;
        this.username = null;
        this.email = null;
        this.roles.clear();
    }
}
