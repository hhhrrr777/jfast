package io.github.hhhrrr777.jfast.baseline.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.hhhrrr777.jfast.baseline.auth.dto.LoginRequest;
import io.github.hhhrrr777.jfast.baseline.auth.vo.CurrentUserVO;
import io.github.hhhrrr777.jfast.baseline.auth.vo.TokenResponse;
import io.github.hhhrrr777.jfast.baseline.common.exception.ServiceException;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysRefreshToken;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUser;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证应用服务:登录(防爆破 + 双 token 签发)、刷新(rotation)、登出(删本端 refresh 行)。
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final LoginFailureGuard loginFailureGuard;

    public AuthService(SysUserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenService refreshTokenService,
                       LoginFailureGuard loginFailureGuard) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.loginFailureGuard = loginFailureGuard;
    }

    /**
     * 登录:校验防爆破锁定 → 校验账号密码 → 签发双 token。
     */
    @Transactional
    public TokenResponse login(LoginRequest request, String loginIp) {
        String username = request.getUsername();
        if (loginFailureGuard.isLocked(username)) {
            long seconds = loginFailureGuard.remainingLockSeconds(username);
            throw new ServiceException("失败次数过多,账号已锁定,请 " + seconds + " 秒后再试");
        }

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, username));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginFailureGuard.recordFailure(username);
            throw new ServiceException("用户名或密码错误");
        }
        if ("1".equals(user.getStatus())) {
            // 停用账号的登录尝试同样计入失败,防止反复探测
            loginFailureGuard.recordFailure(username);
            throw new ServiceException("账号已被停用");
        }

        loginFailureGuard.recordSuccess(username);
        updateLoginInfo(user, loginIp);

        SysRefreshToken refresh = refreshTokenService.issue(user.getUserId(), request.getDeviceId());
        String accessToken = tokenProvider.createAccessToken(user.getUserId(), user.getUserName());
        return buildTokenResponse(accessToken, refresh.getToken());
    }

    /**
     * 刷新:校验 refresh token 有效 → 旋转(旧行作废,签发新双 token)。
     */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        SysRefreshToken current = refreshTokenService.validate(refreshToken);
        if (current == null) {
            throw new ServiceException("refresh token 无效或已过期", 401);
        }
        SysUser user = userMapper.selectById(current.getUserId());
        if (user == null || "1".equals(user.getStatus())) {
            throw new ServiceException("账号不存在或已停用", 401);
        }

        // refresh rotation:旧行作废,按同设备签发新行
        refreshTokenService.revoke(refreshToken);
        SysRefreshToken next = refreshTokenService.issue(user.getUserId(), current.getDeviceId());
        String accessToken = tokenProvider.createAccessToken(user.getUserId(), user.getUserName());
        return buildTokenResponse(accessToken, next.getToken());
    }

    /**
     * 登出:删除本端 refresh 行;access token 短效自然过期,不做黑名单。
     */
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    /**
     * 当前登录用户信息(从库读取,保证昵称等字段为最新)。
     */
    public CurrentUserVO currentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("账号不存在", 401);
        }
        return new CurrentUserVO(user.getUserId(), user.getUserName(), user.getNickName());
    }

    /** 组装双 token 响应。 */
    private TokenResponse buildTokenResponse(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken,
                tokenProvider.getAccessTokenTtlSeconds(),
                refreshToken,
                tokenProvider.getRefreshTokenTtlSeconds());
    }

    private void updateLoginInfo(SysUser user, String loginIp) {
        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setLoginIp(loginIp == null ? "" : loginIp);
        update.setLoginDate(LocalDateTime.now());
        userMapper.updateById(update);
    }
}
