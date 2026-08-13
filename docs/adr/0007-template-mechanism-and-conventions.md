# 模板文件名模板化机制与模板编写规范:`.ftl` 后缀 opt-in + 全通道 Freemarker `${}`

「目录即模板」(ADR-0001)下模板机制定稿:**渲染判定 = `.ftl` 后缀 opt-in**(需渲染的文件命名 `XxxController.java.ftl`,输出剥后缀;无后缀文件字节级原样拷贝,二进制天然覆盖);**文件名/路径变量与文件内容同走一套 Freemarker `${}`、同一渲染接缝、同一份模型**(如 `${entityClass}Controller.java.ftl`),包路径 = 单占位目录段 `${packagePath}`(模型放斜杠分隔路径,dot→slash 在模型构造期完成,与点分隔的 `packageName` 并存);**渲染接缝 = 纯字符串接口** `String render(String templateSource, String templateName, Map<String, Object> model)`,遍历/剥后缀/文件名渲染/写盘归上层 FileTreeWalker,引擎实现零 IO,配置钉死 UTF-8、未定义变量报错(严格模式)、异常带模板名+行号、关闭自动 HTML 转义;**根模型三命名空间** `project.*` / `entity.*`(仅实体建模命令存在)/ `conditions.*`(纯布尔,ADR-0004),统一 camelCase,模板引用未声明的 conditions 键即生成期报错;**目标工程落盘 `.jfast/project.json`**(含 `preset` 字段)作为实体建模命令的 conditions 来源。核心论证:① 目标应用中 `${}` 字面值是常态(TS 模板字面量、`application.yml` 的 Spring 占位符、MyBatis XML),opt-in 让「忘记转义被引擎静默吞字」这一错误类对非模板文件彻底消失——render-all 方案必须全仓逐处转义且依赖严格模式兜底,失败成本不对称;② 文件名与内容单语法单通道,与 ADR-0001「渲染收敛到单一接缝」契合,换 Rocker 时替换面只有一个字符串接口实现类;③ 严格模式 + conditions 白名单校验把模板期一切静默失败变成生成期报错,与 ADR-0004 的保守哲学一致。

## Considered Options

- **render-all(无后缀,JHipster/cookiecutter 式)**:文件保持真实扩展名,IDE 语法支持最好,「目录即模板」最纯粹。但所有文本文件的 `${}` 字面值都要逐处转义,Freemarker 非严格模式下未定义变量静默输出空串,忘转义 = 静默吃字。拒绝,选 `.ftl` opt-in;代价是 `.java.ftl` 只拿 Freemarker 高亮,接受。
- **文件名专用占位符 `__var__` / cookiecutter 式 `{{ }}`**:前者引入第二套语法与第二条替换通道;后者与 Vue 插值 `{{ }}` 视觉混淆。拒绝,文件名同走 `${}`。
- **包路径用点分隔 `packageName` + walker 对渲染结果做 dot→slash 特判,或写死占位真实目录做整段前缀替换**:都是只对包目录生效的隐式魔法,且与「同一条渲染通道」相悖。拒绝,模型构造期派生 `packagePath`。
- **文件级接缝 `render(Path src, Path dst, model)`**:引擎内藏 IO 与遍历,测试要造假文件系统,native 换引擎替换面更大。拒绝,选纯字符串接口。
- **转义一律 `<#noparse>` 或一律内联 `${'${'}...}`**:前者单处也包块、噪音大且块内变量一并失效易误伤;后者在 yml 连续占位符场景连成串不可读。拒绝,用双轨启发式(见 Consequences)。
- **文件级条件写进模板(内容整体包 `<#if>`)或文件名内嵌条件表达式**:前者空工程会产出空 init.sql 污染产物树;后者给文件名引入第二套微语法。拒绝,文件级条件归编排层 Java 代码判定。
- **允许 `${var!"默认值"}` 缺省值写法**:掩饰缺变量,与严格模式的设计意图相悖。拒绝,可选值在模型构造期填充。

## Consequences

- **模板编写规范清单**(模板作者遵守):
  1. 需渲染 → 加 `.ftl` 后缀;不渲染 → 保持产物文件名,字节拷贝。
  2. 文件名/目录名变量只用 `${...}`,与内容同一模型;包路径目录写 `${packagePath}`。
  3. 内容变量只用 `${...}`;`${}` 字面值转义双轨:单处 `${'${'}userId}`,密集处(≥3 或成片)`<#noparse>…</#noparse>` 整段,无第三种写法。
  4. 模板注记一律 `<#-- ... -->`(不进产物);产物注释用产物语言注释直写。
  5. 禁 `${var!"默认"}` 缺省值写法;接受指令行留下的产物空行,不为排版扭曲模板写法(生成物是否接 Spotless/Prettier 归 CI 门禁/测试策略后续票据)。
  6. 条件只写 `<#if conditions.*>`,引用未声明键即报错;「文件产不产」不写进模板,归编排层。
- **文件级条件分工**三层各司其职:manifest 声明 conditions 键(ADR-0004)→ 编排层 Java 代码决定文件级产不产(如 `!conditions.systemAdmin` 时实体建模把 `db/init/` 模板子树从渲染清单剔除,ADR-0003 的「空工程不产 init.sql」落地)→ 模板内 `<#if>` 只管内容分叉。
- **`.jfast/project.json`**:目标工程根落盘,至少含 `preset` 字段,与已拍板的 `.jfast/models/<实体>.json`(建模输入落盘)同目录同风格;实体建模命令据此把目标工程预设映射回 `conditions.*` 同一组键。
- **引擎双轨前提收敛**(ADR-0001):Freemarker 配置在接缝实现内钉死(严格模式、UTF-8、RETHROW 异常、关闭 output_format 自动转义);将来换 Rocker 只重写该实现类,模板按新语法重写是既定代价。
- 与 ADR-0004 的尖括号语法约定一致(与 `.vue`/TS 无冲突:`<#if>` 非合法 HTML/TS,`{{ }}` 与 Freemarker 无交集),不重开。

依据:拍板票据 <https://github.com/hhhrrr777/jfast/issues/20>(两轮 grilling,Q1–Q8 全部按推荐落定)。
