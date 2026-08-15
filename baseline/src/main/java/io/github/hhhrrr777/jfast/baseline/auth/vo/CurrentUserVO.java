package io.github.hhhrrr777.jfast.baseline.auth.vo;

import java.util.Set;

/**
 * 当前登录用户信息(响应契约)。与认证主体 LoginUser 解耦,便于后续按需扩展字段。
 */
public class CurrentUserVO {

    private Long userId;
    private String username;
    private String nickName;
    /** 角色 key 集合。 */
    private Set<String> roles;
    /** 权限标识集合(前端 v-hasPermi 消费)。 */
    private Set<String> permissions;

    public CurrentUserVO() {
    }

    public CurrentUserVO(Long userId, String username, String nickName,
                         Set<String> roles, Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.nickName = nickName;
        this.roles = roles;
        this.permissions = permissions;
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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
