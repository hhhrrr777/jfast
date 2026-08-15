package io.github.hhhrrr777.jfast.baseline.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.hhhrrr777.jfast.baseline.common.exception.ServiceException;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysMenu;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysRoleMenu;
import io.github.hhhrrr777.jfast.baseline.system.dto.MenuSaveDTO;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysMenuMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysRoleMenuMapper;
import io.github.hhhrrr777.jfast.baseline.system.vo.MenuTreeVO;
import io.github.hhhrrr777.jfast.baseline.system.vo.RouterVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理应用服务:树查询、增删改查、前端动态路由数据(getRouters)。
 */
@Service
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public SysMenuService(SysMenuMapper menuMapper, SysRoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    /**
     * 全量菜单树(管理页树表)。按 parent_id + order_num 排序。
     */
    public List<MenuTreeVO> tree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum));
        return buildTree(all);
    }

    /**
     * 详情。
     */
    public MenuTreeVO getById(long menuId) {
        SysMenu menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw new ServiceException("菜单不存在");
        }
        return toVO(menu, new ArrayList<>());
    }

    /**
     * 新增:类型必填字段校验(目录/菜单须 path,菜单须 component,按钮须 perms)。
     */
    @Transactional
    public MenuTreeVO create(MenuSaveDTO dto, String operator) {
        validateTypeFields(dto);
        SysMenu menu = new SysMenu();
        copy(dto, menu);
        menu.setCreateBy(operator);
        menu.setCreateTime(LocalDateTime.now());
        menuMapper.insert(menu);
        return getById(menu.getMenuId());
    }

    /**
     * 修改:不可把自身挂到自己的子孙下(成环)。
     */
    @Transactional
    public MenuTreeVO update(MenuSaveDTO dto, String operator) {
        SysMenu exists = menuMapper.selectById(dto.getMenuId());
        if (exists == null) {
            throw new ServiceException("菜单不存在");
        }
        validateTypeFields(dto);
        if (isDescendant(dto.getMenuId(), dto.getParentId())) {
            throw new ServiceException("父菜单不能选择自己或自己的子菜单");
        }
        SysMenu menu = new SysMenu();
        menu.setMenuId(dto.getMenuId());
        copy(dto, menu);
        menu.setUpdateBy(operator);
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.updateById(menu);
        return getById(dto.getMenuId());
    }

    /**
     * 删除:有子菜单时拒绝;级联删角色菜单关联。「业务功能」目录(实体建模挂载点)受保护。
     */
    @Transactional
    public void deleteById(long menuId) {
        checkBusinessDirProtected(menuId);
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menuId));
        if (children != null && children > 0) {
            throw new ServiceException("存在子菜单,不允许删除");
        }
        menuMapper.deleteById(menuId);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getMenuId, menuId));
    }

    /**
     * 前端动态路由数据:按登录用户权限裁剪后的目录/菜单树(按钮不进路由)。
     */
    public List<RouterVO> getRouters(Long userId, boolean isAdmin) {
        List<SysMenu> menus = isAdmin
                ? menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, "0")
                        .orderByAsc(SysMenu::getParentId)
                        .orderByAsc(SysMenu::getOrderNum))
                : menuMapper.selectMenusByUserId(userId);
        return buildRouters(menus, 0L);
    }

    private void validateTypeFields(MenuSaveDTO dto) {
        String type = dto.getMenuType();
        if (SysMenu.TYPE_DIR.equals(type) || SysMenu.TYPE_MENU.equals(type)) {
            if (isBlank(dto.getPath())) {
                throw new ServiceException("目录/菜单必须填写路由地址");
            }
        }
        if (SysMenu.TYPE_MENU.equals(type) && isBlank(dto.getComponent())) {
            throw new ServiceException("菜单必须填写组件路径");
        }
        if (SysMenu.TYPE_BUTTON.equals(type) && isBlank(dto.getPerms())) {
            throw new ServiceException("按钮必须填写权限标识");
        }
    }

    /** parentId 是否为 menuId 自身或其子孙(防成环)。 */
    private boolean isDescendant(Long menuId, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return false;
        }
        if (menuId.equals(parentId)) {
            return true;
        }
        Long current = parentId;
        int depth = 0;
        while (current != null && current != 0L && depth < 100) {
            if (current.equals(menuId)) {
                return true;
            }
            SysMenu node = menuMapper.selectById(current);
            current = node == null ? null : node.getParentId();
            depth++;
        }
        return false;
    }

    private void checkBusinessDirProtected(long menuId) {
        if (menuId == SysMenu.BUSINESS_DIR_ID) {
            throw new ServiceException("「业务功能」目录为实体建模默认挂载点,不允许删除");
        }
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> all) {
        Map<Long, List<MenuTreeVO>> byParent = new LinkedHashMap<>();
        for (SysMenu menu : all) {
            byParent.computeIfAbsent(menu.getParentId(), k -> new ArrayList<>())
                    .add(toVO(menu, new ArrayList<>()));
        }
        List<MenuTreeVO> roots = byParent.getOrDefault(0L, new ArrayList<>());
        fillChildren(roots, byParent);
        return roots;
    }

    private void fillChildren(List<MenuTreeVO> nodes, Map<Long, List<MenuTreeVO>> byParent) {
        for (MenuTreeVO node : nodes) {
            List<MenuTreeVO> children = byParent.getOrDefault(node.getMenuId(), new ArrayList<>());
            node.setChildren(children);
            fillChildren(children, byParent);
        }
    }

    private List<RouterVO> buildRouters(List<SysMenu> menus, Long parentId) {
        Map<Long, List<SysMenu>> byParent = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            // 仅目录/菜单参与路由,按钮只贡献 perms
            if (!SysMenu.TYPE_BUTTON.equals(menu.getMenuType())) {
                byParent.computeIfAbsent(menu.getParentId(), k -> new ArrayList<>()).add(menu);
            }
        }
        return routersOf(byParent, parentId);
    }

    private List<RouterVO> routersOf(Map<Long, List<SysMenu>> byParent, Long parentId) {
        List<RouterVO> result = new ArrayList<>();
        // byParent 各桶已按 SQL 的 order_num 有序装入,这里保持父级顺序,不再按 path 重排。
        for (SysMenu menu : byParent.getOrDefault(parentId, new ArrayList<>())) {
            List<RouterVO> children = routersOf(byParent, menu.getMenuId());
            result.add(new RouterVO(routeName(menu), menu.getPath(), menu.getComponent(),
                    menu.getMenuName(), menu.getIcon(), children));
        }
        return result;
    }

    /**
     * 路由 name:取完整 path 逐段首字母大写(如 system/user → SystemUser),保证同目录下
     * 多菜单不撞名——只取首段会让同前缀菜单生成重复 Vue Router name,后者覆盖前者致页面丢失。
     */
    private String routeName(SysMenu menu) {
        String path = menu.getPath() == null ? "" : menu.getPath();
        if (path.isBlank()) {
            return "M" + menu.getMenuId();
        }
        StringBuilder name = new StringBuilder();
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                name.append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
            }
        }
        // 段拼接仍可能为空(path 全是斜杠)或与纯目录名冲突,兜底挂 menuId。
        return name.length() == 0 ? "M" + menu.getMenuId() : name.toString();
    }

    private void copy(MenuSaveDTO dto, SysMenu menu) {
        menu.setMenuName(dto.getMenuName());
        menu.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        menu.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        menu.setPath(orEmpty(dto.getPath()));
        menu.setComponent(orEmpty(dto.getComponent()));
        menu.setMenuType(dto.getMenuType());
        menu.setVisible(isBlank(dto.getVisible()) ? "0" : dto.getVisible());
        menu.setStatus(isBlank(dto.getStatus()) ? "0" : dto.getStatus());
        menu.setPerms(orEmpty(dto.getPerms()));
        menu.setIcon(isBlank(dto.getIcon()) ? "#" : dto.getIcon());
        menu.setRemark(dto.getRemark());
    }

    private String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private MenuTreeVO toVO(SysMenu menu, List<MenuTreeVO> children) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setMenuId(menu.getMenuId());
        vo.setMenuName(menu.getMenuName());
        vo.setParentId(menu.getParentId());
        vo.setOrderNum(menu.getOrderNum());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setMenuType(menu.getMenuType());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setPerms(menu.getPerms());
        vo.setIcon(menu.getIcon());
        vo.setCreateTime(menu.getCreateTime());
        vo.setRemark(menu.getRemark());
        vo.setChildren(children);
        return vo;
    }
}
