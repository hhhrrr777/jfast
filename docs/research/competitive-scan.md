# 竞品调研:国产后台脚手架的功能基线

> 对应 Issue: hhhrrr777/jfast #2
> 调研日期: 2026-08-12
> 方法: 以官方 GitHub 仓库 README、官方文档站点、官方文档源码及 DeepWiki(基于仓库源码问答)为一手来源,社区痛点部分引用 GitHub/Gitee Issues 与中文开发者社区文章。

## 1. 概述

本次调研覆盖四类产品:RuoYi-Vue(表驱动后台脚手架)、JeecgBoot(低代码平台型脚手架)、MyBatis-Plus 代码生成器(持久层增强工具及其生成器)、JHipster(模型驱动的国际全栈生成器)。核心结论:国产后台脚手架的「基线功能集」高度收敛——用户/角色/菜单/部门/字典/参数/公告/日志(操作+登录)/在线用户/定时任务/服务监控/缓存监控/代码生成,几乎就是 RuoYi 的 18 项内置功能,JeecgBoot 在此基础上向低代码(Online 表单、报表、大屏、工作流、AI)扩张;而 JHipster 走另一条路——模型驱动(JDL)+ 运维向管理界面(Metrics/Health/Logs/Configuration/Audits),不提供字典、定时任务这类中式后台标配。生成器输入形态上,国产阵营(RuoYi、JeecgBoot、MyBatis-Plus)全部是「数据库表结构驱动」,JHipster 是唯一主流的「模型驱动」代表。

## 2. RuoYi-Vue(若依)

仓库: <https://github.com/yangzongzhuan/RuoYi-Vue>

### 2.1 默认功能清单

以下 18 项为官方 README「内置功能」一节原文逐条列出(来源: [RuoYi-Vue README](https://github.com/yangzongzhuan/RuoYi-Vue/blob/master/README.md),并经 DeepWiki 对照 `sql/ry_20250522.sql` 菜单初始化数据核实):

| 分类 | 功能 | 说明(README 原文要点) |
|---|---|---|
| 系统管理 | 用户管理 | 系统用户配置 |
| 系统管理 | 部门管理 | 组织机构树结构,支持数据权限 |
| 系统管理 | 岗位管理 | 用户职务配置 |
| 系统管理 | 菜单管理 | 菜单、操作权限、按钮权限标识 |
| 系统管理 | 角色管理 | 菜单权限分配,按机构划分数据范围权限 |
| 系统管理 | 字典管理 | 维护系统较固定的常用数据 |
| 系统管理 | 参数管理 | 动态配置常用参数 |
| 系统管理 | 通知公告 | 通知公告信息发布维护 |
| 系统管理 | 操作日志 | 正常操作及异常信息日志记录查询 |
| 系统管理 | 登录日志 | 登录日志记录查询(含登录异常) |
| 系统监控 | 在线用户 | 当前活跃用户状态监控 |
| 系统监控 | 定时任务 | 在线任务调度(增删改),含执行结果日志 |
| 系统监控 | 服务监控 | CPU、内存、磁盘、堆栈等 |
| 系统监控 | 缓存监控 | 缓存信息查询、命令统计 |
| 系统监控 | 连接池监视(数据监控) | 数据库连接池状态,可分析 SQL 找性能瓶颈 |
| 系统工具 | 代码生成 | 前后端代码生成(java、html、xml、sql)支持 CRUD 下载 |
| 系统工具 | 系统接口 | 根据业务代码自动生成 API 接口文档(Swagger) |
| 系统工具 | 在线构建器(表单构建) | 拖动表单元素生成 HTML |

技术栈: 前端 Vue + Element UI(另有 Vue3 + Element Plus 分支);后端 Spring Boot + Spring Security + Redis & JWT,权限认证用 JWT、支持多终端认证与动态权限菜单(来源: 同上 README)。分支策略: master 为 Spring Boot 4.x(JDK 17+),另有 Spring Boot 2.x 分支支持 JDK 8+。

### 2.2 生成器输入形态:表结构驱动

RuoYi 代码生成器**以数据库表结构为输入**:用户在「代码生成」页面查询数据库表列表→「导入表」把表元数据(表名、列名、列类型)写入 `gen_table` / `gen_table_column` 两张配置表→在页面上微调字段的 Java 属性、查询方式、显示类型等→基于 Velocity 模板生成前后端代码并打包下载。也可先用 SQL 建表再导入。(来源: DeepWiki 对 `GenController`/`gen_table` 源码的分析 <https://deepwiki.com/yangzongzhuan/RuoYi-Vue>;README 代码生成条目)

即「先有表,后有代码」——数据库 schema 是唯一的模型事实源。

## 3. JeecgBoot

仓库: <https://github.com/jeecgboot/JeecgBoot>

### 3.1 默认功能清单

以下清单来自官方 README 功能介绍(来源: [JeecgBoot README](https://github.com/jeecgboot/JeecgBoot/blob/master/README.md),DeepWiki 核实):

| 分类 | 功能 |
|---|---|
| 系统管理 | 用户管理、角色管理、菜单管理、首页配置、部门管理、我的部门(二级管理员)、字典管理、分类字典、系统公告、职务管理、通讯录 |
| 权限体系 | 按钮权限、数据权限、表单权限(字段禁用/隐藏) |
| 平台能力 | 多数据源管理、白名单管理、第三方配置(钉钉/企业微信)、多租户管理(租户、租户角色、套餐) |
| 低代码(Online 开发) | Online 在线表单、Online 代码生成器、Online 在线报表、仪表盘设计器、编码规则、校验规则 |
| 设计器 | 表单设计器、报表设计器(JimuReport,可导出 PDF/Excel/Word)、大屏/仪表盘设计器(JimuBI) |
| 工作流 | Flowable 流程引擎、在线流程设计、自定义表单挂靠、流程与表单分离 |
| 代码生成器 | 一键生成前后端代码,4 套模板(单表/树/一对多/一对一),自定义模板,生成代码自带 Excel 导入导出、查询过滤器、高级查询器,提供 uniapp3 移动端模板 |
| 系统监控 | Redis、Tomcat、JVM、SQL 监控、请求追踪、磁盘监控、在线用户 |
| 日志 | 系统日志、数据日志(数据快照对比) |
| 消息 | 消息中心(短信/邮件/微信推送)、WebSocket 通知 |
| 开放能力 | OpenAPI(AK/SK 认证鉴权)、CAS 单点登录、多语言国际化、分布式文件(MinIO/阿里 OSS)、Docker |
| AI 应用 | AI 知识库问答、AI 大模型管理(ChatGPT/DeepSeek/Ollama/智谱/千问)、AI 流程编排、AI 建表(自然语言生成 Online 表单) |
| 移动端 | Uniapp3(H5、小程序、APP、鸿蒙 Next) |

### 3.2 生成器输入形态:表结构驱动为主 + 在线配置 + 自然语言

JeecgBoot 有三层输入形态(来源: README + DeepWiki <https://deepwiki.com/jeecgboot/JeecgBoot>):

1. **表结构驱动**:经典代码生成器与 RuoYi 同构,基于已有数据库表生成前后端 CRUD(README: "代码生成器功能(一键生成前后端代码,生成后无需修改直接用)")。
2. **在线配置驱动(Online 表单)**:在网页上通过表单设计器配置字段、布局、校验规则,由平台把配置同步为数据库表并直接产生可运行的增删改查,无需生成代码文件("在线表单(无需编码,通过在线配置表单,实现表单的增删改查,支持单表、树、一对多、一对一等模型)")。
3. **自然语言驱动(AI)**:新版本通过 AI Skills(如 `jeecg-codegen`、`jeecg-onlform`)把自然语言需求转换为全套 CRUD 代码或 Online 表单(DeepWiki 核实)。

本质仍是「表优先」:Online 配置的最终落地动作也是建表/同步表。

## 4. MyBatis-Plus 代码生成器及生态

仓库: <https://github.com/baomidou/mybatis-plus>;官方文档: <https://baomidou.com>

### 4.1 定位与"功能清单"

MyBatis-Plus **不是后台脚手架**,而是 MyBatis 的增强工具("只做增强不做改变"),因此没有用户/角色/菜单之类的业务功能。它提供给应用的开箱能力是持久层基础设施(来源: <https://baomidou.com/getting-started/> 及官方文档):

| 能力 | 说明 |
|---|---|
| BaseMapper 通用 CRUD | 继承即拥有单表增删改查,无需 XML("甚至连 XML 文件都不用编写") |
| 条件构造器 | QueryWrapper / LambdaQueryWrapper 类型安全动态查询 |
| 插件体系 | 分页、乐观锁、防全表更新、多租户、动态表名 |
| 通用功能 | 逻辑删除、自动填充、多数据源 |
| 代码生成器 | 见下节 |

### 4.2 生成器输入形态:数据库表结构元数据驱动

依据官方文档「代码生成器」页(来源: <https://baomidou.com/guides/code-generator/>):

- 输入 = **数据库连接 + 表名清单**。通过 `DataSourceConfig` 配置 JDBC 连接,生成器查询数据库元数据获得表(`TableInfo`)与字段(`TableField`)信息;`strategy.setInclude("表名,多个英文逗号分割")` 指定要生成的表。
- 产物: "可以快速生成 Entity、Mapper、Mapper XML、Service、Controller 等各个模块的代码";另可通过 `InjectionConfig` + `CustomFile` 生成 DTO/VO 等自定义文件。
- 模板引擎: Velocity(默认)、Freemarker、Beetl,可继承 `AbstractTemplateEngine` 自定义。
- 版本注意: 3.5.1 以下用老生成器,3.5.1+ 推荐新生成器;3.0.3 起 `mybatis-plus-generator` 与模板引擎依赖需手动添加。

### 4.3 与 RuoYi 类脚手架的关系

MyBatis-Plus 生成器是**组件级工具**,只生成持久层+控制层骨架,不含权限、菜单、前端页面;RuoYi 类脚手架是**完整应用模板**,内置了整套系统管理功能和自己的生成器(RuoYi 的生成器产物即基于 MyBatis;RuoYi-Vue-Plus 等衍生版直接采用 MyBatis-Plus)。二者是「零件」与「整车」的关系,MP 生成器常作为这类脚手架的底层零件被集成或替代。(来源: DeepWiki 对 baomidou/mybatis-plus 的分析 <https://deepwiki.com/baomidou/mybatis-plus>)

## 5. JHipster 默认生成的应用

仓库: <https://github.com/jhipster/generator-jhipster>;官网: <https://www.jhipster.tech>

### 5.1 默认功能清单

JHipster 生成的单体应用默认 Administration 菜单包含以下页面(来源: DeepWiki 对生成器前端模板(Angular/React/Vue)的核实 <https://deepwiki.com/jhipster/generator-jhipster>,各页面均有对应前端模块文件):

| 功能 | 说明 |
|---|---|
| User management | 用户管理(增删改查、角色分配);另有注册/登录/账号激活/密码重置全流程 |
| Audits | 审计日志(记录认证/授权事件;`AUDITS` 为 JHipster 保留关键字) |
| Metrics | JVM、HTTP 请求等性能指标(基于 Dropwizard Metrics) |
| Health | 健康检查(Spring Boot Actuator) |
| Configuration | 运行时配置查看 |
| Logs | 日志级别运行时查看与调整(Logback) |
| API | Swagger/OpenAPI 文档界面 |
| Tracker | (可选)WebSocket 用户活动追踪 |
| Gateway | (微服务网关应用)路由管理 |
| Database | (开发环境用 H2 时)数据库控制台 |

技术栈与生产就绪能力(来源: 官方 tech-stack 文档 <https://www.jhipster.tech/tech-stack/>,经文档源码仓库核实):

- 前端: Angular / React / Vue 三选一,Bootstrap 响应式,完整国际化;
- 后端: Spring Boot + Spring Security + Spring MVC REST + Spring Data JPA + Bean Validation + Liquibase 数据库变更;Maven 或 Gradle;dev/prod 双 profile;
- 认证: JWT、OAuth2、Session 三选一(README 构建管道核实);
- 数据库: SQL(JPA)或 NoSQL(MongoDB/Couchbase/Cassandra),可选 Elasticsearch、Kafka/Pulsar;
- 微服务: Spring Cloud Gateway + Consul/Eureka 可选;
- 生产就绪: "Monitoring with Metrics and the ELK Stack"、缓存(ehcache/Caffeine/Hazelcast/Redis 等)、HikariCP、Logback 运行时配置、Docker/Docker Compose、主流云平台支持。

**注意与国产脚手架的差异**:JHipster 默认**没有**菜单管理、字典管理、参数设置、部门/岗位、定时任务、在线用户强制下线这类「中式后台」功能;它的管理界面偏运维(health/metrics/logs/configuration/audits)而非业务配置。

### 5.2 生成器输入形态:模型驱动(JDL)

JHipster 是四者中唯一的**模型驱动**代表(来源: 官方 JDL 文档 <https://www.jhipster.tech/jdl/intro>,经文档源码仓库核实):

- 输入 = **JDL(JHipster Domain Language)文件**,用声明式语法在单个文件中描述应用、部署、实体及其关系,与数据库无关;
- 官方提供在线工具 JDL-Studio(start.jhipster.tech/jdl-studio)与 IntelliJ/Eclipse/VS Code 插件做可视化建模,JDL 是官方「recommended approach」,用于替代交互式问答式的 entity 子生成器;
- JDL 示例(官方文档):

```jdl
entity BankAccount {
  name String required
  balance BigDecimal required
  openingDay LocalDate
  active Boolean
}
```

- 生成器根据 JDL 产出后端实体/Repository/REST + 前端 CRUD 页面 + Liquibase 变更集,再由 Liquibase 建表——即「先有模型,后有表」,与国产阵营的「先有表,后有代码」方向相反。

## 6. 独立开发者/中文社区:痛点与好评点

### 6.1 RuoYi(若依)

**好评点**:

- 简单、不过度封装、二开自由度高。Gitee 热帖《ruoyi后端代码最大的问题》下的高赞反驳观点认为"做框架越简单越好,给使用者更大自由度"(来源: [Gitee Issue #I8X0AO](https://gitee.com/y_project/RuoYi-Vue/issues/I8X0AO));
- 上手快、资料多、生态活跃,"坑容易搜到";社区共识是"想长期维护、掌控代码选 RuoYi"(来源: [掘金对比文](https://juejin.cn/post/7643007657208692786)、[博客园框架对比](https://www.cnblogs.com/longkui-site/p/19293585))。

**痛点**:

- 代码生成器生成质量不高、仍需大量手工调整;PostgreSQL 版获取表列表/字段信息有 Bug、Java 类型识别错误(时间类型识别为 String)(来源: [Gitee Issue #I6KWTY](https://gitee.com/y_project/RuoYi/issues/I6KWTY)、[CSDN 评测](https://blog.csdn.net/m0_73647713/article/details/134384406));
- 树表/主子表生成有坑,需手动改 Mapper SQL 与 Controller(来源: [博客园:树表结构的坑](https://www.cnblogs.com/wyj-java/p/15749263.html)、[Gitee Issue #I41RAG](https://gitee.com/y_project/RuoYi/issues/I41RAG));
- 分层不规范:Entity 与 DTO/VO 混用,"搞不清楚字段是数据库的还是前端需要的"(来源: [Gitee Issue #I8X0AO](https://gitee.com/y_project/RuoYi-Vue/issues/I8X0AO));
- 周边配置坑:Windows 定时任务 NoSuchMethodException、Shiro 匿名访问失效、RuoYi-Cloud 跨域配置难(来源: [CSDN 坑点汇总](https://blog.csdn.net/weixin_45623983/article/details/102675962)、[Gitee Issue #I9JWDN](https://gitee.com/y_project/RuoYi-Cloud/issues/I9JWDN));
- 功能偏基础,低代码/报表/大屏需自行集成第三方。

### 6.2 JeecgBoot

**好评点**:

- 低代码能力最强:Online 表单/报表/大屏/流程设计器完整,"一键生成前后端代码,生成后无需修改直接用,绝对是后端开发福音"(来源: [JeecgBoot README](https://github.com/jeecgboot/JeecgBoot/blob/master/README.md));
- 交付压力大、表单报表流程多的场景(ERP/CRM/审批)首选;新版本 AI 能力(一句话生成)更新积极(来源: [掘金对比文](https://juejin.cn/post/7643007657208692786));
- 被社区称为"接私活儿神器"(来源: [博客园转载](https://www.cnblogs.com/javastack/p/15798401.html))。

**痛点**:

- 技术栈多、概念多、学习曲线比 RuoYi 陡;"黑盒感更强",Online 页面的性能、深度定制与调试被吐槽(来源: [掘金对比文](https://juejin.cn/post/7643007657208692786)、[博客园对比](https://www.cnblogs.com/longkui-site/p/19293585));
- 深度二开时受平台约束,代码可控性不如 RuoYi(同上);
- 复杂问题更依赖官方文档/群,部分高级文档/功能收费;授权与商用边界在知乎有专门讨论(来源: [知乎提问](https://www.zhihu.com/question/587275089))。

### 6.3 MyBatis-Plus

**好评点**:

- "只做增强不做改变",存量 MyBatis 项目零成本迁移;
- 单表 CRUD 零 SQL,分页一行配置,社区反馈可减少 50%~80% 持久层重复代码(来源: [博客园:10 个坑(兼评价)](https://www.cnblogs.com/12lisu/p/19935756)、[腾讯云实战文](https://cloud.tencent.cn/developer/article/2622339));
- 中文文档完善、社区热度高(来源: [华为云博客](https://bbs.huaweicloud.com/blogs/372165))。

**痛点**:

- `IService`/`ServiceImpl` 对 Service 层"侵入"争议:SQL 操作渗入上层,破坏三层架构清晰度(来源: [华为云博客](https://bbs.huaweicloud.com/blogs/372165));
- SQL 黑盒化:排错定位难、内置方法默认 `SELECT *` 有性能隐患(来源: [博客园:10 个坑](https://www.cnblogs.com/12lisu/p/19935756));
- 多表复杂查询仍需回归 XML 手写 SQL;字符串字段名"魔法值"问题(同上)。

### 6.4 JHipster

**好评点**:

- 全栈一体化:设计好实体后直接生成单表 CRUD 接口,项目启动时间大幅缩短;技术栈现代、生产就绪(监控/缓存/Docker 齐全)(来源: [CSDN 使用反馈](https://blog.csdn.net/qq_42428264/article/details/119910299)、[姚伟斌:JHipster 现代全栈开发利器](https://yaoweibin.cn/jhipster-%e7%8e%b0%e4%bb%a3%e5%85%a8%e6%a0%88%e5%bc%80%e5%8f%91%e5%88%a9%e5%99%a8/));
- 模型驱动 + Liquibase 的规范工程实践受推崇(同上)。

**痛点**:

- 学习曲线陡峭:需同时掌握 Spring Boot + 前端框架 + JDL,对新手不友好;
- 项目结构重量级、目录格式不能轻易改动,定制灵活性受限;中小项目"显得过重"(来源: [CSDN 使用反馈](https://blog.csdn.net/qq_42428264/article/details/119910299)、[CSDN 脚手架对比](https://blog.csdn.net/gitblog_00672/article/details/155516774));
- 国内社区/中文资料远少于若依生态,遇到问题更依赖英文社区(综合对比文,同上)。

### 6.5 综合观察

- 中文社区对国产脚手架的期待集中在:**生成代码"生成后能直接用"、少手工调整、分层干净(DTO/VO 分离)、多数据库支持、二开不被框架绑架**;
- 普遍的不满模式是:**表驱动生成器对非标表(树表/主子表/PG 等非 MySQL 库)支持脆弱**——这恰是模型驱动(JHipster 路线)声称能规避的问题;
- 轻与重的张力:RuoYi 因"简单"被原谅,JeecgBoot 因"重、黑盒"被吐槽,JHipster 因"重且学习陡"在国内水土不服——独立开发者明显偏爱"看得懂、改得动"的代码。

## 7. 国产后台基线功能清单(P0/P1/P2)

覆盖标记: RY=RuoYi-Vue, JB=JeecgBoot, MP=MyBatis-Plus(仅持久层能力), JH=JHipster。

### P0 — 基线必备(缺了会被用户直接放弃)

| # | 功能 | 覆盖 |
|---|---|---|
| 1 | 用户管理(增删改查、启用禁用、重置密码) | RY / JB / JH |
| 2 | 角色管理与权限分配 | RY / JB / JH |
| 3 | 菜单管理(动态路由 + 按钮级权限标识) | RY / JB(JH 无,权限为静态角色) |
| 4 | 登录认证(JWT 或多端 token)+ 登录日志 | RY / JB / JH(登录日志为 RY/JB 特色) |
| 5 | 操作日志(含异常记录) | RY / JB(JH 仅有审计认证事件的 Audits) |
| 6 | 字典管理(固定枚举数据维护) | RY / JB(JH 无) |
| 7 | 代码生成器(一键生成前后端 CRUD) | RY / JB / MP(仅后端持久层) / JH(JDL) |
| 8 | 系统接口文档(Swagger/OpenAPI) | RY / JB / JH |
| 9 | 参数/配置管理 | RY / JB(JH 的 Configuration 仅只读查看) |

### P1 — 常见期望(多数用户认为"应该有")

| # | 功能 | 覆盖 |
|---|---|---|
| 1 | 部门/组织机构(树形 + 数据权限) | RY / JB |
| 2 | 岗位/职务管理 | RY / JB |
| 3 | 通知公告 | RY / JB |
| 4 | 在线用户监控(可强退) | RY / JB(JH 的 Tracker 仅显示活动) |
| 5 | 定时任务(在线调度 + 执行日志) | RY / JB(JH 无) |
| 6 | 服务监控(CPU/内存/磁盘/JVM) | RY / JB / JH(Metrics/Health) |
| 7 | 缓存监控 | RY(JB 有 Redis 监控) |
| 8 | 数据权限(按机构范围) | RY / JB |
| 9 | 数据导入导出(Excel) | JB(生成代码自带) / RY(工具类) |
| 10 | 文件上传管理 | JB(分布式文件) / RY(基础) |
| 11 | 表单构建器(拖拽生成表单) | RY(在线构建器) / JB(表单设计器) |
| 12 | 连接池/SQL 监控 | RY(Druid 监视) / JB(SQL 监控) |
| 13 | 国际化 | JB / JH(RY 无) |

### P2 — 加分项(差异化竞争)

| # | 功能 | 覆盖 |
|---|---|---|
| 1 | 报表设计器 / 大屏设计器 | JB(JimuReport/JimuBI) |
| 2 | 工作流引擎(Flowable)与流程设计器 | JB |
| 3 | 多租户(SaaS) | JB(RY 官方版无,衍生版 Plus/Pro 有) |
| 4 | 消息中心(短信/邮件/IM 推送)+ WebSocket 通知 | JB(JH 有可选 WebSocket) |
| 5 | 单点登录(CAS/OAuth2 对接) | JB / JH(OAuth2) |
| 6 | AI 能力(知识库问答、自然语言生成代码/表单) | JB(新版主打) |
| 7 | 微服务/网关形态 | JB / JH / RY(RuoYi-Cloud 独立仓库) |
| 8 | 移动端(uniapp/小程序) | JB(RY 有衍生移动端生态) |
| 9 | 数据日志(快照对比) | JB |
| 10 | 审计(Audits)/运维级监控(ELK) | JH 特色 |

**对 jfast 的启示(简要)**: P0 清单基本就是 RuoYi 18 项内置功能去掉运维向条目后的交集;其中「字典管理」「菜单管理+按钮权限」「操作/登录日志」「表驱动代码生成」是国产用户心智中的"后台标配",JHipster 恰恰全都不提供——这是国产脚手架与国际生成器最本质的功能分野。生成器层面,「生成后无需大量手工调整(树表、主子表、非 MySQL 数据库也要稳)」是社区对现有工具最集中的不满,也是差异化空间所在。

## 8. 来源清单

一手来源(官方仓库/文档):

- RuoYi-Vue README: <https://github.com/yangzongzhuan/RuoYi-Vue/blob/master/README.md>
- RuoYi-Vue DeepWiki(基于源码): <https://deepwiki.com/yangzongzhuan/RuoYi-Vue>
- JeecgBoot README: <https://github.com/jeecgboot/JeecgBoot/blob/master/README.md>
- JeecgBoot DeepWiki: <https://deepwiki.com/jeecgboot/JeecgBoot>
- MyBatis-Plus 官方文档-快速开始: <https://baomidou.com/getting-started/>
- MyBatis-Plus 官方文档-代码生成器: <https://baomidou.com/guides/code-generator/>
- MyBatis-Plus DeepWiki: <https://deepwiki.com/baomidou/mybatis-plus>
- JHipster generator-jhipster README: <https://github.com/jhipster/generator-jhipster/blob/main/README.md>
- JHipster 官方文档-Technology stack: <https://www.jhipster.tech/tech-stack/>(文档源码: <https://github.com/jhipster/jhipster.github.io/blob/main/docs/about/tech-stack.mdx>)
- JHipster 官方文档-JDL intro: <https://www.jhipster.tech/jdl/intro>(文档源码: <https://github.com/jhipster/jhipster.github.io/blob/main/docs/jdl/intro.mdx>)
- JHipster DeepWiki: <https://deepwiki.com/jhipster/generator-jhipster>

社区来源(痛点/好评):

- Gitee 若依 Issue《ruoyi后端代码最大的问题》: <https://gitee.com/y_project/RuoYi-Vue/issues/I8X0AO>
- Gitee 若依 Issue《RuoYi多模块PostgreSQL版本的代码生成模块有问题》: <https://gitee.com/y_project/RuoYi/issues/I6KWTY>
- Gitee 若依 Issue《主子表提交中visible:false问题》: <https://gitee.com/y_project/RuoYi/issues/I41RAG>
- Gitee RuoYi-Cloud Issue《通过gateway解决前后端开发环境下跨域问题》: <https://gitee.com/y_project/RuoYi-Cloud/issues/I9JWDN>
- 若依框架优缺点(CSDN): <https://blog.csdn.net/m0_73647713/article/details/134384406>
- 若依ruoyi代码生成树表结构的那些坑(博客园): <https://www.cnblogs.com/wyj-java/p/15749263.html>
- ruoyi框架坑点(CSDN): <https://blog.csdn.net/weixin_45623983/article/details/102675962>
- Forge Admin vs RuoYi vs JeecgBoot 对比(掘金): <https://juejin.cn/post/7643007657208692786>
- Java常见开发框架大比拼(博客园): <https://www.cnblogs.com/longkui-site/p/19293585>
- 神仙接私活儿项目(博客园): <https://www.cnblogs.com/javastack/p/15798401.html>
- jeecgboot开源版商用讨论(知乎): <https://www.zhihu.com/question/587275089>
- 聊聊Mybatis-Plus中的10个坑(博客园): <https://www.cnblogs.com/12lisu/p/19935756>
- 为什么Mybatis-plus这么好用反而用的不多(华为云): <https://bbs.huaweicloud.com/blogs/372165>
- MyBatis-Plus实战指南(腾讯云): <https://cloud.tencent.cn/developer/article/2622339>
- 使用JHipster快速搭建一个项目(CSDN): <https://blog.csdn.net/qq_42428264/article/details/119910299>
- JHipster现代全栈开发利器(姚伟斌博客): <https://yaoweibin.cn/jhipster-%e7%8e%b0%e4%bb%a3%e5%85%a8%e6%a0%88%e5%bc%80%e5%8f%91%e5%88%a9%e5%99%a8/>
- Spring项目脚手架工具对比(CSDN): <https://blog.csdn.net/gitblog_00672/article/details/155516774>
