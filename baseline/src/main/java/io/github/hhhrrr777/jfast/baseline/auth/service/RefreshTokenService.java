package io.github.hhhrrr777.jfast.baseline.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysRefreshToken;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysRefreshTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * refresh token 落库服务:按(用户, 设备)多行存储、同设备重登覆盖、可吊销、过期校验。
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String REVOKED_NO = "0";
    private static final String REVOKED_YES = "1";

    private final SysRefreshTokenMapper mapper;
    private final JwtTokenProvider tokenProvider;

    public RefreshTokenService(SysRefreshTokenMapper mapper, JwtTokenProvider tokenProvider) {
        this.mapper = mapper;
        this.tokenProvider = tokenProvider;
    }

    /**
     * 为(用户, 设备)签发新 refresh token 并落库;同设备旧行先删除(重登覆盖)。
     */
    @Transactional
    public SysRefreshToken issue(Long userId, String deviceId) {
        String device = normalize(deviceId);
        // 同设备重登覆盖:先删旧行
        mapper.delete(new LambdaQueryWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getUserId, userId)
                .eq(SysRefreshToken::getDeviceId, device));

        SysRefreshToken row = new SysRefreshToken();
        row.setUserId(userId);
        row.setDeviceId(device);
        row.setToken(newOpaqueToken());
        row.setExpireTime(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenTtlSeconds()));
        row.setRevoked(REVOKED_NO);
        row.setCreateTime(LocalDateTime.now());
        mapper.insert(row);
        return row;
    }

    /**
     * 校验 refresh token 有效性:存在、未吊销、未过期。无效返回 null。
     */
    public SysRefreshToken validate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        SysRefreshToken row = mapper.selectOne(new LambdaQueryWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getToken, token));
        if (row == null) {
            return null;
        }
        if (REVOKED_YES.equals(row.getRevoked())) {
            return null;
        }
        if (row.getExpireTime() == null || row.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        return row;
    }

    /**
     * 吊销(删除)指定 refresh token 行——登出删本端行。
     */
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        mapper.delete(new LambdaQueryWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getToken, token));
    }

    /**
     * 吊销某用户全部 refresh token(如改密/踢下线)。
     */
    public void revokeAllForUser(Long userId) {
        mapper.update(null, new LambdaUpdateWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getUserId, userId)
                .set(SysRefreshToken::getRevoked, REVOKED_YES));
    }

    private String normalize(String deviceId) {
        return (deviceId == null || deviceId.isBlank()) ? "default" : deviceId;
    }

    private String newOpaqueToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
