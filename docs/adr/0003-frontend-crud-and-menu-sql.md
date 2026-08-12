# 实体建模前端 CRUD 模板与菜单 SQL 接线:TS 组件化四件 + 人工执行 init.sql

承接 ADR-0002 移出 MVP 的前端部分:每实体生成**前端四件**——`api/<模块>/<实体>.ts`(axios 封装:list/get/add/update/del 五函数)、`types/<模块>/<实体>.ts`(Query/Form/VO 三类型)、`views/<模块>/<实体>/index.vue`(搜索区+表格+分页+按钮权限指令)、`views/<模块>/<实体>/form.vue`(新增/编辑共用的独立路由页,按路由参数 `:id?` 区分);代码形态 **TypeScript + `<script setup>`**。菜单与字典初始化产出**每实体一个 `db/init/<实体>-init.sql`**(注释分菜单/按钮权限/字典三段),由管理员**人工执行一次**。核心论证:① 表单独立路由页 + 组件拆分 + TS 是面向复杂业务表单的工程化选择,示例 CRUD(#9 底线项)按此形态实现,生成模板即其复刻,用户心智以「jfast 自己的示例」为准而非若依逐字复刻;② 菜单 SQL 人工执行与若依心智一致、管理员可控,避免把 DML 种子数据塞进 MP 自动维护 DDL 的机制约束(见 Considered Options);③ init.sql 用方言无关子查询写法,一套模板通吃 MySQL/PG/达梦,与按方言分目录的 DDL 脚本职责分明。

## Considered Options

- **跟随若依 Vue3:JS + 单文件 index.vue + el-dialog 表单**:用户心智最近,但 JS 无类型保障,DTO/VO 分层理念(ADR-0002)到前端断档;对话框在复杂表单(多字段分组、校验、字典联动)下拥挤;单文件不拆组件在大实体下可读性差。拒绝,接受「偏离若依心智」的代价——jfast 的模板范本是自带示例 CRUD,不是若依截图。
- **菜单 SQL 纳入 MP 自动维护 DDL 启动时自动执行**:机制上可行(底层是通用 ScriptRunner,支持任意 DML),但经研究子代理核查有三重约束:去重粒度是**文件路径**而非内容(追加菜单必须出新文件);默认**吞掉异常**仅打日志(菜单插入失败无声);`ddl_history` 方言仅内置 MySQL/PG/Oracle,信创库需自定义 `IDdlGenerator`。菜单是管理员该审阅后执行的东西,人工执行与若依心智一致。拒绝自动执行;DDL 建表仍走 auto-ddl(ADR-0002 不变)。
- **菜单 SQL 按方言各产一份(MySQL 会话变量 / PG RETURNING)**:若依原生写法(`@parentId := LAST_INSERT_ID()`)耦合 MySQL;两套模板维护成本高。拒绝,改用方言无关子查询(按钮 parent_id 按 perms 反查菜单 id)。
- **固定 id 段显式写主键**:幂等可控,但要求生成器维护跨实体的 id 分配状态,与「无状态模板渲染」相悖。拒绝。
- **空工程预设不支持实体建模**:空工程无 sys_menu/权限体系,但实体建模能力不应缺一角。拒绝,改用条件渲染分叉(复用 ADR-0001 验证的条件文件机制)。

## Consequences

- **前端产物契约**:每实体四件,TS + `<script setup>`;表单页走完整后台前端预置的**静态隐式路由**(`/:module/:entity/form/:id?`),不进菜单表,菜单 SQL 只产列表页菜单项。
- **控件映射**:字符串→el-input、长文本→el-textarea、数值→el-input-number、布尔→el-switch、日期→el-date-picker、枚举→el-select 绑字典;**字段级元数据最小集** = 是否列表显示 / 是否搜索条件 / 是否必填,建模表单逐字段采集。
- **init.sql 契约**:方言无关、不写死 id、不幂等(人工执行一次);菜单 1 条(menu_type='C',component 指向列表页)+ 按钮 4 条(`<模块>:<实体>:query/add/edit/remove`,无 export——Excel 导出已被 #9 排除)+ 枚举字段的字典类型与数据;挂载点为骨架预置的「业务功能」目录型菜单(固定 id),生成时可输入其他父菜单 id 覆盖;模块名逐实体输入,默认 `business`。
- **预设分叉**:空工程预设下同一套模板条件渲染——页面无 `v-hasPermi`,不产 init.sql。
- **记入迷雾**:实体间关联(一对多/多对一)的前端表单控件与初始化 SQL 接线。
- **隐含假设**:后端列表端点提供分页 + 条件查询契约(若依风格),与 ADR-0002 的六件对齐,实现期校验。

依据:拍板票据 <https://github.com/hhhrrr777/jfast/issues/10>(四轮 grilling 问答 Q1–Q13;含研究子代理对 MP 自动维护 DDL 源码语义与 RuoYi-Vue `ruoyi-generator` 模板产物的事实核查)。
