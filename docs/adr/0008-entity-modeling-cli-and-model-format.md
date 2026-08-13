# 实体建模 CLI 交互与模型落盘格式:逐字段循环问答 + 版本化 JSON schema

承接 ADR-0002(后端六件)与 ADR-0003(前端四件 + init.sql):产物已定,本 ADR 定**输入**——实体建模的交互采集流程与模型落盘格式。交互采用**逐字段循环问答**(字段名 → 显示名 → 抽象类型 → 类型追问 → 三布尔多选),末尾字段摘要表确认后落盘;落盘为 `.jfast/models/<实体>.json`,**版本化(`version: 1`)+ 白名单字段(未知字段报错)**,字段类型用**九种抽象类型**(string / text / integer / long / decimal / boolean / date / datetime / enum),枚举字段内联 `dict`(值→显示名键值对)或 `dictRef` 引用既有字典;参数模式 `jfast model --model-file <path>` 直读 JSON 生成,与向导共用同一 schema。核心论证:① 字段是开放集合,与 ADR-0004 向导的固定问题集不同构——自研交互层(jline-terminal 裸原语)上线性循环问答成本最低,嵌套列表编辑成本高,逐字段循环与枚举键值对循环共用同一种循环原语;② 抽象类型集是 ADR-0003 控件映射与 ADR-0002 三方言 DDL 映射的共同出发点,模型文件不耦合 Java,给远期 DSL 与非 Java 目标留兼容姿态;③ `--model-file` 一旦存在,JSON 就是用户可见的公共接口,版本字段 + 白名单(与 preset.yaml 同哲学)给远期 DSL 演进留迁移抓手,且「可手写的 JSON」是落盘格式的天然验收。

## Considered Options

- **一张大 Tab 表单内嵌套字段列表编辑**:字段开放集合嵌套进表单,自研交互层(~300 行裸原语)实现成本高、手感未验证。拒绝,逐字段循环 + 摘要确认页代替;字段编辑(改错)MVP 不做,摘要确认时选重来,与「撞车即报错退出」的 MVP 心智一致。
- **字段类型直选 Java 类型(String/Integer/BigDecimal…)**:模型文件耦合目标语言,前端控件与 DDL 需从 Java 类型反推抽象语义(text vs string 无法区分)。拒绝。
- **枚举单行速记(`0=启用,1=停用`)**:省不了几步,多一套解析与报错面。拒绝,键值对循环。
- **三布尔连问三个 y/n / 全默认不问**:前者啰嗦(10 字段多 30 次按键),后者搜索条件永远配不上。拒绝,单多选行 + 类型感知默认值(默认仅勾「列表显示」)。
- **父菜单 id 落盘进 JSON**:它是一次性生成参数(init.sql 挂载点),非模型属性;再生成同名撞车即报错,无重放需求。拒绝落盘,结尾「生成接线」阶段采集,仅 full 预设工程内出现。
- **JSON 不承诺稳定、远期 DSL 另起炉灶**:`--model-file` 使 JSON 成为公共接口,「不承诺」自欺。拒绝,承诺稳定 + `version` 整型 schema 版本,字段命名 camelCase 对齐 JHipster `.jhipster/*.json` 心智(fieldName/fieldType)。
- **审计字段(create_by/create_time/update_by/update_time)纳入 MVP**:涉及登录上下文取当前用户与 MP 自动填充处理器,是一个功能而非模板细节。拒绝,记入地图迷雾;用户可自建四字段顶用。
- **SQL 保留字撞车全量校验报错**:三方言保留字全集维护成本高,撞车时数据库报错足够直白。拒绝,降级为约 30 词公共高危子集(order/desc/group/key/user 等)的**警告**(不阻断)。

## Consequences

- **落盘 schema 定稿**(示例为「客户」实体):

```json
{
  "version": 1,
  "entityName": "Customer",
  "tableName": "biz_customer",
  "module": "business",
  "fields": [
    {
      "fieldName": "name",
      "label": "客户名称",
      "fieldType": "string",
      "length": 128,
      "required": true,
      "showInList": true,
      "showInSearch": true
    },
    {
      "fieldName": "status",
      "label": "状态",
      "fieldType": "enum",
      "required": true,
      "showInList": true,
      "showInSearch": true,
      "dict": {
        "name": "business_customer_status",
        "items": [
          { "value": "0", "label": "正常" },
          { "value": "1", "label": "停用" }
        ]
      }
    }
  ]
}
```

- **采集顺序与默认值**:实体名 → 模块名(默认 `business`,落盘) → 字段循环(字段名 → 显示名 → 类型 → 类型追问 → 三布尔多选) → 摘要表确认(可覆盖 `tableName` 推导值) → 生成接线(仅 full:父菜单 id,默认骨架「业务功能」目录,不落盘)。
- **类型追问矩阵**:`string` 追问长度(默认 255);`decimal` 追问精度/标度(默认 18,2);`enum` 追问键值对循环 + 字典类型名(默认派生 `<模块>_<实体>_<字段>` 可覆盖);其余六型零追问。类型追问字段(`length`/`precision`/`scale`)仅对应类型出现在 JSON。
- **enum 表达**:字典值一律字符串(对齐若依 `sys_dict_data.dict_value`),Java/TS 侧生成 `String`,DDL `VARCHAR(64)`;`dictRef` 引用既有字典时不产 init.sql 字典段。
- **搜索匹配方式不采集**:`string`/`text` 默认 `LIKE` 模糊,其余类型精确等值。
- **tableName 推导**:`snake_case(entityName)`,**不带模块前缀**,摘要页可覆盖后落盘。
- **主键**:模板自动注入 `id BIGINT`,不进字段循环、不落盘;字段名禁止 `id`。
- **显示名(label)是第 4 项必需元数据**(ADR-0003 三布尔之外的实际缺口):前端表单 label、表格列头、DDL 列注释、Bean Validation 消息共用。
- **空工程分叉在交互侧的唯一体现**:不问父菜单 id;产物侧分叉(无 `v-hasPermi`、不产 init.sql)ADR-0003 已定。
- **校验规则集**(交互期即时重问与 `--model-file` 读入报错清单共用):① entityName 大驼峰 Java 标识符、非 Java 关键字;② fieldName 小驼峰、非关键字、不重名、非 `id`;③ label 非空;④ enum 至少 2 项、value 不重;⑤ string 长度 1–4000,decimal 精度 1–38 且标度 ≤ 精度;⑥ tableName/module 小写标识符;⑦ SQL 保留字高危子集仅警告。
- **记入迷雾**:实体审计字段(是否作为建模可选项纳入)。

依据:拍板票据 <https://github.com/hhhrrr777/jfast/issues/21>(两轮 grilling 问答 Q1–Q12,全部按推荐落定)。
