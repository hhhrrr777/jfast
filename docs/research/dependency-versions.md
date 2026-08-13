# 依赖版本定版:目标应用与生成器

> 对应 Issue: hhhrrr777/jfast #17(地图 Issue #15 下研究票据)
> 调研日期: 2026-08-13
> 方法: 全部版本事实取自一手来源——Maven Central maven-metadata.xml / POM / jar 实测、npm registry dist-tags、GitHub Releases API、官方文档站点(Spring、baomidou.com、jline.org)。下文每条结论均附来源 URL。

## 定版总表

| 侧 | 依赖 | 定版版本 | 来源 |
|---|---|---|---|
| 目标应用·后端 | Spring Boot(3.x 线) | **3.5.16** | [Maven Central](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml) |
| 目标应用·后端 | MyBatis-Plus(`mybatis-plus-spring-boot3-starter`) | **3.5.17** | [Maven Central](https://repo1.maven.org/maven2/com/baomidou/mybatis-plus-spring-boot3-starter/maven-metadata.xml) |
| 目标应用·后端 | MyBatis-Plus 分页插件配套(`mybatis-plus-jsqlparser`) | **3.5.17** | [Maven Central](https://repo1.maven.org/maven2/com/baomidou/mybatis-plus-jsqlparser/maven-metadata.xml) |
| 目标应用·后端 | jjwt(`jjwt-api` / `jjwt-impl` / `jjwt-jackson`) | **0.13.0** | [Maven Central](https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-api/maven-metadata.xml) · [GitHub Release](https://github.com/jwtk/jjwt/releases/latest) |
| 目标应用·前端 | Vue | **3.5.41** | [npm registry](https://registry.npmjs.org/vue/latest) |
| 目标应用·前端 | Vite | **8.2.1** | [npm registry](https://registry.npmjs.org/vite/latest) |
| 目标应用·前端 | Element Plus | **2.14.4** | [npm registry](https://registry.npmjs.org/element-plus/latest) |
| 目标应用·前端 | axios | **1.19.0** | [npm registry](https://registry.npmjs.org/axios/latest) |
| 目标应用·前端 | TypeScript | **~5.9.3**(理由见 §7,不用 7.x) | [npm registry](https://registry.npmjs.org/typescript) |
| 生成器 | picocli | **4.7.7** | [Maven Central](https://repo1.maven.org/maven2/info/picocli/picocli/maven-metadata.xml) · [GitHub Release](https://github.com/remkop/picocli/releases/latest) |
| 生成器 | JLine(`jline-terminal` + `jline-terminal-jni`) | **4.3.1** | [Maven Central](https://repo1.maven.org/maven2/org/jline/jline-terminal/maven-metadata.xml) · [jline.org](https://jline.org/versions/4.0/docs/modules/terminal-providers) |
| 生成器 | Freemarker(`org.freemarker:freemarker`) | **2.3.34** | [Maven Central](https://repo1.maven.org/maven2/org/freemarker/freemarker/maven-metadata.xml) |

---

## 1. Spring Boot 3.x → 3.5.16

- **事实**:Maven Central `spring-boot-starter-parent` 元数据中 3.x 线最新稳定版为 **3.5.16**(2026-06 发布列车);其后为 4.0.x / 4.1.0(最新 major 为 4.1.0,不在本次定版范围——ADR-0002/0003 既定目标栈为 Spring Boot 3)。
- **JDK 基线**:官方系统要求文档原文:"Spring Boot 3.5.16 requires at least **Java 17** and is compatible with versions up to and including **Java 25**." JDK 21 落在兼容区间内,且 3.2+ 起含虚拟线程支持。
- 来源: <https://docs.spring.io/spring-boot/3.5/system-requirements.html> ;元数据 <https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml>
- **定版**: 3.5.16。3.5 是 3.x 末代 feature 线,OSS 支持已随 4.x GA 接近尾声,但因目标栈既定 Spring Boot 3,取 3.x 线最新补丁版即可;升级 4.x 属另一决策(出 MVP 范围)。

## 2. MyBatis-Plus → 3.5.17(boot3 starter)

- **自动维护 DDL 引入版本**:官方文档原文——"在 MyBatis-Plus 的 `3.5.3+` 版本中,引入了……表结构的自动维护";存储过程支持自 `3.5.3.2` 起;`DdlApplicationRunner` 部分自定义属性自 `3.5.11` 起。使用 starter 时"会自动实例化一个 DdlApplicationRunner 实例来执行 DDL 脚本",通过实现 `IDdl` 接口返回 SQL 脚本列表。ADR-0002 的「MP 3.5.3+ 自动维护 DDL」前提成立。
  来源: <https://baomidou.com/guides/auto-ddl/>
- **Spring Boot 3 的 artifact**:官方安装文档明确 Spring Boot 3 使用 `com.baomidou:mybatis-plus-spring-boot3-starter`(该 artifact 自 3.5.4.1 起存在于 Central;更早的 3.5.3.x 只有 `mybatis-plus-boot-starter` 面向 Boot 2)。当前 3.5.x 最新稳定版 **3.5.17**(2026-07-08 同步至 Central)。
  来源: <https://baomidou.com/getting-started/install/> ;<https://repo1.maven.org/maven2/com/baomidou/mybatis-plus-spring-boot3-starter/maven-metadata.xml>
- **3.5.9+ 的 jsqlparser 拆分(重要)**:官方原文"版本 3.5.9+ 插件部分开始修改为可选依赖",分页插件(`PaginationInnerInterceptor`)等需额外引入 `mybatis-plus-jsqlparser`(跟随 jsqlparser 最新版,JDK 11+)或 `mybatis-plus-jsqlparser-4.9`(JDK 8+)。目标应用 JDK 17+,应配 `mybatis-plus-jsqlparser` 3.5.17。
  来源: <https://baomidou.com/getting-started/install/>
- **版本线选择建议**:锁 3.5.x 主线最新版 **3.5.17**,不要停留在 3.5.3.x(无 boot3 starter,且后续有大量修复)。与 Spring Boot 3.5.16 组合无官方不兼容记录,starter 面向整个 Boot 3 线。

## 3. jjwt → 0.13.0

- **事实**:`io.jsonwebtoken:jjwt-api` Central 最新稳定版 **0.13.0**(2025-08-20 发布,GitHub Releases 最新同为 0.13.0)。`jjwt-impl` / `jjwt-jackson` 同版本号同步发布。
- 0.12+ 起 API 稳定(`Jwts.builder()` 新流式 API),最低 Java 8,JDK 21 无兼容性问题。
- 来源: <https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-api/maven-metadata.xml> ;<https://github.com/jwtk/jjwt/releases/latest>
- **定版**: 0.13.0 三件套(`jjwt-api` compile + `jjwt-impl`/`jjwt-jackson` runtime)。

## 4. Vue / Vite / Element Plus / axios

以下均取 npm dist-tag `latest`(2026-08-13 实测):

| 包 | latest | 关键约束(registry 原文) |
|---|---|---|
| vue | **3.5.41** | peerDependencies: `typescript: *` |
| vite | **8.2.1** | engines: `node: ^20.19.0 \|\| >=22.12.0` |
| element-plus | **2.14.4** | peerDependencies: `vue: ^3.3.7` |
| axios | **1.19.0** | 无特殊 engines/peer 约束 |

- **Element Plus × Vue**:peer 要求 `vue ^3.3.7`,Vue 3.5.41 满足。
- **Vite × Node**:Vite 8 要求 Node `^20.19.0 || >=22.12.0`(即 Node 20.19+ 或 22.12+,Node 18/21 已出局)。这约束的是**构建机/CI 与开发者本机**的 Node 版本,不约束生成器用户是否装 Node(生成器本体零 Node 依赖,ADR-0001 不变;但目标应用前端构建需 Node 20.19+/22.12+,CI 质量门禁的 `npm run build` 环节须按此配 Node)。
- 配套:`@vitejs/plugin-vue` 最新 6.0.8,peer `vite ^5||^6||^7||^8`、`vue ^3.2.25`,与上述组合兼容;`vue-tsc` 最新 3.3.9,peer `typescript >=5.0.0`。
- 来源: <https://registry.npmjs.org/vue/latest> · <https://registry.npmjs.org/vite/latest> · <https://registry.npmjs.org/element-plus/latest> · <https://registry.npmjs.org/axios/latest> · <https://registry.npmjs.org/@vitejs/plugin-vue/latest> · <https://registry.npmjs.org/vue-tsc/latest>

## 5. picocli → 4.7.7

- **事实**:Central 与 GitHub Releases 最新一致,**4.7.7**(2025-04-19)。picocli 运行时单 jar、零依赖,最低 Java 8,JDK 21 无问题。
- 来源: <https://repo1.maven.org/maven2/info/picocli/picocli/maven-metadata.xml> ;<https://github.com/remkop/picocli/releases/latest>

## 6. JLine 4.x → 4.3.1(仅 jline-terminal 原语,遵 ADR-0001)

- **最新版本**:JLine 4.x 线最新 **4.3.1**(2026-07-21 同步 Central;`jline-terminal` / `jline-terminal-jni` / `jline-terminal-ffm` 同号发布)。注:GitHub Releases 最新 tag 为 3.x 维护线的 `jline-3.30.16`(3.x 仍在维护),但 ADR-0001 已定 4.x。
- **3 vs 4 差异(官方文档原文)**:"JLine 4.x requires **Java 11** as the minimum runtime version"(3.x 为 Java 8+);"The **JNA** provider has been **removed** in JLine 4.x"、"The **Jansi** provider has been **removed** in JLine 4.x";"The recommended providers for JLine 4.x are **JNI**(for maximum compatibility)and **FFM**(for best performance on Java 22+)."
  来源: <https://github.com/jline/jline3/blob/master/README.md> ;<https://jline.org/versions/4.0/docs/modules/terminal-providers>
- **JDK 21 上的 provider(本调研对 4.3.1 jar 实测核实)**:
  - `jline-terminal-4.3.1.jar` 主 jar 字节码为 class version 55(Java 11),内置仅 **Exec + Dumb** 两个 provider(见 `META-INF/services/org.jline.terminal.spi.TerminalProvider`);
  - **JNI provider 在独立 artifact `jline-terminal-jni`**(Java 11 字节码,覆盖 Linux/macOS/FreeBSD/Windows 原生 PTY)——JDK 11–21 获得完整原生终端能力必须显式引入它;
  - **FFM provider 在独立 artifact `jline-terminal-ffm`**,字节码 class version 66(**Java 22**),JDK 21 无法加载,不引入;
  - 官方对 Java 11–21 用户的指引是 `jline` 聚合 jar 使用 `jdk11` classifier(排除 FFM 类);但 ADR-0001 只用 `jline-terminal` 原语,等价做法是 `jline-terminal` + `jline-terminal-jni` 两个 artifact。
- **与 ADR-0001 的印证**:ADR-0001 所述「JLine 4 在 JDK 21 仅有 JNI 终端 provider,冷门架构退化为 dumb terminal 时须强制走全参数模式」与实测一致——JNI 原生库仅覆盖主流架构,LoongArch 等信创冷门架构上 JNI 加载失败会退化到 Exec/Dumb,全参数模式兜底逻辑必须保留。
- **定版**: `org.jline:jline-terminal:4.3.1` + `org.jline:jline-terminal-jni:4.3.1`。

## 7. Freemarker → 2.3.34;TypeScript → ~5.9.3

- **Freemarker**:`org.freemarker:freemarker` 最新稳定版 **2.3.34**(2024-12-22,此后无新版;项目已转入低活跃维护,但 ADR-0001 已定 MVP 用它、native 阶段再评估 Rocker,定版 2.3.34 即可)。
  来源: <https://repo1.maven.org/maven2/org/freemarker/freemarker/maven-metadata.xml>
- **TypeScript**:npm `latest` dist-tag 已指向 **7.0.2**(2026 年发布的新架构版本,Go 原生实现);5.x 线最新为 **5.9.3**。**建议定版 `~5.9.3`** 而非 7.x:TS 7 是全新实现,Vue 工具链(`vue-tsc` 3.3.9 基于 Volar/TS 编译器 API,peer 声明 `typescript >=5.0.0`)对 7.x 的适配未经充分验证,目标应用模板求稳;TS 7 的观察与升级记入后续迭代。此为工程判断项,事实项是:latest=7.0.2、5.x 最新=5.9.3、vue-tsc peer 要求 ≥5.0.0。
  来源: <https://registry.npmjs.org/typescript> ;<https://registry.npmjs.org/vue-tsc/latest>

---

## 8. 兼容性注意点汇总

1. **JDK 21 基线**:Spring Boot 3.5.16 官方兼容 Java 17–25,JDK 21 在内;picocli 4.7.7 / Freemarker 2.3.34 / jjwt 0.13.0 / MyBatis-Plus 3.5.17 均远低于此基线,无冲突。JLine 4.3.1 主 jar 与 jni artifact 均为 Java 11 字节码,JDK 21 直接可用。
2. **MyBatis-Plus × Spring Boot 3**:必须用 `mybatis-plus-spring-boot3-starter`(Boot 2 的 `mybatis-plus-boot-starter` 不可混用);3.5.9+ 起分页等插件依赖 jsqlparser 拆分为可选依赖,JDK 17+ 目标应用须同时引入 `mybatis-plus-jsqlparser`;自动维护 DDL(IDdl / DdlApplicationRunner)自 3.5.3 引入,3.5.17 远超门槛;注意 ADR-0003 已记录的三重约束(去重粒度为文件路径、默认吞异常仅打日志、`ddl_history` 方言仅内置 MySQL/PG/Oracle,信创库需自定义 `IDdlGenerator`)不变。
3. **Element Plus × Vue/Vite**:Element Plus 2.14.4 peer `vue ^3.3.7`,Vue 3.5.41 满足;Vite 8.2.1 要求 Node `^20.19.0 || >=22.12.0`,生成器 CI 质量门禁(生成 → `npm run build`)与用户文档中的 Node 版本要求须按此写明;`@vitejs/plugin-vue` 6.0.8 兼容 Vite 8。
4. **JLine 4 × JDK 21 终端 provider**:FFM provider 需 Java 22 字节码,JDK 21 不可用且不应引入 `jline-terminal-ffm`;JDK 21 的完整原生终端能力 = `jline-terminal` + `jline-terminal-jni`;不加 jni artifact 时退化为 Exec(fork `stty` 等外部命令)或 Dumb;信创冷门架构(LoongArch 等)JNI 原生库缺失时退化 Dumb,ADR-0001 的「非 TTY/dumb 强制全参数模式」兜底必须落实。
5. **TypeScript 版本线**:锁 `~5.9.3`(5.x 线),不追 latest 的 7.0.2,待 Vue/Volar 工具链对 TS 7 适配明朗后再评估。
