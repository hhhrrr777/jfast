package io.github.hhhrrr777.jfast.baseline.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.hhhrrr777.jfast.baseline.auth.model.LoginUser;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysMenu;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysRole;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUser;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUserRole;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysMenuMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysRoleMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限集合装配:按用户角色联查菜单 perms,超管(admin 角色)直接给全量标识。
 * 每请求由 JwtAuthenticationFilter 调用,停用角色/停用菜单即时生效。
 */
@Service
public class SysPermissionService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public SysPermissionService(SysUserMapper userMapper,
                                SysRoleMapper roleMapper,
                                SysMenuMapper menuMapper,
                                SysUserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 装配当前登录用户:从库读用户最新状态(停用/删除即时失效),挂角色 key 与权限标识集合。
     * 用户不存在或已删除时返回 null(等同未认证)。
     */
    public LoginUser loadLoginUser(Long userId, String username) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || "1".equals(user.getStatus())) {
            return null;
        }

        Set<String> roles = loadRoleKeys(userId);
        Set<String> permissions;
        if (roles.contains(LoginUser.ROLE_ADMIN)) {
            permissions = new HashSet<>();
            permissions.add(LoginUser.ALL_PERMISSION);
        } else {
            permissions = loadPermissions(userId);
        }
        return new LoginUser(userId, username, user.getNickName(), permissions, roles);
    }

    /** 用户启用的角色 key 集合(停用的角色不算)。 */
    public Set<String> loadRoleKeys(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        Set<String> keys = new HashSet<>();
        if (roleIds.isEmpty()) {
            return keys;
        }
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, "0")
                .in(SysRole::getRoleId, roleIds));
        for (SysRole role : roles) {
            keys.add(role.getRoleKey());
        }
        return keys;
    }

    /** 用户启用的权限标识集合(经角色 ⋈ 角色菜单 ⋈ 菜单联查)。 */
    public Set<String> loadPermissions(Long userId) {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId);
        Set<String> perms = new HashSet<>();
        for (SysMenu menu : menus) {
            if (menu.getPerms() != null && !menu.getPerms().isBlank()) {
                perms.add(menu.getPerms());
            }
        }
        return perms;
    }
}
