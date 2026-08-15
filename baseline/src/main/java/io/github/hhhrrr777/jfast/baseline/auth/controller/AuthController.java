package io.github.hhhrrr777.jfast.baseline.auth.controller;

import io.github.hhhrrr777.jfast.baseline.auth.dto.LoginRequest;
import io.github.hhhrrr777.jfast.baseline.auth.dto.RefreshRequest;
import io.github.hhhrrr777.jfast.baseline.auth.service.AuthService;
import io.github.hhhrrr777.jfast.baseline.auth.vo.TokenResponse;
import io.github.hhhrrr777.jfast.baseline.common.core.AjaxResult;
import io.github.hhhrrr777.jfast.baseline.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口:登录 / 刷新 / 登出 / 当前登录信息。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录", description = "校验防爆破与账号密码,签发 access + refresh 双 token")
    @PostMapping("/login")
    public AjaxResult login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        TokenResponse token = authService.login(request, clientIp(httpRequest));
        return AjaxResult.success(token);
    }

    @Operation(summary = "刷新令牌", description = "凭 refresh token 换新双 token(rotation,旧 refresh 作废)")
    @PostMapping("/refresh")
    public AjaxResult refresh(@Valid @RequestBody RefreshRequest request) {
        return AjaxResult.success(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "登出", description = "删除本端 refresh 行;access token 短效自然过期")
    @PostMapping("/logout")
    public AjaxResult logout(@RequestBody(required = false) RefreshRequest request) {
        authService.logout(request == null ? null : request.getRefreshToken());
        return AjaxResult.success();
    }

    @Operation(summary = "当前登录信息", description = "校验 access token 并返回当前用户(需认证)")
    @GetMapping("/info")
    public AjaxResult currentUser() {
        return AjaxResult.success(authService.currentUser(SecurityUtils.getUserId()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
