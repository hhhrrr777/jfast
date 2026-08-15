package io.github.hhhrrr777.jfast.baseline.system.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysMenu;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysRole;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysRoleMenu;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUser;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUserRole;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysMenuMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysRoleMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysRoleMenuMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限域种子数据(幂等,已存在则跳过,不覆盖人工修改):
 *  - 种子管理员 admin/admin123(userId=1,绑超管角色)——文档提示首登改密;
 *  - 超管角色(全量)与普通角色(无菜单绑定,供演示最小权限);
 *  - 系统管理目录 + 用户/角色/菜单三菜单 + 各自按钮 perms;
 *  - 「业务功能」目录(id 固定 2000,实体建模默认挂载点,ADR-0003)。
 */
@Component
public class SeedDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataInitializer.class);

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin123";

    /** 种子菜单固定 id 段(避开自增,防止与未来种子/生成物冲突)。 */
    public static final long MENU_SYSTEM_DIR = 1L;
    public static final long MENU_USER = 100L;
    public static final long MENU_ROLE = 101L;
    public static final long MENU_MENU = 102L;
    public static final long MENU_BUSINESS_DIR = SysMenu.BUSINESS_DIR_ID;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PasswordEncoder passwordEncoder;

    public SeedDataInitializer(SysUserMapper userMapper,
                               SysRoleMapper roleMapper,
                               SysMenuMapper menuMapper,
                               SysUserRoleMapper userRoleMapper,
                               SysRoleMenuMapper roleMenuMapper,
                               PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdminUser();
        Long adminRoleId = seedRoles();
        seedMenus();
        bindAdminRoleMenus(adminRoleId);
    }

    /** 种子管理员:幂等;userId=1 由自增保证(首插)。 */
    private void seedAdminUser() {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, ADMIN_USERNAME));
        if (count != null && count > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUserName(ADMIN_USERNAME);
        admin.setNickName("系统管理员");
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setStatus("0");
        admin.setCreateBy("system");
        admin.setCreateTime(LocalDateTime.now());
        admin.setRemark("种子管理员,首次登录后请修改密码");
        userMapper.insert(admin);
        log.info("已创建种子管理员账号 admin(默认密码 admin123,请首次登录后修改)");
    }

    /** 超管角色(admin,全量标识)与普通角色(common,无绑定)。返回超管角色 id。 */
    private Long seedRoles() {
        Long adminRoleId = ensureRole("超级管理员", "admin", 1, "拥有全部权限(不可删除)");
        ensureRole("普通角色", "common", 2, "无任何权限,作为最小权限演示");
        return adminRoleId;
    }

    private Long ensureRole(String name, String key, int sort, String remark) {
        SysRole exists = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, key));
        if (exists != null) {
            return exists.getRoleId();
        }
        SysRole role = new SysRole();
        role.setRoleName(name);
        role.setRoleKey(key);
        role.setRoleSort(sort);
        role.setStatus("0");
        role.setCreateBy("system");
        role.setCreateTime(LocalDateTime.now());
        role.setRemark(remark);
        roleMapper.insert(role);
        return role.getRoleId();
    }

    /** admin 用户绑超管角色(幂等)。 */
    private void bindAdminRoleMenus(Long adminRoleId) {
        SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, ADMIN_USERNAME));
        if (admin != null) {
            Long bound = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, admin.getUserId())
                    .eq(SysUserRole::getRoleId, adminRoleId));
            if (bound == null || bound == 0) {
                userRoleMapper.insert(new SysUserRole(admin.getUserId(), adminRoleId));
            }
        }
        // 超管运行期按 *:*:* 全量放行,不依赖 sys_role_menu 行;但绑定关系仍落库,
        // 保证角色管理页勾选树与权限装配链路一致(权限集合亦含全量标识)。
        Long boundMenus = roleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, adminRoleId));
        if (boundMenus == null || boundMenus == 0) {
            List<SysMenu> all = menuMapper.selectList(null);
            for (SysMenu menu : all) {
                roleMenuMapper.insert(new SysRoleMenu(adminRoleId, menu.getMenuId()));
            }
        }
    }

    /** 系统管理目录 + 三管理菜单 + 按钮 perms + 「业务功能」目录。 */
    private void seedMenus() {
        // 目录:系统管理
        ensureMenu(MENU_SYSTEM_DIR, "系统管理", 0L, 1, "system", "", SysMenu.TYPE_DIR, "", "system");
        // 菜单:用户/角色/菜单管理
        ensureMenu(MENU_USER, "用户管理", MENU_SYSTEM_DIR, 1, "user", "system/user/index", SysMenu.TYPE_MENU, "system:user:list", "user");
        ensureMenu(MENU_ROLE, "角色管理", MENU_SYSTEM_DIR, 2, "role", "system/role/index", SysMenu.TYPE_MENU, "system:role:list", "peoples");
        ensureMenu(MENU_MENU, "菜单管理", MENU_SYSTEM_DIR, 3, "menu", "system/menu/index", SysMenu.TYPE_MENU, "system:menu:list", "tree-table");
        // 用户管理按钮
        ensureMenu(1100L, "用户查询", MENU_USER, 1, "", "", SysMenu.TYPE_BUTTON, "system:user:query", "#");
        ensureMenu(1101L, "用户新增", MENU_USER, 2, "", "", SysMenu.TYPE_BUTTON, "system:user:add", "#");
        ensureMenu(1102L, "用户修改", MENU_USER, 3, "", "", SysMenu.TYPE_BUTTON, "system:user:edit", "#");
        ensureMenu(1103L, "用户删除", MENU_USER, 4, "", "", SysMenu.TYPE_BUTTON, "system:user:remove", "#");
        ensureMenu(1104L, "重置密码", MENU_USER, 5, "", "", SysMenu.TYPE_BUTTON, "system:user:resetPwd", "#");
        // 角色管理按钮
        ensureMenu(1110L, "角色查询", MENU_ROLE, 1, "", "", SysMenu.TYPE_BUTTON, "system:role:query", "#");
        ensureMenu(1111L, "角色新增", MENU_ROLE, 2, "", "", SysMenu.TYPE_BUTTON, "system:role:add", "#");
        ensureMenu(1112L, "角色修改", MENU_ROLE, 3, "", "", SysMenu.TYPE_BUTTON, "system:role:edit", "#");
        ensureMenu(1113L, "角色删除", MENU_ROLE, 4, "", "", SysMenu.TYPE_BUTTON, "system:role:remove", "#");
        // 菜单管理按钮
        ensureMenu(1120L, "菜单查询", MENU_MENU, 1, "", "", SysMenu.TYPE_BUTTON, "system:menu:query", "#");
        ensureMenu(1121L, "菜单新增", MENU_MENU, 2, "", "", SysMenu.TYPE_BUTTON, "system:menu:add", "#");
        ensureMenu(1122L, "菜单修改", MENU_MENU, 3, "", "", SysMenu.TYPE_BUTTON, "system:menu:edit", "#");
        ensureMenu(1123L, "菜单删除", MENU_MENU, 4, "", "", SysMenu.TYPE_BUTTON, "system:menu:remove", "#");
        // 目录:业务功能(实体建模默认挂载点,固定 id)
        ensureMenu(MENU_BUSINESS_DIR, "业务功能", 0L, 2, "business", "", SysMenu.TYPE_DIR, "", "component");
    }

    private void ensureMenu(long menuId, String name, long parentId, int orderNum,
                            String path, String component, String type, String perms, String icon) {
        SysMenu exists = menuMapper.selectById(menuId);
        if (exists != null) {
            return;
        }
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setMenuName(name);
        menu.setParentId(parentId);
        menu.setOrderNum(orderNum);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setMenuType(type);
        menu.setVisible("0");
        menu.setStatus("0");
        menu.setPerms(perms);
        menu.setIcon(icon);
        menu.setCreateBy("system");
        menu.setCreateTime(LocalDateTime.now());
        menuMapper.insert(menu);
    }
}
