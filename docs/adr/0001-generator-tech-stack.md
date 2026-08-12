# 生成器自身技术栈:Java 自研 CLI

jfast 生成器本体采用 **Java 自研 CLI**(picocli 参数解析 + 基于 jline-terminal 裸原语的自研交互层 + Freemarker 模板引擎),MVP 经 **jbang(首选)+ fat jar(兜底)** 分发,运行基线 **JDK 21**。核心论证按权重:① 目标用户(国内 Java 后端开发者,含信创环境)零 Node 依赖;② 贡献者与用户同构;③ 保留 GraalVM native image 上限;④ 原型已实测自研交互层成本可控(约 300 行,手感经三轮用户实测通过)。

## Considered Options

- **Node + Yeoman 系(JHipster 同款)**:生态最成熟(交互组件、mem-fs 冲突合并、`.yo-rc.json` 状态追踪现成),但要求用户先装 Node——信创环境中 Node 工具链是额外合规负担,且与贡献者社区(Java)不同构。拒绝。
- **Node 自研 CLI**:既要用户装 Node 又放弃 Yeoman 生态,两头不占。拒绝。

## Consequences

- **模板组织为「目录即模板 + 预设目录分层」**:模板树即产物树,预设 = base/full 目录叠加;条件文件的元数据约定留实现阶段。
- **模板引擎双轨**:MVP 用 Freemarker(运行时资源,与目录即模板的 jar 内 walk 契合;GraalVM 支持至今是开放 issue FREEMARKER-229);渲染收敛到单一接口作接缝,**若且唯若** native 阶段启动才换 Rocker(编译期生成 Java 源码、零反射)。换引擎意味着全部模板按语法重写,故 native 启动前须先以目标环境 JDK 普及率验证必要性(Quarkus CLI 只发 JAVA_BINARY 的先例表明 native 并非必需)。
- **分发渠道**:MVP 仅 jbang(fat jar 发 Maven Central + `jbang-catalog.json` 别名)与 fat jar(GitHub Releases);Maven 插件渠道(面向信创企业 Maven 团队)进路线图,不在 MVP;native 可执行文件列第二阶段。
- **信创姿态**:生成器本体与目标应用同款——国产 JDK/OS「声明兼容 + 文档」,不做实测认证。JLine 4 在 JDK 21 仅有 JNI 终端 provider,冷门架构(LoongArch 等)退化为 dumb terminal 时须强制走全参数模式。
- 交互组件层全自研:JLine 4.x 高层组件不可用(jline-prompt 4.3.1 列表渲染 bug、LineReader 与常驻 UI 不兼容),仅依赖 jline-terminal 原语。

依据:调研 `docs/research/java-cli-toolchain.md`、原型分支 `prototype/java-cli-minimal`、拍板票据 <https://github.com/hhhrrr777/jfast/issues/6>。
