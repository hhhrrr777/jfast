package io.github.hhhrrr777.jfast.baseline.auth.vo;

/**
 * 当前登录用户信息(响应契约)。与认证主体 LoginUser 解耦,便于后续按需扩展字段。
 */
public class CurrentUserVO {

    private Long userId;
    private String username;
    private String nickName;

    public CurrentUserVO() {
    }

    public CurrentUserVO(Long userId, String username, String nickName) {
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
