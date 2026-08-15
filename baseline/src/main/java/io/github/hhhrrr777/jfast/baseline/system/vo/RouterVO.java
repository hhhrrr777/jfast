package io.github.hhhrrr777.jfast.baseline.system.vo;

import java.util.List;

/**
 * 前端动态路由节点(getRouters 响应契约)。仅目录/菜单参与;按钮只贡献 perms 不进路由。
 * 组件路径为相对 src/views 的模块路径(如 system/user/index),前端 import.meta.glob 解析。
 */
public class RouterVO {

    private String name;
    private String path;
    private String component;
    /** 菜单标题(侧边栏展示)。 */
    private String title;
    private String icon;
    /** 子路由。 */
    private List<RouterVO> children;

    public RouterVO() {
    }

    public RouterVO(String name, String path, String component, String title, String icon, List<RouterVO> children) {
        this.name = name;
        this.path = path;
        this.component = component;
        this.title = title;
        this.icon = icon;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<RouterVO> getChildren() {
        return children;
    }

    public void setChildren(List<RouterVO> children) {
        this.children = children;
    }
}
