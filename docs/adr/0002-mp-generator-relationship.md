# 与 MyBatis-Plus 代码生成器的关系:用 MP 的库,不用 MP 的生成器

jfast 实体建模**自研生成**,全部走已有的 Freemarker「目录即模板」管线(ADR-0001),不调用/包装 MP 代码生成器;目标应用数据层照常使用 MyBatis-Plus 运行时(BaseMapper/QueryWrapper/分页等插件)。核心论证:① 输入模型方向相反——MP 生成器是表驱动(JDBC 连接 + 表名清单,从活库读元数据),jfast 是模型驱动,套娃需「模型→DDL→临时库→读回元数据→生成」四段折返跑,纯迁就 MP 输入形态,不创造价值;② MP 生成器产物(持久层骨架)只是实体建模产物的一小块,前端/分层/权限/菜单本就只能自研,套娃不减少任何关键模板,反增第二套生成体系与跟随 MP 生成器 API 变更的维护成本;③ 「目录即模板」管线已为自研备好,实体建模只是加模板。

## Considered Options

- **套娃(调用/包装 MP 生成器)**:生成体系裂成两套(MP 引擎配置 + jfast Freemarker 管线),模板控制度受 MP 的 TableInfo 抽象约束,且生成时要求可读的活数据库——信创场景(达梦/金仓/openGauss)意味着生成期就要备齐目标库实例。拒绝。
- **混引 JPA 仅为 Hibernate `ddl-auto` 建表**:Hibernate 方言没有达梦/人大金仓/openGauss,国产库上建表正确性无保障;另有 jsqlparser 依赖冲突等已知坑。拒绝。
- **目标应用引入 dromara/auto-table(注解驱动启动建表)**:活跃且原生支持达梦/金仓,但 openGauss 缺席;每个生成物多背一个第三方运行时依赖;「启动时自动改表」在企业生产环境(尤其信创甲方)通常被 DBA 禁止。拒绝。
- **actable(MyBatis 注解建表)**:停更于 2021、仅 MySQL、不支持 Spring Boot 3。排除。
- **生成 Liquibase/Flyway changelog**:工程规范最好,但两者对三个国产库均无官方支持(金仓/openGauss 可套 PG 方言,达梦为 Oracle 血统需社区扩展),且实体变更需生成增量 changeset,生成器复杂度上一个台阶。MVP 拒绝,远期视用户反馈再议(已记入地图迷雾)。

## Consequences

- **实体建模 MVP 产物 = 每实体后端竖切六件**:Entity / Mapper / Mapper XML(每实体一个骨架文件,确立目录与命名约定,不需要可删)/ Service / ServiceImpl / Controller,外加 DTO/VO 分层 + Bean Validation 校验 + Controller 权限注解(回应调研中「RuoYi Entity 与 DTO/VO 混用」痛点)。
- **前端页面与菜单 SQL 移出 MVP**,由后续票据「拍板:实体建模前端 CRUD 模板与菜单 SQL 接线」(#10)承接。
- **模型→表**:jfast 按方言生成 SQL DDL 脚本,目标应用用 MP 3.5.3+ 自带「自动维护 DDL」启动时执行;方言分层为 MySQL / PostgreSQL(复用于人大金仓、openGauss)/ 达梦单独,MVP 产 MySQL + PG,达梦方言记入迷雾。
- **判出范围**:从存量数据库反向导入(表→模型)不做;目标应用内置运行时代码生成器(若依式)不做,生成职责归 jfast CLI。

依据:拍板票据 <https://github.com/hhhrrr777/jfast/issues/8>(含研究子代理对 MP 官方文档、auto-table/actable 维护状态、Liquibase/Flyway 国产库支持矩阵的事实核查)、调研 `docs/research/competitive-scan.md`(分支 `research/competitive-scan`)。
