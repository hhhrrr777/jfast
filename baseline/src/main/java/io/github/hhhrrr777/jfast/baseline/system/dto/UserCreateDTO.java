package io.github.hhhrrr777.jfast.baseline.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增用户请求。
 */
public class UserCreateDTO {

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 30, message = "登录账号长度不能超过 30 个字符")
    private String userName;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度不能超过 30 个字符")
    private String nickName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 5, max = 20, message = "密码长度必须在 5 到 20 个字符之间")
    private String password;

    /** 状态(0正常 1停用)。 */
    private String status;

    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    /** 绑定角色 ID 列表。 */
    private java.util.List<Long> roleIds;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
