package io.github.hhhrrr777.jfast.baseline.system.controller;

import io.github.hhhrrr777.jfast.baseline.common.core.AjaxResult;
import io.github.hhhrrr777.jfast.baseline.common.utils.SecurityUtils;
import io.github.hhhrrr777.jfast.baseline.system.dto.MenuSaveDTO;
import io.github.hhhrrr777.jfast.baseline.system.service.SysMenuService;
import io.github.hhhrrr777.jfast.baseline.system.vo.MenuTreeVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理接口。按钮级权限:system:menu:*。
 */
@Tag(name = "系统-菜单管理")
@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    private final SysMenuService menuService;

    public SysMenuController(SysMenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "菜单树(管理页树表 / 角色绑菜单树共用)")
    @GetMapping("/tree")
    @PreAuthorize("@perm.hasPermi('system:menu:list') or @perm.hasPermi('system:role:list')")
    public AjaxResult tree() {
        return AjaxResult.success(menuService.tree());
    }

    @Operation(summary = "菜单详情")
    @GetMapping("/{menuId}")
    @PreAuthorize("@perm.hasPermi('system:menu:query')")
    public AjaxResult getById(@PathVariable long menuId) {
        return AjaxResult.success(menuService.getById(menuId));
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("@perm.hasPermi('system:menu:add')")
    public AjaxResult create(@Valid @RequestBody MenuSaveDTO dto) {
        return AjaxResult.success(menuService.create(dto, SecurityUtils.getUsername()));
    }

    @Operation(summary = "修改菜单")
    @PutMapping
    @PreAuthorize("@perm.hasPermi('system:menu:edit')")
    public AjaxResult update(@Valid @RequestBody MenuSaveDTO dto) {
        return AjaxResult.success(menuService.update(dto, SecurityUtils.getUsername()));
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{menuId}")
    @PreAuthorize("@perm.hasPermi('system:menu:remove')")
    public AjaxResult deleteById(@PathVariable long menuId) {
        menuService.deleteById(menuId);
        return AjaxResult.success();
    }
}
