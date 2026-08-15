package io.github.hhhrrr777.jfast.baseline.system.controller;

import io.github.hhhrrr777.jfast.baseline.common.core.AjaxResult;
import io.github.hhhrrr777.jfast.baseline.common.core.TableDataVO;
import io.github.hhhrrr777.jfast.baseline.common.utils.SecurityUtils;
import io.github.hhhrrr777.jfast.baseline.system.dto.RoleSaveDTO;
import io.github.hhhrrr777.jfast.baseline.system.service.SysRoleService;
import io.github.hhhrrr777.jfast.baseline.system.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口。按钮级权限:system:role:*。
 */
@Tag(name = "系统-角色管理")
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色分页列表")
    @GetMapping("/list")
    @PreAuthorize("@perm.hasPermi('system:role:list')")
    public TableDataVO<RoleVO> list(@RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) String roleName,
                                    @RequestParam(required = false) String roleKey,
                                    @RequestParam(required = false) String status) {
        return roleService.list(pageNum, pageSize, roleName, roleKey, status);
    }

    @Operation(summary = "全部启用角色(用户表单下拉)")
    @GetMapping("/all")
    @PreAuthorize("@perm.hasPermi('system:user:list') or @perm.hasPermi('system:role:list')")
    public AjaxResult listAll() {
        return AjaxResult.success(roleService.listAll());
    }

    @Operation(summary = "角色详情(含绑定菜单)")
    @GetMapping("/{roleId}")
    @PreAuthorize("@perm.hasPermi('system:role:query')")
    public AjaxResult getById(@PathVariable long roleId) {
        return AjaxResult.success(roleService.getById(roleId));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("@perm.hasPermi('system:role:add')")
    public AjaxResult create(@Valid @RequestBody RoleSaveDTO dto) {
        return AjaxResult.success(roleService.create(dto, SecurityUtils.getUsername()));
    }

    @Operation(summary = "修改角色(含绑菜单)")
    @PutMapping
    @PreAuthorize("@perm.hasPermi('system:role:edit')")
    public AjaxResult update(@Valid @RequestBody RoleSaveDTO dto) {
        return AjaxResult.success(roleService.update(dto, SecurityUtils.getUsername()));
    }

    @Operation(summary = "删除角色(路径变量支持逗号分隔多 id)")
    @DeleteMapping("/{roleIds}")
    @PreAuthorize("@perm.hasPermi('system:role:remove')")
    public AjaxResult deleteByIds(@PathVariable List<Long> roleIds) {
        roleService.deleteByIds(roleIds);
        return AjaxResult.success();
    }
}
