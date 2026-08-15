package io.github.hhhrrr777.jfast.baseline.system.controller;

import io.github.hhhrrr777.jfast.baseline.common.core.AjaxResult;
import io.github.hhhrrr777.jfast.baseline.common.core.TableDataVO;
import io.github.hhhrrr777.jfast.baseline.common.utils.SecurityUtils;
import io.github.hhhrrr777.jfast.baseline.system.dto.ChangePasswordDTO;
import io.github.hhhrrr777.jfast.baseline.system.dto.ResetPasswordDTO;
import io.github.hhhrrr777.jfast.baseline.system.dto.UserCreateDTO;
import io.github.hhhrrr777.jfast.baseline.system.dto.UserUpdateDTO;
import io.github.hhhrrr777.jfast.baseline.system.service.SysUserService;
import io.github.hhhrrr777.jfast.baseline.system.vo.UserVO;
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
 * 用户管理接口。按钮级权限:system:user:*。
 */
@Tag(name = "系统-用户管理")
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户分页列表")
    @GetMapping("/list")
    @PreAuthorize("@perm.hasPermi('system:user:list')")
    public TableDataVO<UserVO> list(@RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) String userName,
                                    @RequestParam(required = false) String nickName,
                                    @RequestParam(required = false) String status) {
        return userService.list(pageNum, pageSize, userName, nickName, status);
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{userId}")
    @PreAuthorize("@perm.hasPermi('system:user:query')")
    public AjaxResult getById(@PathVariable long userId) {
        return AjaxResult.success(userService.getById(userId));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("@perm.hasPermi('system:user:add')")
    public AjaxResult create(@Valid @RequestBody UserCreateDTO dto) {
        return AjaxResult.success(userService.create(dto, SecurityUtils.getUsername()));
    }

    @Operation(summary = "修改用户(含启用禁用)")
    @PutMapping
    @PreAuthorize("@perm.hasPermi('system:user:edit')")
    public AjaxResult update(@Valid @RequestBody UserUpdateDTO dto) {
        return AjaxResult.success(userService.update(dto, SecurityUtils.getUsername()));
    }

    @Operation(summary = "删除用户(路径变量支持逗号分隔多 id)")
    @DeleteMapping("/{userIds}")
    @PreAuthorize("@perm.hasPermi('system:user:remove')")
    public AjaxResult deleteByIds(@PathVariable List<Long> userIds) {
        userService.deleteByIds(userIds, SecurityUtils.getUserId());
        return AjaxResult.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/resetPwd")
    @PreAuthorize("@perm.hasPermi('system:user:resetPwd')")
    public AjaxResult resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto, SecurityUtils.getUsername());
        return AjaxResult.success();
    }

    @Operation(summary = "修改自己的密码(登录后自行修改)")
    @PutMapping("/profile/password")
    public AjaxResult changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(SecurityUtils.getUserId(),
                dto.getOldPassword(), dto.getNewPassword(), SecurityUtils.getUsername());
        return AjaxResult.success();
    }
}
