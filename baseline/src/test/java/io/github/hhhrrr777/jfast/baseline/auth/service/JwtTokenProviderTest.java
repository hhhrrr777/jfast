package io.github.hhhrrr777.jfast.baseline.auth.service;

import io.github.hhhrrr777.jfast.baseline.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 纯逻辑单测:签发/解析往返、负载字段、过期与类型校验、密钥隔离。
 */
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key-0123456789";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.setJwtSecret(SECRET);
        properties.setAccessTokenTtlSeconds(7200);
        properties.setRefreshTokenTtlSeconds(604800);
        provider = new JwtTokenProvider(properties);
    }

    @Test
    void createAndParseRoundTrip() {
        String token = provider.createAccessToken(1L, "admin");
        Claims claims = provider.parseAccessToken(token);
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("username", String.class)).isEqualTo("admin");
        assertThat(claims.get(JwtTokenProvider.CLAIM_TOKEN_TYPE, String.class))
                .isEqualTo(JwtTokenProvider.TYPE_ACCESS);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void rejectsTamperedToken() {
        String token = provider.createAccessToken(1L, "admin");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThatThrownBy(() -> provider.parseAccessToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        SecurityProperties other = new SecurityProperties();
        other.setJwtSecret("another-secret-key-another-secret-key-another-secret-98");
        JwtTokenProvider otherProvider = new JwtTokenProvider(other);
        String foreignToken = otherProvider.createAccessToken(1L, "admin");
        assertThatThrownBy(() -> provider.parseAccessToken(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        SecurityProperties properties = new SecurityProperties();
        properties.setJwtSecret(SECRET);
        properties.setAccessTokenTtlSeconds(-10); // 已过期
        JwtTokenProvider expired = new JwtTokenProvider(properties);
        String token = expired.createAccessToken(1L, "admin");
        assertThatThrownBy(() -> provider.parseAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void exposesConfiguredTtls() {
        assertThat(provider.getAccessTokenTtlSeconds()).isEqualTo(7200);
        assertThat(provider.getRefreshTokenTtlSeconds()).isEqualTo(604800);
    }
}
