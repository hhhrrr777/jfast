# CLI 向导问题树与全参数模式参数集:两段式向导,kebab-case 全量参数

jfast 生成器的交互形态定为**两段式向导 + 全参数模式双轨**:命令形态为子命令树 `jfast new`(生成工程)/ `jfast entity`(实体建模),裸 `jfast` 打印 help;向导第一段是**预设独立单选屏**(必选无默认),第二段是按预设 `questions` 白名单装配的 **Tab 表单**(骨架三段固定:工程坐标 / 数据库 / 运行,字段按白名单增减);全参数模式承载全部信息位的 kebab-case 参数,非 TTY / dumb terminal 强制,**向导默认值仅作用于交互预填、全参数模式下不生效**。核心论证按权重:① 预设是问题树的装配开关(ADR-0004 白名单),必须先于 Tab 表单确定,故独立成屏而非表单字段;② 向导默认值全部做成「预填可改」(`com.example`/`demo`、`root`/`password123`),上手速度优先——默认值被原样接受的风险用「预填即可编辑」的交互形态消解,而非取消默认值;③ 全参数模式不读默认值、白名单内参数逐项校验齐全,承 ADR-0004「--preset 必填」的显式书写精神,CI 脚本不因默认值变迁而悄悄改产物;④ kebab-case 是 picocli 惯例,与生态一致。

## Considered Options

- **预设作为 Tab 表单第一个字段(单屏表单)**:选定预设前整个表单的字段集不确定,需整屏重排,自研交互层(~300 行,见 #5)复杂度上一个台阶。拒绝,preset 独立成屏;此决定修正 ADR-0004「预设是 Tab 表单必选单选项」的措辞,语义不变。
- **裸命令即生成(`jfast` = 生成工程)**:裸命令带默认行为使 help 发现性变差,且与「生成器不止一种生成动作」的能力面不符。拒绝,用子命令树。
- **命令名 `init`**:init 心智是「初始化当前目录」,与「目标目录非空即报错退出」的再生成策略语义冲突。拒绝,用 `new`。
- **groupId/artifactId 必填无默认**:坐标是工程身份,JHipster 的 `com.mycompany` 默认值常被原样回车接受——但预填可改的交互已给出同样的提示强度,必填只增摩擦。拒绝,给默认值 `com.example` / `demo`。
- **dbUser/dbPassword 无默认**:各库惯例账号不同(root/postgres/system),猜比不猜更糟——但开发库场景 `root`/`password123` 命中率足够高,且均预填提示可改。拒绝,给默认值。
- **二选一模式(带任何参数即视为全参数,缺项报错)**:实现最简单,但「先给 groupId 再慢慢选」的体验断掉。拒绝,TTY 下用混合模式:已给参数跳过对应问题,缺失项补问。
- **camelCase 参数(`--groupId`)**:picocli 惯例为 kebab-case。拒绝,统一 `--group-id` 形态,且不设短选项。
- **初始管理员密码进问题树**:full 预设有认证体系,但若依心智是初始化 SQL 写死 admin/admin123。拒绝,写死 + 文档提示首登改密。
- **数据库可选值只放 MySQL/PostgreSQL**:骨架层面(驱动坐标 + URL 模板)支持五个库成本极低,信创数据库是项目核心卖点(L1);达梦的缺口仅在实体建模 DDL 方言(ADR-0002 迷雾项),降级提示比缺席更诚实。拒绝,五选。

## Consequences

- **信息位全集与预设白名单**(preset.yaml `questions` 字段的落地取值):
  - empty:preset、groupId、artifactId、basePackage、jdkVersion、database。
  - full:empty 全部 + dbHost、dbPort、dbName、dbUser、dbPassword、serverPort。
  - 不进问题树:输出目录(参数 `--output-dir`,默认 `./<artifactId>/`,非空报错)、projectName(从 artifactId 推导)、description、构建工具(MVP 只 Maven)、初始管理员密码。
- **默认值与校验规则**:

  | 问题 | 默认值 | 校验 |
  |---|---|---|
  | groupId | `com.example`(预填可改) | Java 包名规则、点分段、至少两段 |
  | artifactId | `demo`(预填可改) | 小写字母/数字/连字符,首字符字母 |
  | basePackage | 推导 `groupId + '.' + artifactId`,连字符去除粘连(`my-app`→`myapp`),可覆盖 | Java 包名规则,不强制与 groupId 前缀一致 |
  | jdkVersion | `21` | 枚举 {17, 21, 25} |
  | database | `mysql` | 枚举 mysql / postgresql / dm / kingbase / opengauss(向导显示中文名) |
  | dbHost | `localhost` | — |
  | dbPort | 随 database 联动:3306 / 5432 / 5236 / 54321 / 5432;用户手改后不跟随联动 | 1024–65535 |
  | dbName | artifactId 规范化(连字符转下划线) | — |
  | dbUser | `root`(预填提示可改) | 必填 |
  | dbPassword | `password123`(预填提示可改) | 允许空,向导掩码输入 |
  | serverPort | `8080` | 1024–65535,提示范围 |

  校验失败在字段下方红字提示、原地重输(#5 原型已验证的模式)。
- **Tab 表单骨架三段固定**:Tab1「工程坐标」(groupId/artifactId/basePackage/jdkVersion)、Tab2「数据库」(database + full-only 连接五字段)、Tab3「运行」(serverPort);字段按白名单增减,骨架不变。
- **全参数模式参数面**:`--preset`、`--group-id`、`--artifact-id`、`--base-package`、`--jdk-version`、`--database`、`--db-host`、`--db-port`、`--db-name`、`--db-user`、`--db-password`、`--server-port`、`--output-dir`;其中 `--base-package`、`--output-dir` 可选(缺省走推导),其余按预设白名单逐项校验齐全;缺参报错一次性列全缺失项并附一行可拷贝的示例命令。
- **达梦降级**:选 `dm` 时实体建模自动 DDL 提示「暂不支持、手工建表」,与 ADR-0002 的达梦方言迷雾项衔接。
- **openGauss 端口取 5432**(文档默认值);OM 安装实践常用的 26000 属部署侧配置,用户手改即可。
- **实现期事实核查**:达梦/金仓/openGauss 的 JDBC 驱动 Maven 坐标与版本在施工任务中验证(达梦 DmJdbcDriver18 已在 Maven Central)。
- `jfast entity` 的参数面(建模输入、`.jfast/models/` 落盘)归实体建模施工票据,不在此 ADR。

依据:拍板票据 <https://github.com/hhhrrr777/jfast/issues/19>(两轮 grilling 问答 Q1–Q12,Q7/Q9 按用户改法落定,其余按推荐)、ADR-0001(非 TTY 强制全参数)、ADR-0004(questions 白名单、--preset 必填)、原型票据 <https://github.com/hhhrrr777/jfast/issues/5>(Tab 表单交互形态)。
