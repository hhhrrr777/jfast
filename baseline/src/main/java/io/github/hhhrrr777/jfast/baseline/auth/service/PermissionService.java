package io.github.hhhrrr777.jfast.baseline.auth.service;

import io.github.hhhrrr777.jfast.baseline.auth.model.LoginUser;
import io.github.hhhrrr777.jfast.baseline.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * 按钮级权限校验(SpEL Bean,命名 "perm"):供 @PreAuthorize("@perm.hasPermi('system:user:add')") 引用。
 * 安全边界在后端;前端 v-hasPermi 仅做展示裁剪。
 */
@Service("perm")
public class PermissionService {

    /**
     * 当前用户是否持有指定权限标识;超管(*:*:*)全通过。
     */
    public boolean hasPermi(String permission) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        if (loginUser.isAdmin()) {
            return true;
        }
        return loginUser.getPermissions().contains(permission);
    }

    /**
     * 当前用户是否持有任一指定权限标识。
     */
    public boolean hasAnyPermi(String... permissions) {
        for (String permission : permissions) {
            if (hasPermi(permission)) {
                return true;
            }
        }
        return false;
    }
}
