package io.github.hhhrrr777.jfast.baseline.auth.model;

import java.io.Serializable;

/**
 * 当前登录用户(认证主体)。S2-2 仅含基本身份;权限集合在 S2-3 权限域接入。
 */
public class LoginUser implements Serializable {

    private Long userId;
    private String username;
    private String nickName;

    public LoginUser() {
    }

    public LoginUser(Long userId, String username, String nickName) {
        this.userId = userId;
        this.username = username;
        this.nickName = nickName;
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
}
