# Java 自研 CLI 生成器技术选型调研(issue #4)

> 调研日期:2026-08-12。为 jfast"面向信创的 JHipster 式脚手架生成器"的 Java 自研 CLI 方案铺路。
> 来源以 picocli/JLine/GraalVM/各模板引擎/JBang/Quarkus/Spring 官方文档与源码仓库为准,逐条标注 URL。

## TL;DR 结论表

| 问题 | 结论 | 置信度 |
|---|---|---|
| (a) picocli + JLine3 交互问答 | **可行且组件基本现成**:单选/多选/确认/输入/可展开选择 + 依赖前序答案的动态多步 prompt 都有;需自研的是"向导编排层"(问题模型、条件分支、校验),而非终端组件。注意 `jline-console-ui` 已废弃,官方迁移到新的 `jline-prompt` 模块 | 高 |
| (b) 模板引擎 × GraalVM native | 推荐排序:**Rocker > Freemarker > Pebble**。Rocker 编译期生成 Java 源码、零反射,天然适配 native;Freemarker 有社区 reachability 元数据但官方支持仍开放;Pebble 轻量但元数据需完全自理 | 高 |
| (c) MVP 分发渠道 | **首选 jbang**(fat jar + jbang-catalog,Quarkus CLI 同款先例);同步发 Maven Central;native 可执行文件作为第二阶段增强(CI 矩阵成本高,Quarkus CLI 本身也不发 native);Maven 插件可作为面向存量 Java 团队的补充渠道 | 高 |
| (d) 案例借鉴 | Initializr:生成核心做成与交付方式解耦的独立库 + 元数据驱动;Quarkus CLI:picocli mixin 组织参数、jbang alias 插件机制、JReleaser 多渠道发布;JBang:catalog/alias 机制可复用为"模板目录" | 高 |

---

## (a) picocli + JLine3 交互式问答能力现状

### 结论

做"脚手架生成向导"所需的终端 prompt 组件(输入、单选、多选、确认、可展开选择、动态多步)在 JLine 侧**基本现成**;picocli 只管参数解析与单值交互输入。需要自研的是上层的**向导编排层**(问题模型、条件分支、默认值/校验、答案到模板模型的映射),估计占向导功能工作量的主体,终端渲染层几乎不用写。

### 关键事实

**picocli 的交互式输入(能力边界清晰,只管单值):**
- `@Option(interactive = true)` 支持交互式输入,默认用 `Console.readPassword()` 不回显;`echo`/`prompt` 属性可控制回显与提示语;密码建议用 `char[]` 以便用后清零。
- 配合 `arity = "0..1"` 可实现"命令行给了值就用,没给就提示"的可选交互。
- 限制:交互式 positional 参数后必须跟非交互 positional(最后一个位置参数不能是交互式);与 JLine 2 的 `ConsoleReader` 不兼容,需自定义 `IParameterConsumer` 或用 picocli-shell-jline3。
- **picocli 不提供任何表单/向导/菜单组件**,官方定位就是参数解析,复杂交互交给 JLine 之类。
- 来源:picocli 官方手册 <https://picocli.info/#_interactive>;deepwiki 对 remkop/picocli 的源码问答 <https://deepwiki.com/search/what-interactive-input-support_349b1797-4790-4a96-8dea-9cf14398cf26>

**picocli-shell-jline3(REPL/子命令补全,非向导):**
- 提供 `PicocliCommands`(把 picocli 命令树桥接进 JLine3 的 `SystemCompleter`,支持 TAB 补全、命令描述、alias、`TailTipWidgets` 状态栏)和 `PicocliJLineCompleter`(实现 JLine `Completer` 接口,兼容旧版 JLine)。
- 要求 JLine ≥ 3.14.1(`TailTipWidgets` API 变更)。它是为"交互式 shell"场景设计,不是表单组件库。
- 来源:<https://github.com/remkop/picocli/tree/master/picocli-shell-jline3>;deepwiki 同上。

**JLine 的 prompt 组件(重点,有重要的版本新旧变化):**
- `jline-console-ui`(artifact `org.jline:jline-console-ui`)提供:`InputValuePrompt`(文本输入,支持补全与掩码)、`ListChoicePrompt`(单选列表)、`CheckboxPrompt`(多选)、`ConfirmPrompt`(是/否)、可展开选择(key-based choice)。入口是 `ConsolePrompt` + `PromptBuilder`(`createInputPrompt()`/`createListPrompt()`/`createCheckboxPrompt()`/`createConfirmPromp()`/`createChoicePrompt()`)。**`ConsolePrompt` 支持动态多步 prompt——后续问题可依赖前面答案,这就是"向导"的最小骨架**。来源:deepwiki 对 jline/jline3 的问答 <https://deepwiki.com/search/what-components-does-jlinecons_1843c8a1-2e22-4be6-9692-751d1b7d0f0e>
- **重要:`jline-console-ui` 模块已被官方标记 deprecated**,README 明确指示迁移到新的 **`jline-prompt`** 模块(`org.jline:jline-prompt`,包名 `org.jline.prompt`),新 API 为接口式:`ConsoleUIFactory`/`PrompterFactory` 创建 prompter,`PromptBuilder` 构建,结果返回 `Map<String, PromptResult>`;演示类 `org.jline.prompt.examples.NewApiExample`。Maven Central 上 `jline-prompt` 已随 JLine 4.x 发布(查到 4.3.1)。来源:<https://github.com/jline/jline3/tree/master/console-ui>;<https://central.sonatype.com/artifact/org.jline/jline-prompt/versions>
- `jline-console`(`org.jline.builtins`)是命令注册/widget 框架,面向 REPL 基础设施,与表单无关。
- JLine 旧 wiki 已停止更新,内容迁往 <https://jline.org>。

### 对 jfast 的含义

- 终端组件零自研:输入/单选/多选/确认/可展开选择直接覆盖"脚手架向导"的题型;`ConsolePrompt`/`Prompter` 的动态多步能力覆盖条件分支。
- 自研重点在**问题模型与编排**:声明式的问卷定义(问题、类型、默认值、校验、显示条件)、答案到模板模型的转换、与 picocli 命令参数的双向映射(参数给了就跳过对应问题——参考 picocli `arity="0..1"` + interactive 的模式)。
- **版本决策点**:直接基于 `jline-prompt`(JLine 4.x)开发,避开 deprecated 的 console-ui;需验证 picocli-shell-jline3 与 JLine 4 的兼容性(其文档以 JLine 3.x 为准)。

---

## (b) 模板引擎候选 × GraalVM native image

### 结论

推荐排序:**Rocker > Freemarker > Pebble**。Rocker 编译期把模板生成 Java 源码、运行期零反射零引擎,是 native image 的天然适配者;Freemarker 生态最成熟且有社区 reachability 元数据兜底,但官方 native 支持仍是开放 issue,模型类反射注册是主要工作量;Pebble 引擎本身轻,但属性解析全靠反射且官方元数据仓库无收录,DIY 成本最高。

### 关键事实

**GraalVM native image 的通用限制(官方文档):**
- **封闭世界假设**:构建期静态分析决定哪些类/方法/资源进二进制,运行期不能再加载新字节码;反射(`Class.forName`、`Method.invoke` 等)若无元数据会抛 `MissingReflectionRegistrationError`。
- 元数据提供方式:`META-INF/native-image/<groupId>/<artifactId>/reachability-metadata.json`(JDK 23+ 合并了旧的 `reflect-config.json` 等)、构建期初始化、`-H:Preserve`、Feature API。
- **Tracing Agent**(JVM 模式跑一遍收集动态访问)+ 社区 **GraalVM Reachability Metadata** 仓库(native-build-tools Maven/Gradle 插件自动拉取)是两条现实路径。
- 动态类加载运行期不可行(构建期生成类或实验性 predefined-classes 是绕行方案,后者"不保证可用");资源必须显式注册(常量 `getResource` 或 JSON glob)。
- 来源:<https://www.graalvm.org/latest/reference-manual/native-image/metadata/>;<https://github.com/oracle/graalvm-reachability-metadata>

**Rocker(fizzed/rocker)——编译型:**
- Maven/Gradle 插件在 `generate-sources` 阶段把模板**解析生成普通 Java 源码**,随项目一起编译;模板即 POJO,参数有类型、IDE 可补全、编译期检查。
- 官方明确"**No reflection used**"、无运行期引擎("每个模板自己知道怎么渲染")。
- 与 GraalVM 的唯一注意点:默认的纯文本策略 `STATIC_BYTE_ARRAYS_VIA_UNLOADED_CLASS` 依赖类加载技巧,官方建议 native 场景改用 `STATIC_BYTE_ARRAYS` 策略;热重载是 JVM-only 特性(native 下不需要)。
- 运行时兼容 Java 8+;模板不占用 resource 注册(已编译成 class)。
- 来源:<https://github.com/fizzed/rocker>(README,含 "GraalVM compatability if you leverage the new PlainText strategy of STATIC_BYTE_ARRAYS")。

**Freemarker(apache/freemarker)——解释型,反射密集:**
- 运行期解释模板;对模型对象的属性访问走 BeansWrapper 内省,**重度依赖反射**——意味着你传给模板的所有模型类都要注册反射元数据(与引擎本身无关,是用户代码的工作量)。
- **官方 native 支持仍未落地**:FREEMARKER-229("Add GraalVM native support")处于开放状态,issue 中只引用了社区方案。
- 社区兜底:GraalVM 官方 reachability 元数据仓库已有 `org.freemarker/freemarker`(收录至 2.3.31 目录,index 映射更新版本);社区项目 `fugerit-org/freemarker-native` 提供 substitutions,并记录了坑(如 `freemarker.ext.jython.JythonWrapper` 需 `--initialize-at-run-time`、2.3.34 的 MR-JAR `_Java9Impl`/`_Java16Impl` 告警)。
- 模板以 classpath 资源加载时,native 下需注册 resource pattern。
- 来源:<https://issues.apache.org/jira/browse/FREEMARKER-229>;<https://github.com/fugerit-org/freemarker-native>;<https://github.com/oracle/graalvm-reachability-metadata/tree/master/metadata/org.freemarker/freemarker>;<https://stackoverflow.com/questions/78822980/unnecessary-warnings-for-freemarker-classes-on-native-image-execution>

**Pebble(PebbleTemplates/pebble)——解释型,反射密集,无元数据收录:**
- 模板运行期解析为 AST 解释执行(`LexerImpl`/`ParserImpl` → `PebbleTemplateImpl`);属性访问由 `DefaultAttributeResolver` 按 Map → 数组 → List → **反射**(getter/is/has/方法/public 字段,带 `MemberCacheUtils` 缓存)解析。
- GraalVM reachability 元数据仓库**无 `io.pebbletemplates` 条目**(目录 404),需自行用 Tracing Agent 跑模板渲染路径收集,并为所有模型类注册反射。
- 有第三方项目(SoftInstigate/facet)在 native 下跑通了 Pebble,证明可行但要自己维护元数据。
- 来源:deepwiki 对 PebbleTemplates/pebble 的源码问答 <https://deepwiki.com/search/how-does-pebble-resolve-object_f8500b3d-1d6b-430a-a7e7-dab69b596317>;<https://github.com/oracle/graalvm-reachability-metadata/tree/master/metadata>(无 pebble 条目);<https://github.com/SoftInstigate/facet>

### 对 jfast 的含义

- 若坚定走 native:**Rocker 是默认答案**——编译期检查还能顺带提升模板质量(脚手架模板写错在 CI 就暴露);代价是模板语法对非 Java 贡献者略陌生,且模板变更需重新构建 CLI(对"模板内嵌于 CLI"的分发模式这反而一致)。
- 若看重模板生态/贡献者熟悉度(JHipster 用户习惯):Freemarker 可行,JVM 模式零障碍,native 模式靠社区元数据 + 自有模型类的反射注册 + 模板资源注册,需做一轮 native 冒烟验证。
- Pebble 不建议在本项目引入:反射面相同但元数据全靠自维护,生态收益不足以抵偿。
- 架构建议:无论选谁,把"模板渲染"收敛到一个接口后,方便 JVM 期用 Freemarker 起步、native 期换 Rocker 的双轨策略。

---

## (c) 分发渠道现状

### 结论

MVP 推荐 **jbang 渠道**(fat jar 发布 Maven Central + jbang-catalog 别名),这是 Quarkus CLI 验证过的路径,对 Java 用户几乎零摩擦且天然跨平台;native 可执行文件列为第二阶段(成本主要在跨平台 CI 矩阵与模板引擎适配,见 (b));Maven 插件作为面向存量 Maven 团队的补充渠道可选做。

### 关键事实

**jbang:**
- 运行方式:`jbang <脚本|别名|jar>`;单文件脚本用 `//DEPS`(Gradle 坐标式依赖,从 Maven Central 解析)、`//SOURCES`(多文件)声明;catalog 机制(`jbang-catalog.json`,支持 `alias@user/org` 隐式引用 GitHub/GitLab/Bitbucket/HTTP 上的目录)。
- 安装方式全:curl|bash、SDKMAN、Homebrew、Chocolatey、Scoop、Docker。
- picocli 支持:`//DEPS info.picocli:picocli`;native 兼容需再加 `info.picocli:picocli-codegen`。
- `jbang export native`:调 GraalVM `native-image` 产出独立可执行文件;**前提是本机装有 GraalVM + native-image;产物平台相关**(Linux 构建只能在 Linux 跑);反射/资源问题用 `//NATIVE_OPTIONS` 传参解决。
- 来源:deepwiki 对 jbangdev/jbang 的问答 <https://deepwiki.com/search/describe-jbangs-architecture-t_777690a5-8975-4a4d-bd86-a16deada2472>;<https://www.jbang.dev>

**Maven 插件形式:**
- `maven-archetype-plugin:generate` 是标准交互式骨架生成入口,但 archetype 模板基于 Velocity、能力弱、交互问答简陋,做"JHipster 式"复杂生成会顶到天花板。
- 自定义 mojo(自定义 Maven 插件)是成熟惯例,可把生成器包成 `jfast:generate`;但前提是用户环境有 Maven,受众收敛为 Java 后端团队——对信创存量企业 Java 团队这其实不是坏事。
- 来源:<https://maven.apache.org/archetype/maven-archetype-plugin/generate-mojo.html>;<https://maven.apache.org/plugin-tools/maven-plugin-plugin/>

**native 可执行文件分发:**
- GraalVM 官方 native-build-tools(Maven/Gradle 插件)成熟,自动拉取社区 reachability 元数据。
- 跨平台现实成本:native 产物平台相关,必须 CI 矩阵(ubuntu/macos/windows × amd64/aarch64),社区有现成的 `graalvm/setup-graalvm` GitHub Action 与矩阵工作流范例(Okta、BellSoft 均发布过完整示例,产物经 GitHub Releases 分发)。
- 先例:GraalVM 官方 GDK/Micronaut CLI 走 Homebrew + SDKMAN + GitHub Releases 分发 native 二进制;**反例:Quarkus CLI 明确以 `JAVA_BINARY`(jar)形式经 jbang/SDKMAN/Homebrew/Chocolatey/Scoop 分发,并不发 native**——说明"生成器 CLI 必须是 native"并非行业共识。
- 来源:<https://github.com/graalvm/setup-graalvm>;<https://developer.okta.com/blog/2022/04/22/github-actions-graalvm>;<https://bell-sw.com/blog/how-to-build-and-release-graalvm-native-images-using-github-actions/>;<https://github.com/oracle/gcn>;deepwiki 对 quarkusio/quarkus 的问答 <https://deepwiki.com/search/describe-the-quarkus-cli-locat_f5cb5dc5-2351-4ce8-980b-82f61495588f>

### 对 jfast 的含义

- 信创环境前提是目标机器有 JDK(做 Java 脚手架生成器,用户本来就要 JDK),因此 jar + jbang 的"非 native"形态在目标人群里不是短板。
- MVP:发布 fat jar 到 Maven Central + 提供 `jbang-catalog.json`(用户 `jbang jfast@hhhrrr777` 即用)+ 一键安装脚本;这同时解决了版本升级(jbang 自动缓存新版本)。
- 第二阶段:若需要"无 JDK 环境可用"或极致启动速度,再上 native;此时 (b) 的模板引擎选型结论(Rocker)直接兑现收益,CI 用 `graalvm/setup-graalvm` 矩阵。
- 可选:面向企业 Maven 用户补一个 `jfast-maven-plugin`(复用同一生成核心),成本低、合规场景(内网 Nexus)友好。

---

## (d) 同类工程案例架构要点

### Spring Initializr(spring-io/initializr)

**事实:**
- 模块结构:`initializr-generator`(核心生成库:`ProjectGenerator`/`ProjectDescription`,构建系统抽象 `Build` → `MavenBuild`/`GradleBuild`,`MavenBuildWriter`/`GradleBuildWriter` 落盘)、`initializr-generator-spring`(Spring 约定的 contributors/customizers,含 `BuildCustomizer`)、`initializr-web`(REST 端点)、`initializr-metadata`(可配置元数据 `InitializrMetadata`)、`initializr-actuator`(统计)、`initializr-generator-test`(测试基建)、`initializr-version-resolver`、`initializr-bom`;`start-site` 是 start.spring.io 的配套实例配置而非核心库。
- 模板用 **Mustache**,`MustacheTemplateRenderer` 渲染(模板名 + model map → 字符串,可配缓存)。
- **HTTP/web-first**:Web UI、REST API(`/starter.zip` 等)、对 curl/HTTPie 的 content-negotiation 特判(`CommandLineContentNegotiationStrategy`)、IDE 插件,全部是同一 HTTP 服务的客户端。没有 CLI-first,因为生成逻辑服务化后元数据/版本集中更新、可缓存、可统计(actuator),客户端无需升级即获得最新模板。
- 来源:deepwiki 对 spring-io/initializr 的问答 <https://deepwiki.com/search/describe-the-module-structure_f65cf44d-9ce9-46cc-9291-7fea10b64ff5>;<https://github.com/spring-io/initializr>

**可借鉴:**
1. **生成核心与交付方式解耦**:把 generator 做成独立库(ProjectDescription 驱动),CLI/Web/Maven 插件都是它的薄壳——jfast 多渠道分发的前提。
2. **构建描述建模**:`Build` 抽象 + Writer + `BuildCustomizer` 链,比纯模板拼接 pom.xml 更可控,信创场景定制(替换源、内置合规依赖)有了挂点。
3. **元数据驱动**:可选项(依赖、版本)放进 metadata 而非代码,便于企业私有化改配。
4. 测试基建单独成模块(generator-test),生成器项目回归成本的大头在测试。

### Quarkus CLI(quarkusio/quarkus, devtools/cli)

**事实:**
- picocli 实现,入口 `QuarkusCli` 注册 `Create`/`Build`/`Dev`/`ProjectExtensions` 等子命令;交互式创建由 `io.quarkus.cli.create.BaseCreateCommand` 的子类 + 一组 mixin(`DataOptions`、`TargetBuildToolGroup`、`TargetLanguageGroup` 等)收集参数;确认类交互用 `Prompt.yesOrNo` 工具。
- 分发:**JAVA_BINARY(jar)** 经 jbang、SDKMAN、Homebrew、Chocolatey、Scoop 发布,用 **JReleaser** 编排多渠道。
- 插件机制:`PluginManager` 管理插件;插件可以是可执行文件、jar、**jbang alias** 或 Maven 坐标;插件目录存于 `~/.quarkus/cli/plugins/quarkus-cli-catalog.json`(及项目级);与扩展注册中心 `registry.quarkus.io` 联动(扩展元数据可声明 `cli-plugins`,CLI 发现未安装命令对应插件时提示安装)。
- 来源:deepwiki 对 quarkusio/quarkus 的问答 <https://deepwiki.com/search/describe-the-quarkus-cli-locat_f5cb5dc5-2351-4ce8-980b-82f61495588f>

**可借鉴:**
1. **mixin 组织参数组**:生成器选项多,按"构建工具/语言/平台/代码生成"分组复用,jfast 可直接照搬。
2. **jbang alias 插件机制**:生态扩展几乎零基建——第三方模板/子命令就是一个 jbang 脚本;信创生态共建时门槛低。
3. **项目级 + 用户级双层插件 catalog**:企业内可沉淀私有插件集。
4. **JReleaser 多渠道发布**:一套配置同时覆盖包管理器,免去手写发布流水线。

### JBang(jbangdev/jbang)

**事实:**
- 单文件脚本模型:`//DEPS` 声明依赖、`//SOURCES` 引入多文件,首次运行编译并缓存产物。
- catalog:`jbang-catalog.json` 支持当前目录/父目录/`~/.jbang/` 多级;隐式别名 `alias@user/org` 直接从 GitHub 等解析。
- `jbang export native` 基于 GraalVM,产物平台相关,`//NATIVE_OPTIONS` 透传参数。
- 自身分发:curl|bash、SDKMAN、Homebrew、Chocolatey、Scoop、Docker。
- 来源:deepwiki 对 jbangdev/jbang 的问答 <https://deepwiki.com/search/describe-jbangs-architecture-t_777690a5-8975-4a4d-bd86-a16deada2472>

**可借鉴:**
1. **catalog/alias 即"模板目录"**:jfast 的模板集可以复用同一思想——官方 catalog + 企业私有 catalog + `alias@org` 寻址。
2. 单文件低心智负担:原型期 jfast 本身就可以是一个 jbang 脚本起步,验证后再拆工程。
3. 多级配置查找(当前目录→父目录→用户目录)的惯例适合"项目级模板覆盖全局模板"。

---

## 风险与开放问题

1. **JLine 4 vs picocli-shell-jline3 兼容性未验证**:`jline-console-ui` 已废弃,新模块 `jline-prompt` 属 JLine 4.x;picocli-shell-jline3 文档以 JLine 3.x(≥3.14.1)为准,混用需 POC。若只做向导式(非 REPL)可不依赖 picocli-shell-jline3,风险可控。
2. **native 路线的真实成本集中在模板引擎与 CI**:模板引擎结论见 (b);CI 矩阵(linux/macos/windows × arm/x86,信创还涉及麒麟/统信 ARM 环境)构建与验证成本需在原型票据中单独估算。
3. **Freemarker native 支持非官方**:FREEMARKER-229 仍开放,社区元数据版本滞后(收录 2.3.31);选 Freemarker 就要承担自建 native 验证的责任。
4. **纯本地 CLI 还是"本地 CLI + 可选服务端"**:Initializr 证明了服务化在元数据集中更新、统计上的价值;信创内网环境又偏向纯离线本地。建议 MVP 纯本地 + catalog 可指向内网 HTTP,留服务端演化空间。
5. **模板/插件注册中心机制未定**:Quarkus 的 registry + cli-plugins、jbang 的 catalog 是两种参照,jfast 的"信创模板生态"采哪种(或先 catalog 后 registry)需专门票据。
6. **Quarkus CLI 不发 native 的事实**提示:native 是否值得做,应先用用户环境数据(目标机器 JDK 普及率)验证,而非默认上 native。

## 参考链接清单

**picocli / JLine:**
- <https://picocli.info/#_interactive>
- <https://github.com/remkop/picocli/tree/master/picocli-shell-jline3>
- <https://deepwiki.com/search/what-interactive-input-support_349b1797-4790-4a96-8dea-9cf14398cf26>
- <https://deepwiki.com/search/what-components-does-jlinecons_1843c8a1-2e22-4be6-9692-751d1b7d0f0e>
- <https://github.com/jline/jline3/tree/master/console-ui>(含 jline-prompt 迁移说明)
- <https://central.sonatype.com/artifact/org.jline/jline-prompt/versions>
- <https://jline.org>

**GraalVM / 模板引擎:**
- <https://www.graalvm.org/latest/reference-manual/native-image/metadata/>
- <https://github.com/oracle/graalvm-reachability-metadata>
- <https://github.com/oracle/graalvm-reachability-metadata/tree/master/metadata/org.freemarker/freemarker>
- <https://github.com/fizzed/rocker>
- <https://issues.apache.org/jira/browse/FREEMARKER-229>
- <https://github.com/fugerit-org/freemarker-native>
- <https://stackoverflow.com/questions/78822980/unnecessary-warnings-for-freemarker-classes-on-native-image-execution>
- <https://deepwiki.com/search/how-does-pebble-resolve-object_f8500b3d-1d6b-430a-a7e7-dab69b596317>
- <https://github.com/SoftInstigate/facet>

**分发:**
- <https://www.jbang.dev>
- <https://deepwiki.com/search/describe-jbangs-architecture-t_777690a5-8975-4a4d-bd86-a16deada2472>
- <https://maven.apache.org/archetype/maven-archetype-plugin/generate-mojo.html>
- <https://maven.apache.org/plugin-tools/maven-plugin-plugin/>
- <https://github.com/graalvm/setup-graalvm>
- <https://developer.okta.com/blog/2022/04/22/github-actions-graalvm>
- <https://bell-sw.com/blog/how-to-build-and-release-graalvm-native-images-using-github-actions/>
- <https://github.com/oracle/gcn>

**案例:**
- <https://github.com/spring-io/initializr>
- <https://deepwiki.com/search/describe-the-module-structure_f65cf44d-9ce9-46cc-9291-7fea10b64ff5>
- <https://deepwiki.com/search/describe-the-quarkus-cli-locat_f5cb5dc5-2351-4ce8-980b-82f61495588f>
