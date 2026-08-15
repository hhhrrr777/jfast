package io.github.hhhrrr777.jfast.baseline.auth.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 当前登录用户(认证主体)。承载权限集合与角色标识,由 JwtAuthenticationFilter 每请求从库装载。
 */
public class LoginUser implements Serializable {

    /** 超管全量权限标识。 */
    public static final String ALL_PERMISSION = "*:*:*";
    /** 超管角色 key。 */
    public static final String ROLE_ADMIN = "admin";

    private Long userId;
    private String username;
    private String nickName;
    /** 权限标识集合(超管含 *:*:*)。 */
    private Set<String> permissions = new HashSet<>();
    /** 角色 key 集合。 */
    private Set<String> roles = new HashSet<>();

    public LoginUser() {
    }

    public LoginUser(Long userId, String username, String nickName,
                     Set<String> permissions, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.nickName = nickName;
        this.permissions = permissions == null ? new HashSet<>() : permissions;
        this.roles = roles == null ? new HashSet<>() : roles;
    }

    /** 是否超管(拥有全量权限标识)。 */
    public boolean isAdmin() {
        return permissions.contains(ALL_PERMISSION);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Set<String> getPermissions() {
        return permissions == null ? Collections.emptySet() : permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Set<String> getRoles() {
        return roles == null ? Collections.emptySet() : roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
