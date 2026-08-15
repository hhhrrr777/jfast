package io.github.hhhrrr777.jfast.baseline.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户-角色关联表 sys_user_role(联合主键 user_id + role_id)。
 */
@TableName("sys_user_role")
public class SysUserRole {

    private Long userId;
    private Long roleId;

    public SysUserRole() {
    }

    public SysUserRole(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
