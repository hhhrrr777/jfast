# 目标应用前端视觉对标 vue-vben-admin:设计令牌 + 亮暗双主题,架构零改

目标应用前端的**视觉风格对标 [vue-vben-admin v5](https://github.com/vbenjs/vue-vben-admin)(MIT)**,把它的设计语言(配色、圆角、间距、菜单/布局/登录形态)搬进 jfast 自研精简骨架,**只搬视觉、不搬架构**。核心论证:① 用户痛点是「登录页、菜单页太丑」,要的是 vben 的**长相**,不是它的 @core 内核/适配层/schema 表单/monorepo——那些会推翻「自研精简模板」(#9)、让目标应用背上 40+ 内部包,违背信创可审计定位;② vben 的好看本质来自一套 **CSS 变量设计令牌 + 菜单/布局规则**(见 `packages/@core/base/design/src/design-tokens/default.css`),与组件库解耦,可抽取翻译进 jfast 骨架而不引入其依赖;③ 架构(实体四件、独立路由页表单、`v-hasPermi`、极简依赖)经 ADR-0002/0003 拍板,本次一律不动,只在其上重画样式。**参照底稿与实测截图:`docs/research/vben-as-frontend-standard.md`、`docs/research/vben-shots/`(亮/暗令牌值与登录/布局/菜单/列表/表单视觉规格均已实测成文)。**

## Considered Options

- **吃进 vben @core 内核作为前端底座**:换来成熟布局/权限内核,但目标应用从「逐行可审计的精简骨架」变「绑定外部前端内核」,依赖膨胀(14+ 内部包),且实体四件须改成 vben schema 驱动、推翻 ADR-0003。拒绝——用户诉求是视觉,不是架构。
- **引入 Tailwind/UnoCSS 流水线复刻 vben 写法**:最贴近 vben 实现,但给骨架新增一条工具链,违背「不引入 Tailwind」前提。拒绝,改用 plain CSS 变量。
- **只调几个 Element Plus 主色、不集中令牌**:最简单,但令牌散落各处、亮暗双主题难维护。拒绝,集中成设计令牌层。

## Consequences

- **设计令牌层**:`styles/index.css` 单文件,`:root`(亮)/ `.dark`(暗)两块 `--jfast-*` 变量(照抽 vben 实测值:主色 `hsl(212 100% 45%)`、亮 `--background: 0 0% 100%` / 暗 `224 71% 4%`、`--radius: .5rem`、菜单 active/hover = accent 浅底等),并写一段映射灌进 Element Plus `--el-color-primary` 等。**不引入 Tailwind**。
- **亮暗双主题**:默认亮色、跟随系统 `prefers-color-scheme`、手动可覆写;`localStorage` + 根元素 `html.dark` 类持久化。
- **主色**:沿用 vben 蓝 `hsl(212 100% 45%)`,不做 jfast 自定义主题色。
- **登录页**:左分屏品牌区(渐变品牌区 + 产品名/slogan/插画 + 右侧表单、整宽主色登录按钮);**品牌内容骨架写死占位,不动向导**,用户生成后自行改代码。
- **实体表单页**:保留 ADR-0003 独立路由页契约,仅对齐输入框/标签/按钮/卡片视觉,**不改对话框**。
- **多标签页 tabbar**:本次不做,记入地图迷雾(后续迭代;若做需动路由/store)。
- **覆盖范围**:全部现存页面——登录、布局(侧栏+顶栏)、系统管理三域(user/role/menu)、示例实体、home、error。
- **改动落点**:先 `baseline/` 实工程改好并人工验收,再模板化进 `templates/base/`(与 S2→S3 流程一致);架构、路由、`v-hasPermi`、实体四件、依赖清单**零回归**。
- **验收**:人工并排对照 `vben-shots/` 截图 + 生成器 `npm run build` 门禁绿 + 关键文件定点断言不碎;不做像素级视觉回归。
- **施工组织**:派生一张施工票「目标应用前端视觉对标 vben」(令牌层 + 逐页重画 + 双主题),接入依赖图;实体前端四件模板(#49)须沿用本令牌层,故本决策先于 #49。

依据:拍板票据 <https://github.com/hhhrrr777/jfast/issues/60>(三轮 grilling;前提——架构零改、登录左分屏、亮暗双主题、全部页面、主色 vben 蓝,均用户 2026-08-16 拍板);参照底稿 `docs/research/vben-as-frontend-standard.md` 与实测截图 `docs/research/vben-shots/`。
