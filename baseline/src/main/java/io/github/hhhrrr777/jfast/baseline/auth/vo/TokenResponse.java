package io.github.hhhrrr777.jfast.baseline.auth.vo;

/**
 * 登录/刷新成功的令牌响应。双 token 契约:accessToken 短效,refreshToken 长效。
 */
public class TokenResponse {

    private String accessToken;

    /** access token 有效期(秒)。 */
    private long expiresIn;

    private String refreshToken;

    /** refresh token 有效期(秒)。 */
    private long refreshExpiresIn;

    public TokenResponse() {
    }

    public TokenResponse(String accessToken, long expiresIn, String refreshToken, long refreshExpiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(long refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }
}
