package io.github.hhhrrr777.jfast.baseline.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.hhhrrr777.jfast.baseline.auth.model.LoginUser;
import io.github.hhhrrr777.jfast.baseline.common.core.TableDataVO;
import io.github.hhhrrr777.jfast.baseline.common.exception.ServiceException;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysRole;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysRoleMenu;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUserRole;
import io.github.hhhrrr777.jfast.baseline.system.dto.RoleSaveDTO;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysRoleMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysRoleMenuMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserRoleMapper;
import io.github.hhhrrr777.jfast.baseline.system.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理应用服务:分页查询、增删改查、菜单绑定(角色即权限集合的载体)。
 * 超管角色(role_key=admin)禁止修改与删除。
 */
@Service
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public SysRoleService(SysRoleMapper roleMapper,
                          SysRoleMenuMapper roleMenuMapper,
                          SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 分页查询:按名称模糊、权限字符串精确、状态精确。
     */
    public TableDataVO<RoleVO> list(long pageNum, long pageSize, String roleName, String roleKey, String status) {
        Page<SysRole> page = roleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysRole>()
                        .like(!isBlank(roleName), SysRole::getRoleName, roleName)
                        .eq(!isBlank(roleKey), SysRole::getRoleKey, roleKey)
                        .eq(!isBlank(status), SysRole::getStatus, status)
                        .orderByAsc(SysRole::getRoleSort));
        List<RoleVO> rows = page.getRecords().stream().map(this::toVO).toList();
        return TableDataVO.of(page.getTotal(), rows);
    }

    /** 全量启用角色(用户表单的角色下拉)。 */
    public List<RoleVO> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, "0")
                        .orderByAsc(SysRole::getRoleSort))
                .stream().map(this::toVO).toList();
    }

    /**
     * 详情(含绑定菜单)。
     */
    public RoleVO getById(long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        return toVO(role);
    }

    /**
     * 新增:role_key 唯一校验 → 落库 → 绑菜单。
     */
    @Transactional
    public RoleVO create(RoleSaveDTO dto, String operator) {
        checkRoleKeyUnique(dto.getRoleKey(), null);
        SysRole role = new SysRole();
        copy(dto, role);
        role.setCreateBy(operator);
        role.setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);
        replaceMenus(role.getRoleId(), dto.getMenuIds());
        return getById(role.getRoleId());
    }

    /**
     * 修改:名称/排序/状态/备注/菜单;超管角色(role_key=admin)受保护。
     */
    @Transactional
    public RoleVO update(RoleSaveDTO dto, String operator) {
        SysRole exists = roleMapper.selectById(dto.getRoleId());
        if (exists == null) {
            throw new ServiceException("角色不存在");
        }
        checkRoleKeyUnique(dto.getRoleKey(), dto.getRoleId());
        checkAdminRoleProtected(exists, "不允许修改超管角色");
        SysRole role = new SysRole();
        role.setRoleId(dto.getRoleId());
        copy(dto, role);
        role.setUpdateBy(operator);
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
        if (dto.getMenuIds() != null) {
            replaceMenus(dto.getRoleId(), dto.getMenuIds());
        }
        return getById(dto.getRoleId());
    }

    /**
     * 删除:物理删除(与 SysUser 一致,避开逻辑删除与唯一键冲突);级联删角色菜单/用户角色关联。
     * 超管角色与已分配角色受保护。
     */
    @Transactional
    public void deleteByIds(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysRole role = roleMapper.selectById(roleId);
            if (role == null) {
                continue;
            }
            checkAdminRoleProtected(role, "不允许删除超管角色");
            Long assigned = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, roleId));
            if (assigned != null && assigned > 0) {
                throw new ServiceException("角色已分配给用户,不能删除:" + role.getRoleName());
            }
            roleMapper.deleteById(roleId);
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getRoleId, roleId));
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, roleId));
        }
    }

    private void checkRoleKeyUnique(String roleKey, Long excludeRoleId) {
        SysRole exists = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey));
        if (exists != null && (excludeRoleId == null || !exists.getRoleId().equals(excludeRoleId))) {
            throw new ServiceException("角色权限字符串已存在:" + roleKey);
        }
    }

    private void checkAdminRoleProtected(SysRole role, String message) {
        if (LoginUser.ROLE_ADMIN.equals(role.getRoleKey())) {
            throw new ServiceException(message);
        }
    }

    private void replaceMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds) {
            roleMenuMapper.insert(new SysRoleMenu(roleId, menuId));
        }
    }

    private void copy(RoleSaveDTO dto, SysRole role) {
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setRoleSort(dto.getRoleSort() == null ? 0 : dto.getRoleSort());
        role.setStatus(isBlank(dto.getStatus()) ? "0" : dto.getStatus());
        role.setRemark(dto.getRemark());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private RoleVO toVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setRoleId(role.getRoleId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleKey(role.getRoleKey());
        vo.setRoleSort(role.getRoleSort());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setRemark(role.getRemark());
        vo.setMenuIds(roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, role.getRoleId()))
                .stream().map(SysRoleMenu::getMenuId).toList());
        return vo;
    }
}
