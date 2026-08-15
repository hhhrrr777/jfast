package io.github.hhhrrr777.jfast.baseline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证域常量配置。值见 application.yml 的 jfast.security.*,均为模板可改常量。
 */
@Component
@ConfigurationProperties(prefix = "jfast.security")
public class SecurityProperties {

    /** JWT 签名密钥(HS256)。 */
    private String jwtSecret;

    /** access token 有效期(秒),默认 2 小时。 */
    private long accessTokenTtlSeconds = 7200;

    /** refresh token 有效期(秒),默认 7 天。 */
    private long refreshTokenTtlSeconds = 604800;

    /** 连续登录失败锁定阈值,默认 5 次。 */
    private int loginMaxFailCount = 5;

    /** 登录锁定时长(秒),默认 10 分钟。 */
    private long loginLockDurationSeconds = 600;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public int getLoginMaxFailCount() {
        return loginMaxFailCount;
    }

    public void setLoginMaxFailCount(int loginMaxFailCount) {
        this.loginMaxFailCount = loginMaxFailCount;
    }

    public long getLoginLockDurationSeconds() {
        return loginLockDurationSeconds;
    }

    public void setLoginLockDurationSeconds(long loginLockDurationSeconds) {
        this.loginLockDurationSeconds = loginLockDurationSeconds;
    }
}
