package io.github.hhhrrr777.jfast.baseline.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增/修改菜单请求(无 id 为新增,有 id 为修改)。
 */
public class MenuSaveDTO {

    private Long menuId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过 50 个字符")
    private String menuName;

    @NotNull(message = "父菜单ID不能为空")
    private Long parentId;

    private Integer orderNum;

    /** 路由地址(目录/菜单必填)。 */
    @Size(max = 200, message = "路由地址长度不能超过 200 个字符")
    private String path;

    /** 前端组件路径(菜单类型必填)。 */
    @Size(max = 255, message = "组件路径长度不能超过 255 个字符")
    private String component;

    /** 类型:M目录 C菜单 F按钮。 */
    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "[MCF]", message = "菜单类型必须为 M/C/F")
    private String menuType;

    /** 显示状态(0显示 1隐藏)。 */
    private String visible;

    /** 菜单状态(0正常 1停用)。 */
    private String status;

    /** 权限标识(按钮必填,如 system:user:add)。 */
    @Size(max = 100, message = "权限标识长度不能超过 100 个字符")
    private String perms;

    @Size(max = 100, message = "图标长度不能超过 100 个字符")
    private String icon;

    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String visible) {
        this.visible = visible;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
