# 生成器仓库骨架与构建落地:Maven 单模块 + io.github 坐标 + 同仓 baseline

jfast 生成器本体的仓库骨架拍板:**Maven 单模块**(artifactId `jfast`,初始版本 `0.1.0-SNAPSHOT`,语义化版本);**groupId `io.github.hhhrrr777`,根包 `io.github.hhhrrr777.jfast`**;模板树位于 `src/main/resources/templates/{base,presets/<name>}/`(沿用原型布局,与 ADR-0004 结构一致);**`baseline/` 放同仓顶层**,是独立的 Spring Boot + Vue 工程(自带 pom/package.json,不进生成器 reactor),定位为「模板底稿,手工构建验证」;CI 为**单 workflow `ci.yml` 两 job**:生成器本体 `mvn verify`(JDK 21 单版本)+ 预设门禁矩阵(empty/full × 生成 → 后端 `mvn compile` → 前端 `npm run build`,Node LTS),触发于 push 主干与 PR;分支模型 **main 唯一主干**,每施工单元一 `feat/<单元>` 分支,PR **squash merge**,`dev` 废弃(归并 main 后删除),`research/*`、`adr/*`、`prototype/*` 前缀惯例保留,main 开分支保护(要求 PR + CI 绿 + 禁 force push)。核心论证按权重:① 原型分支已实测 Maven + shade fat jar 链路跑通,ADR-0001 的 jbang/Maven Central 分发链在 Maven 下全是现成插件;② 用户与贡献者同构(国内 Java 后端团队,含信创环境),Maven 普及率压倒性高;③ Maven Central 命名空间是硬约束——`io.jfast` 需持有 `jfast.io` 域名(经查无 DNS 记录),`io.github.hhhrrr777` 以 GitHub 账号即可验证,零等待零成本;④ 「目录即模板 + jar 内 walk」(ADR-0001)要求模板是运行时资源,放 `src/main/resources/` 下 fat jar 分发零额外处理;⑤ 多模块(Maven 插件渠道,ADR-0001 路线图项)是远期需求,Maven 下拆模块是低成本重构,现在预拆只剩仪式成本——以 `cli` / `core` 包分包留接缝即可。

## Considered Options

- **Gradle**:增量构建与灵活性对一个单 artifact 的小 CLI 收益甚微,shade 等价物(Shadow 插件)与发布链均非官方内置,且与贡献者社区(Maven 为主)不同构。拒绝。
- **多模块预拆(`jfast-core` / `jfast-cli`)**:Initializr 式「生成核心独立库」与 Maven 插件渠道(ADR-0001 路线图)兑现时才有多模块的真实收益;MVP 只有一个 fat jar CLI。拒绝,包结构留接缝。
- **`io.jfast` groupId(先购 `jfast.io` 域名)**:长期更干净,但多一步域名采购与续费负担,且已发布坐标不可迁移——将来若购域名或转 org,新坐标另起、旧坐标留在 Central。MVP 拒绝。
- **模板树放仓库顶层、构建期拷入 jar**:多一个拷贝步骤,无收益;`getResource` walk 的运行时契约还要求拷贝规则与资源布局严格同步。拒绝。
- **`baseline/` 独立仓库**:模板化搬迁时跨仓对照 diff 与引用都不便;同仓独立工程(不进 reactor)已无构建耦合。拒绝。
- **保留 `dev` 集成分支**:对「一批 `ready-for-agent` 任务并行施工」多一层合并、agent 领工多记一个目标分支;main 直提 + CI 门禁 + squash 已足够防烂。拒绝,dev 归并后删除。
- **merge commit 保留施工过程**:agent 施工的中间提交(试错、返工)多为噪音;squash 让 main 历史与任务 issue 一一对应,`git show` 一次看全一个单元。拒绝。

## Consequences

- **包结构接缝**:生成器源码按 `cli`(picocli 入口、交互层)/ `core`(生成管线、模板渲染)分包,为将来拆多模块留落点;包名前缀统一 `io.github.hhhrrr777.jfast`。
- **一次性设置动作**(施工启动前完成,归实施计划总览承接):`dev` 归并 `main` 后删除;GitHub 仓库设置仅留 squash merge;main 开分支保护(要求 PR、要求 `ci.yml` 全绿、禁 force push)。
- **发布工作流不在本骨架内**:Maven Central 发布 + GitHub Releases + jbang catalog 校验归「任务:Maven Central 发布通道准备」(#23)。
- **baseline 是否进 CI 编译**、更广测试矩阵,归「拍板:生成器自身测试策略」(#22);本 ADR 只锁定 ADR-0004 已定的预设门禁底线。
- 组内 agent 施工的分支命名扩展为:`feat/<施工单元>`;既有 `research/*`、`adr/*`、`prototype/*` 前缀不变。

依据:拍板票据 <https://github.com/hhhrrr777/jfast/issues/16>(两轮 grilling,Q1–Q9 全部按推荐落定)、原型分支 `prototype/java-cli-minimal`(Maven + shade + resources 模板树实测)、ADR-0001(分发链与目录即模板)、ADR-0004(预设目录结构与 CI 门禁底线)。
