# vben 视觉风格参照底稿(登录页 / 菜单布局)— 目标应用前端「样式对标 vben」

> 触发:用户澄清——**「目前的登录页面、菜单页面长得太丑,我希望样式和 vben 一样」**。
> 意图界定:这是**视觉风格对标**,不是架构替换。要的是 vben 的**长相**(登录页、布局/菜单/顶栏的视觉),**不要** vben 的 @core 内核、适配层、schema 表单、monorepo 依赖。
> 参照对象:[vbenjs/vue-vben-admin](https://github.com/vbenjs/vue-vben-admin) v5(MIT);DeepWiki:https://deepwiki.com/vbenjs/vue-vben-admin
> 评估日期:2026-08-16
> **对既有决策的影响:零**。自研精简模板(#9/ADR-0003)、实体四件、独立路由页表单、`v-hasPermi` 权限指令、极简依赖——**全部保留,一行不动**。本文只改「登录页 / 布局 / 菜单」的**视觉与样式**。

---

## 0. 为什么这件事比「换框架」小得多

最初按「用 vben 改造」理解时,冲突面很大(内核、实体模板、路由、权限层全要重做)。用户澄清后,诉求收敛为**纯视觉**:把 baseline 里 `login/index.vue`、`layout/index.vue`(侧栏菜单 + 顶栏)按 vben 的设计语言重画。

- **不动的**:Element Plus 组件库不变(与 #9 一致)、架构不变、依赖不引入 @vben 内核。
- **要动的**:登录页 + 布局 + 菜单的样式、配色、圆角、间距、暗色模式。
- **工作量量级**:从「重做实体模板 + 换内核」塌缩成「**重画两三个页面的样式 + 一套设计令牌**」。

## 1. vben 视觉的实现机制(决定怎么搬)

vben 的好看**不来自组件本身**,来自它的一套 **CSS 变量设计令牌 + Tailwind 工具类**:

- 令牌集中定义在 `packages/@core/base/design/src/design-tokens/default.css`,以 `--*` CSS 变量 + HSL 通道形式表达;暗色模式靠根元素挂 `dark` 类覆盖同一组变量。
- 布局/菜单/登录页用 Tailwind 类(`rounded-3xl`、`bg-primary`、`bg-background-deep` 等)消费这些令牌。

**对 jfast 的含义**:要「样式像 vben」,正确做法是**抽取它的设计令牌与视觉规则,翻译进 jfast 精简骨架的样式层**(plain CSS 变量 + Element Plus 主题变量),**而不是**搬它的 Tailwind 流水线或组件。Element Plus 自身也有 `--el-*` 变量可对接主色。

## 2. 设计令牌(已从 www.vben.pro 实测抽取,2026-08-16)

> 注意:预览站 www.vben.pro 是 **antd 版**(页面里是 `--ant-color-primary:#1677ff`),但 vben 的**自有设计令牌是 UI 无关的**(antd/ele 共用同一套 `--*`),已直接抽到亮/暗两套真实值。落法:同步到 Element Plus `--el-color-primary` 等 + 骨架 `--*` 变量。下列 HSL 通道值直接可用。

**亮(:root)**:主色 `--primary: 212 100% 45%`(色阶 `--primary-50..950` 同色相 212,亮度 97%→9%);`--background: 0 0% 100%`;`--background-deep: 216 20% 95%`;`--foreground: 210 6% 21%`;`--card/popover: 0 0% 100%`;`--muted: 240 5% 96%`;`--accent: 240 5% 96%`;`--heavy: 192 9% 90%`;`--border: 240 6% 90%`;`--radius: .5rem`;`--sidebar/header: 0 0% 100%`;success `144 57% 58%`、warning `42 84% 61%`、destructive `348 100% 61%`。

**暗(.dark)**:`--background: 224 71% 4%`;`--foreground: 210 20% 98%`;`--card/popover: 224 71% 4%`;`--muted: 215 28% 17%`;`--accent: 215 28% 17%`;`--heavy: 216 5% 24%`;`--border: 215 28% 17%`;`--sidebar/header: 224 71% 4%`;`--input-background: 0 0% 100% / 5%`。

**菜单项规则(暗色实测,亮色同构)**:底 `hsl(var(--menu))` = `--sidebar`;文字 `--menu-item-color: hsl(var(--foreground)/80%)`;**hover = `--menu-item-hover-background-color: hsl(var(--accent))`**;**active = `--menu-item-active-background-color: hsl(var(--accent))`**(注意:v5 菜单 active 是 accent 底而非主色实底,与旧版不同);圆角 `--radius`、字号 `--menu-font-size = 基准*0.875`。

暗色切换:根元素挂/摘 `dark` 类覆盖同组变量(实测 `html.dark`)。完整令牌(含 primary/warning/success/destructive 全 50–950 色阶)已抽取,可照抄进骨架 `styles/index.css`。

## 3. 登录页视觉规格(实测截图:`docs/research/vben-shots/login-light.png`,实为暗色)

> 实测:www.vben.pro 默认暗色。登录页 = **左分屏品牌区**:左侧深蓝渐变底 + 3D 插画 + 产品名「Vben Admin」+ slogan「开箱即用的大型中后台管理系统 / 工程化、高性能、跨组件库的前端模版」;右侧表单。右上角有主题/语言/布局工具条。jfast 版品牌区换成 jfast 自己的产品名与 slogan。

- **布局三态**:`Center`(居中卡片)/ `Left` / `Right` 分屏。分屏时一侧是**品牌/插画区**(渐变背景 + 模糊,亮 `bg-background-deep`、暗 `#070709`),另一侧是表单;居中态是单卡片。
- **表单卡片**:居中卡片 `rounded-3xl`(大圆角)+ `shadow-float shadow-primary/5`(浮起软阴影)。
- **表单元素**:输入框(经其 form 体系,视觉上是干净的大输入框)、**登录按钮整宽 `w-full`**、「记住我 / 忘记密码」一行 `justify-between` 放在输入框下方、登录按钮上方。
- **品牌元素**:产品名(`appName`)、slogan(`pageTitle`/`pageDescription`)、插画(`sloganImage`,缺省给默认 Slogan 图标)。
- **工具条**:登录页右上角可有主题/语言切换(`toolbar`)。
- **现状差距**:jfast 现状 `baseline/src/views/login/index.vue` 是朴素表单,没有分屏品牌区、没有大圆角浮起卡片、没有主色调色板。

## 4. 布局 / 侧栏菜单 / 顶栏视觉规格(实测截图,亮色=layout-light、暗色=layout-dark)

**截图索引**(全部实测自 www.vben.pro,存 `docs/research/vben-shots/`,施工时并排对照):
`login-light.png`(暗色登录)/ `login-light-form.png`(亮色登录)/ `layout-light.png` / `layout-dark.png` / `sidebar-expanded.png`(二级菜单展开)/ `sidebar-collapsed.png` / `header.png`(顶栏+tabbar+头像下拉+角色管理页首屏)/ `table-page.png`(角色管理列表页全页)/ `form-page.png`(新增部门对话框表单+部门树表)。

**整体三段式**:左固定侧栏 + 顶栏(约 50px)+ 其下**多标签页 tabbar** + 内容区(浅灰/深底 `--background-deep`,卡片白/深 `--card` + `--radius` 圆角 + 软阴影)。

**侧栏菜单**:
- 顶部 logo + 应用名;宽度可折叠(折叠后图标only、文字隐藏、图标放大)。
- 一级菜单带图标 + 右侧展开箭头;展开的父级露出**缩进的二级菜单**(各带图标)。
- **选中项 = 浅蓝高亮块 + 主色文字**(亮色)/ **深色高亮块 + 亮文字**(暗色),圆角;**当前激活的上级父菜单文字变主色**;hover 态 accent 底。机制对应 §2 的 `--menu-item-*` 变量(active/hover = accent 底)。
- 侧栏底亮 `--sidebar: 0 0% 100%`、暗 `224 71% 4%`。

**顶栏(header)**:左侧折叠按钮 + 面包屑(`系统管理 / 角色管理`,父级主色);右侧搜索框(带 ⌘K)、设置、暗色切换、语言、全屏、通知、锁屏、退出、**头像下拉**(个人中心/文档/GitHub/问题帮助)。jfast MVP 顶栏从简:折叠 + 面包屑 + 暗色切换 + 头像下拉即可,其余图标留接口。

**多标签页 tabbar**:每个已开页一个标签,当前页浅蓝高亮、可关闭(×)、可固定(图钉)。**属新增功能**——见 §7,默认不进本次范围。

**内容页**:
- **列表页**(table-page):顶部搜索区(字段 + 重置/搜索/收起)→ 「XX列表」卡片(右上**新增按钮主色** + 刷新/密度/列设置)→ 表格(状态列用 el-switch,操作列 `修改·详情·删除`,删除红色)→ 底部分页(共 N 条 / N条每页 / 页码)。**与 jfast 实体四件 `index.vue` 结构一致,可直接当样板。**
- **表单**(form-page):vben 系统管理用**对话框表单**(标题 + 字段(标签在上/必填星号)+ 底部 重置/取消/确认)。**jfast 保留 ADR-0003 的独立路由页表单**,只对齐其输入框/标签/按钮的**视觉**,不改成对话框。
- **树表**(部门管理):可展开树形表格 + 状态徽标 + `新增下级/修改/删除` 操作列——菜单/部门类实体样板。

## 5. 要动的文件(baseline 实测,模板侧同构)

视觉对标只触这几个,均在 `baseline/src/` 与 `src/main/resources/templates/base/`(模板为 `.ftl` 同构体):

| 文件 | 改动 |
|---|---|
| `styles/index.css` | **新增设计令牌层**:`--primary`/背景/圆角/菜单项等 CSS 变量 + 同步 `--el-color-primary`;暗色留 `dark` 类接口 |
| `views/login/index.vue` | 重画为分屏/居中卡片、整宽登录按钮、记住我/忘记密码行、产品名 + slogan |
| `layout/index.vue` | 重画侧栏(logo 区、菜单项激活/悬停/圆角/折叠态)+ 顶栏(50px、面包屑/头像下拉)|
| (可选)`router`/`store` | 仅当引入多标签页才动——**本次不动** |

**不引入 Tailwind 流水线**:令牌用 plain CSS 变量落 `index.css`,组件内用 Element Plus 主题变量 + 局部样式实现 vben 视觉,保持骨架极简。

## 6. 验收对照(给施工单元用)

- 登录页:分屏品牌区(或居中卡片)+ 整宽主色登录按钮 + 大圆角浮起卡片,与 vben 登录页并排截图视觉一致。
- 侧栏:激活项主色高亮 + 圆角、悬停态、折叠态文字隐藏/图标放大,与 vben 菜单视觉一致。
- 配色:全站主色 = `hsl(212 100% 45%)`,经 `--el-color-primary` 贯穿 Element Plus 组件。
- 架构零回归:实体四件、路由、`v-hasPermi`、依赖清单不变;生成器 `npm run build` 门禁全绿。

## 7. 待拍板的小问题(定样式时顺带定)

1. ~~登录页布局~~ → **已拍板:左分屏品牌区**(用户 2026-08-16)。
2. ~~暗色模式~~ → **已拍板:亮暗双主题都做**(用户 2026-08-16)。
3. **多标签页 tabbar**:vben 标志性特征(见 layout-*/header.png),但属新增功能非纯样式——**建议留路线图,不进本次**;若要,需动路由/store,单独立票。
4. ~~品牌插画/slogan~~ → **已拍板:左侧品牌区的产品名/slogan/插画留参数,由用户生成时填**(用户 2026-08-16);骨架给占位默认值。
5. **主色** → **已拍板:沿用 vben 蓝 `hsl(212 100% 45%)`**(用户 2026-08-16),不做 jfast 自定义主题色。

**本次样式对标的固定边界**:架构零改;登录 = 左分屏品牌区;亮暗双主题;主色 vben 蓝;逐页对齐(登录/布局/菜单/系统管理/示例实体/home/error);tabbar 留路线图;品牌内容用户填。

---

*依据:DeepWiki「vbenjs/vue-vben-admin」视觉问答(2026-08-16);令牌定义见 vben `packages/@core/base/design/src/design-tokens/default.css`;现状文件见 `baseline/src/` 与 `src/main/resources/templates/base/`。*
