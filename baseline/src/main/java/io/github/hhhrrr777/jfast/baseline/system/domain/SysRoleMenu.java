package io.github.hhhrrr777.jfast.baseline.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 角色-菜单关联表 sys_role_menu(联合主键 role_id + menu_id)。
 */
@TableName("sys_role_menu")
public class SysRoleMenu {

    private Long roleId;
    private Long menuId;

    public SysRoleMenu() {
    }

    public SysRoleMenu(Long roleId, Long menuId) {
        this.roleId = roleId;
        this.menuId = menuId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
