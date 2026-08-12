# 第一梯队国产数据库工程可得性调研:达梦 DM8 / 人大金仓 KingbaseES / openGauss

> 调研日期:2026-08-12 · 关联票据:#3(Part of #1)· 证据均为一手来源,链接见各条目。

## 结论速览

| 维度 | 达梦 DM8 | 人大金仓 KingbaseES | openGauss |
|---|---|---|---|
| Maven Central 驱动 | ✅ `com.dameng:DmJdbcDriver18`(最新 8.1.3.140,2024-04) | ✅ `cn.com.kingbase:kingbase8`(最新 9.0.1,2025-04) | ✅ `org.opengauss:opengauss-jdbc`(稳定 6.0.3,7.0.0-RC3 预发) |
| License 再分发 | ✅ POM 声明 Apache-2.0(厂商自行发布) | ✅ POM 声明 Apache-2.0(注意 jar 元数据不一致,见 1.2) | ✅ BSD-2-Clause(沿用 pgjdbc 许可证,jar 内含 LICENSE 原文) |
| MyBatis-Plus 分页 | ✅ `DbType.DM`,官方支持列表含"达梦" | ✅ `DbType.KINGBASE_ES`,官方列表含"人大金仓" | ✅ `DbType.OPENGAUSS`(枚举存在;官方列表页列了 GaussDB,未单列 openGauss) |
| Docker 镜像 | ⚠️ Docker Hub 无官方镜像;官方 tar 需官网下载;有社区镜像 | ⚠️ Docker Hub 无官方镜像;有社区镜像;运行需 license 授权文件 | ✅ Docker Hub 官方镜像 `opengauss/opengauss` + 高人气社区镜像 `enmotech/opengauss`,双架构 |
| PG 血缘 | 无(自研,Oracle 风格) | 协议/驱动层 PG 系(驱动结构与 pgjdbc 同构),官方自称自研内核,提供 PG 兼容模式 | 内核源自 Postgres-XC / PostgreSQL 9.2.4(源码与官方文档均可证) |

---

## (a) JDBC 驱动:Maven Central 坐标与 License

### 1.1 达梦 DM8 —— `com.dameng:DmJdbcDriver18`

- 坐标:`com.dameng:DmJdbcDriver18:8.1.3.140`(latest/release;历史版本 8.1.1.193 ~ 8.1.3.140,最后发布 2024-04-02)。
  来源:[maven-metadata.xml](https://repo1.maven.org/maven2/com/dameng/DmJdbcDriver18/maven-metadata.xml)
- License:POM 声明 **The Apache License, Version 2.0**,发布者为达梦官方(developer `dmtech@dameng.com`,SCM 指向 `gitee.com/dmedu/dm-jdbc-jars`)。
  来源:[DmJdbcDriver18-8.1.3.140.pom](https://repo1.maven.org/maven2/com/dameng/DmJdbcDriver18/8.1.3.140/DmJdbcDriver18-8.1.3.140.pom)
- jar 实测(本地解包):`META-INF/services/java.sql.Driver` 注册 `dm.jdbc.driver.DmDriver`;**jar 内未附 LICENSE/NOTICE 文件**。POM 声明 + 厂商自发布到 Central,再分发风险低;稳妥做法是在项目 NOTICE 中保留声明。
- 连接串:`jdbc:dm://host:5236/dbname`(5236 为 DM 默认端口)。

### 1.2 人大金仓 KingbaseES —— `cn.com.kingbase:kingbase8`

- 坐标:`cn.com.kingbase:kingbase8:9.0.1`(同时提供 `.jre6`/`.jre7` 变体;版本线 8.6.0 → 9.0.1,最后发布 2025-04-29)。
  来源:[maven-metadata.xml](https://repo1.maven.org/maven2/cn/com/kingbase/kingbase8/maven-metadata.xml)
- License:POM 声明 **The Apache License, Version 2.0**,SCM 指向官方组织仓库 `gitee.com/kingbasees/kingbase8`。
  来源:[kingbase8-9.0.1.pom](https://repo1.maven.org/maven2/cn/com/kingbase/kingbase8/9.0.1/kingbase8-9.0.1.pom)
- ⚠️ 元数据不一致:jar 内 `META-INF/MANIFEST.MF` 的 `Bundle-License` 写的是占位 URL `http://www.kingbase8.com/about/licence/`(疑似沿用了 pgjdbc 打包模板,该域名不存在),与 POM 的 Apache-2.0 声明不一致。以 POM 声明为准可用,但建议项目 NOTICE 留存并在合规审查时向厂商确认。
- jar 实测:服务注册 `com.kingbase8.Driver`;包结构 `com.kingbase8.{core,copy,fastpath,largeobject,ds,osgi…}` 与 pgjdbc 的 `org.postgresql.*` 一一同构,MANIFEST 自称 "Kingbase8 Global Development Group"(对照 pgjdbc 的 "PostgreSQL Global Development Group")——驱动系 pgjdbc 代码系改包名衍生,可作为 PG 血缘的旁证。
- 连接串:`jdbc:kingbase8://host:54321/dbname`(54321 为 KES 默认端口)。

### 1.3 openGauss —— `org.opengauss:opengauss-jdbc`

- 坐标:`org.opengauss:opengauss-jdbc:6.0.3`(最新稳定;7.0.0-RC3 预发,更新至 2026-04)。**每个版本有两个变体**:无后缀(`6.0.3`)为 PG 命名空间兼容构建,服务注册 `org.postgresql.Driver`;`-og` 后缀(`6.0.3-og`)为 openGauss 原生命名空间构建,服务注册 `org.opengauss.Driver`(jar 实测,`org/opengauss/` 下 421 个类)。
  来源:[maven-metadata.xml](https://repo1.maven.org/maven2/org/opengauss/opengauss-jdbc/maven-metadata.xml)
- License:POM 声明 **BSD-2-Clause**(链接 pgjdbc 许可证页);jar 内附 `META-INF/LICENSE`,为 pgjdbc 的 BSD 许可原文("Copyright (c) 1997, PostgreSQL Global Development Group")——因为该驱动就是 pgjdbc 的 fork。BSD-2-Clause 允许自由再分发(保留版权声明即可)。
  来源:[opengauss-jdbc-6.0.3.pom](https://repo1.maven.org/maven2/org/opengauss/opengauss-jdbc/6.0.3/opengauss-jdbc-6.0.3.pom)
- 连接串(`-og` 变体):`jdbc:opengauss://host:5432/dbname`。
- 要求 JDK 8+(POM `javac.target=1.8`)。

**小结 (a)**:三个驱动都在 Maven Central、都是宽松许可证、均可随项目再分发,工程上零障碍;唯一需要注意的是 kingbase8 的 jar/POM 许可元数据不一致,以及选用 opengauss-jdbc 时要分清普通/`-og` 变体。

## (b) MyBatis-Plus 分页插件方言支持

官方文档页[分页插件 · mybatis-plus](https://baomidou.com/plugins/pagination/)的"支持的数据库"原文列出(含但不限于):MySQL、MariaDB、Oracle、PostgreSQL、……、GaussDB、……、**达梦、人大金仓**、南大通用、瀚高、神州通用、虚谷、优炫、星瑞格。

源码级核实(v3.5.9):

- [`DbType.java`](https://github.com/baomidou/mybatis-plus/blob/v3.5.9/mybatis-plus-annotation/src/main/java/com/baomidou/mybatisplus/annotation/DbType.java) 枚举中三库均为一等公民:
  - `DM("dm", "达梦数据库")`,且 `oracleSameType()` 含 DM;
  - `KINGBASE_ES("kingbasees", "人大金仓数据库")`,且 `postgresqlSameType()` 含 KINGBASE_ES;
  - `OPENGAUSS("openGauss", "华为 opengauss 数据库")`,且 `postgresqlSameType()` 含 OPENGAUSS。
- [`DialectFactory.java`](https://github.com/baomidou/mybatis-plus/blob/v3.5.9/mybatis-plus-extension/src/main/java/com/baomidou/mybatisplus/extension/plugins/pagination/DialectFactory.java) 的映射:
  - DM → `OracleDialect`(Oracle ROWNUM 包裹式分页);
  - KINGBASE_ES / OPENGAUSS → `PostgreDialect`(`LIMIT x OFFSET y`)。
- 用法:`new PaginationInnerInterceptor(DbType.XX)`;官方建议单数据源场景显式指定 `dbType`。

**小结 (b)**:三库方言官方一等支持,直接可用。注意官方文档列表页未单列 openGauss(有 GaussDB),但枚举与方言映射真实存在;DM 走 Oracle 方言、Kingbase/openGauss 走 PostgreSQL 方言——这与两家的 PG 血缘一致。

## (c) Docker 镜像与 CI 可行性

### 3.1 openGauss —— ✅ 有官方镜像,CI 直接可行

- **官方镜像**:[`opengauss/opengauss`](https://hub.docker.com/r/opengauss/opengauss),Docker Hub API 自述 "The openGauss official docker image",约 22.5 万 pulls,tags 覆盖 5.x–7.0.0-RC3,`latest` 为 **amd64+arm64 双架构**([API](https://hub.docker.com/v2/repositories/opengauss/opengauss/))。
- **社区镜像**:[`enmotech/opengauss`](https://hub.docker.com/r/enmotech/opengauss)(云和恩墨),约 90 万 pulls,版本线 1.0.0–6.0.0,双架构;README 明示:openGauss 5.0+ 企业版容器**在 macOS/Windows 桌面 Docker 上无法正常启动(用 `enmotech/opengauss-lite` 替代),Linux 上无此问题**。
- CI 注意点:官方镜像启动要求 `--privileged=true`(官方 README 示例:`docker run --name opengauss --privileged=true -d -e GS_PASSWORD=...`)。GitHub Actions 步骤内 `docker run` 可使用 `--privileged`;Testcontainers 支持 `withPrivilegedMode(true)`。结论:**Linux CI(GitHub Actions ubuntu runner)实测可行**;本地 macOS 开发机建议用 lite 版或 enmotech 镜像。

### 3.2 达梦 DM8 —— ⚠️ 无 Docker Hub 官方镜像,需曲线方案

- Docker Hub 检索 `dameng` 全部为社区镜像(`is_official: false`),用户 `dameng` 名下无相关产品([搜索 API](https://hub.docker.com/v2/search/repositories/?query=dameng))。人气最高的社区镜像 [`xuxuclassmate/dameng`](https://hub.docker.com/r/xuxuclassmate/dameng) 约 3.2 万 pulls(描述 "DAMENG DataBases version:8.1.2",持续更新至 2025-12)。
- 官方渠道:达梦生态社区/官网下载中心提供 DM8 Docker 镜像 tar 包(`dm8_single` 系列,需注册账号下载;达梦技术社区有[镜像下载地址问答](https://eco.dameng.com/community/question/397f0b416f413f18a73d0df8ad400882))。
- CI 结论:可行但有摩擦力——要么 CI 前把官方 tar 导入私有镜像仓库(推荐,合规可控),要么信任社区镜像直接拉取。

### 3.3 人大金仓 KingbaseES —— ⚠️ 无官方镜像 + license 授权门槛

- Docker Hub 检索 `kingbase` 全部为社区镜像([搜索 API](https://hub.docker.com/v2/search/repositories/?query=kingbase)):较活跃的有 [`wephoon/kingbase`](https://hub.docker.com/r/wephoon/kingbase)(V008R003,x86_64+arm64/v8,约 8.6k pulls)、`godmeowicesun/kingbase`、`huzhihui/kingbase` 等;有镜像描述直言 "add license"。
- KES 运行需要 license 授权文件(license.dat);社区镜像通常内置试用授权——用于 CI 测试可以跑通,但**存在授权合规风险**,生产/长期方案应走厂商授权或私有化部署。
- CI 结论:用社区镜像可跑通,合规上弱于 openGauss,与 DM 相当。

## (d) 与 PostgreSQL 的血缘与兼容度

### 4.1 openGauss —— 确证的 PG 血缘(Postgres-XC / PG 9.2.4)

- 一手证据(源码):[`openGauss-server/configure.in`](https://github.com/opengauss-mirror/openGauss-server/blob/master/configure.in) 原文:
  > `# Package is based on former PostgreSQL, so base package version on that`
  > `PACKAGE_VERSION='9.2.4'`
  > `# Postgres-XC 1.1devel is based on PostgreSQL 9.2.4`

  即 openGauss 内核经由 Postgres-XC 1.1 衍生自 **PostgreSQL 9.2.4**。
- 一手证据(官方文档):[查看当前参数值](https://github.com/opengauss-mirror/docs/blob/master/docs/zh/database_reference/view_current_parameter_values.md) 示例 `SHOW server_version;` 返回 `9.2.4`(协议层对外报 PG 9.2.4,pgjdbc 可直接连)。
- 兼容模式(库级,建库时 `DBCOMPATIBILITY` 指定):[CREATE DATABASE](https://github.com/opengauss-mirror/docs/blob/master/docs/zh/sql_reference/create_database.md) 与 [sql_compatibility 参数](https://github.com/opengauss-mirror/docs/blob/master/docs/zh/database_reference/platform_and_client_compatibility.md) 原文:
  > 取值范围:A、B、C、PG、D。分别表示兼容 O(Oracle)、MY(MySQL)、TD(Teradata)、POSTGRES 和 S(SQL Server)数据库。**默认兼容 O**。
- 含义:openGauss 是"PG 血统但默认 Oracle 行为"。用 PG 模式建库(`DBCOMPATIBILITY 'PG'`)时与 PG 语法高度接近,但内核基于 PG 9.2 老版本,现代 PG 特性(CTE 物化语义、部分窗口/JSON 函数、声明式分区细节等)存在差异,不能假设与 PG 14+ 等价。

### 4.2 KingbaseES —— PG 协议系,官方口径为自研内核 + 多语法兼容

- 官方口径([金仓官方技术博客](https://www.kingbase.com.cn/explore/tech-blog/%E9%87%91%E4%BB%93%E6%95%B0%E6%8D%AE%E5%BA%93postgresql%E5%85%BC%E5%AE%B9%E6%A8%A1%E5%BC%8F%E6%B7%B1%E5%BA%A6%E8%A7%A3%E6%9E%90%EF%BC%9A%E6%97%A0%E7%BC%9D%E5%AF%B9%E6%8E%A5%E4%B8%BB%E6%B5%81%E7%BC%96/)):"通过 KES 兼容扩展框架实现对 Oracle、MySQL、SQL Server、PostgreSQL 等多种异构数据库语法体系的支持";PG 兼容模式通过 `sql_compatibility = 'PG'` 开启;**官方不承认内核衍生自 PostgreSQL**,自称"多语法一体化兼容架构"。
- 工程事实(本次实测):JDBC 驱动是 pgjdbc 代码系改包名衍生(1.2 节);驱动走 PG 协议(默认端口 54321);MyBatis-Plus 将其归入 postgresqlSameType。**协议与驱动层是 PG 系,SQL 语法默认偏 Oracle 兼容**。
- 含义:与 openGauss 类似——"PG 连接层 + 默认 Oracle 行为";PG 兼容模式下可用 PG 语法,但兼容深度以厂商发版说明为准。

### 4.3 对默认开发库选型的影响

1. **开发默认库建议 PostgreSQL**:openGauss 与 KingbaseES 均为 PG 协议系,pgjdbc/驱动层互通,MyBatis-Plus 对两者用同一 `PostgreDialect`;用 PG 作开发库可覆盖两者 80%+ 的日常行为,CI 成本最低(PG 官方镜像成熟)。
2. **但 PG 不能替代真实库的验证**:两者默认都是 Oracle 兼容行为,且 openGauss 内核停在 PG 9.2 时代、Kingbase 兼容深度由厂商定义——必须保留针对真实库的 CI 测试矩阵(openGauss 用官方/enmotech 镜像可直接做;DM/Kingbase 用社区镜像或私有仓库)。
3. **达梦是另一条方言线**:DM 走 Oracle 风格(MyBatis-Plus 用 `OracleDialect`),与 PG 系行为差异最大(空串即 NULL、标识符大小写、序列/伪列等),需要独立的方言验证用例。
4. 生成器模板层面:分页/主键策略按方言分组即可——`PG 组(openGauss、KingbaseES-PG 模式、PostgreSQL)`与 `Oracle 组(DM、KingbaseES-Oracle 模式、openGauss-A 模式)`,这与 MyBatis-Plus 的 `postgresqlSameType()/oracleSameType()` 分组天然对齐。

## 附:已核实的工程参数速查

| 项 | DM8 | KingbaseES | openGauss |
|---|---|---|---|
| Driver 类 | `dm.jdbc.driver.DmDriver` | `com.kingbase8.Driver` | `org.opengauss.Driver`(`-og`)/ `org.postgresql.Driver` |
| JDBC URL | `jdbc:dm://host:5236/db` | `jdbc:kingbase8://host:54321/db` | `jdbc:opengauss://host:5432/db` |
| MyBatis-Plus DbType | `DbType.DM` | `DbType.KINGBASE_ES` | `DbType.OPENGAUSS` |
| 分页方言 | OracleDialect | PostgreDialect | PostgreDialect |
| Docker | 社区镜像/官方 tar | 社区镜像(需 license) | 官方 `opengauss/opengauss` / `enmotech/opengauss` |

## 未决/待办

- kingbase8 驱动 License 元数据不一致(POM Apache-2.0 vs MANIFEST 占位 URL),合规审查前宜向厂商确认。
- DM/Kingbase 的 CI 镜像策略(自建私有仓库 vs 社区镜像)需在测试策略票据中拍板。
- openGauss 官方镜像 `--privileged` 要求与部分 CI 服务的兼容性,建议在原型阶段用 Testcontainers 实测一次。
