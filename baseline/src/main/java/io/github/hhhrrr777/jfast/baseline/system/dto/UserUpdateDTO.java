package io.github.hhhrrr777.jfast.baseline.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改用户请求。userName 不可改(user_name 为登录账号,改账号等价换身份,不给入口)。
 */
public class UserUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过 30 个字符")
    private String nickName;

    /** 状态(0正常 1停用)。 */
    private String status;

    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    /** 绑定角色 ID 列表(全量替换)。 */
    private java.util.List<Long> roleIds;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public java.util.List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(java.util.List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
