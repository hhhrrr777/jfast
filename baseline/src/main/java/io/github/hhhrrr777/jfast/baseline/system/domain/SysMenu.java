package io.github.hhhrrr777.jfast.baseline.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 菜单权限表 sys_menu。三种类型:M目录 C菜单 F按钮;树结构(parent_id=0 为根)。
 */
@TableName("sys_menu")
public class SysMenu {

    public static final String TYPE_DIR = "M";
    public static final String TYPE_MENU = "C";
    public static final String TYPE_BUTTON = "F";

    /** 「业务功能」目录菜单固定 ID:实体建模生成的菜单默认挂载点(ADR-0003)。 */
    public static final long BUSINESS_DIR_ID = 2000L;

    @TableId(type = IdType.AUTO)
    private Long menuId;

    /** 菜单名称。 */
    private String menuName;

    /** 父菜单ID(0 为根)。 */
    private Long parentId;

    /** 显示顺序。 */
    private Integer orderNum;

    /** 路由地址。 */
    private String path;

    /** 前端组件路径(相对 src/views,目录/按钮为空)。 */
    private String component;

    /** 类型(M目录 C菜单 F按钮)。 */
    private String menuType;

    /** 显示状态(0显示 1隐藏)。 */
    private String visible;

    /** 菜单状态(0正常 1停用)。 */
    private String status;

    /** 权限标识(如 system:user:add)。 */
    private String perms;

    /** 菜单图标。 */
    private String icon;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
