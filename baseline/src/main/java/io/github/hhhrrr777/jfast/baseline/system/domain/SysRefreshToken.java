package io.github.hhhrrr777.jfast.baseline.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 登录刷新令牌表 sys_refresh_token。双 token 方案中 refresh 落库、按(用户, 设备)多行、可吊销。
 */
@TableName("sys_refresh_token")
public class SysRefreshToken {

    @TableId(type = IdType.AUTO)
    private Long tokenId;

    /** 用户ID。 */
    private Long userId;

    /** 设备/会话标识。 */
    private String deviceId;

    /** refresh token 值(唯一)。 */
    private String token;

    /** 过期时间。 */
    private LocalDateTime expireTime;

    /** 是否吊销(0正常 1吊销)。 */
    private String revoked;

    private LocalDateTime createTime;

    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public String getRevoked() {
        return revoked;
    }

    public void setRevoked(String revoked) {
        this.revoked = revoked;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
