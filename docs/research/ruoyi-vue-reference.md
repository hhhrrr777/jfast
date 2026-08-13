# RuoYi-Vue 功能参照底稿与许可证标注义务

> 研究票据:#18(wayfinder 地图 #15 子任务)
> 研究日期:2026-08-13
> 参照对象:[yangzongzhuan/RuoYi-Vue](https://github.com/yangzongzhuan/RuoYi-Vue)(master 分支,MIT License)、[yangzongzhuan/RuoYi-Vue3](https://github.com/yangzongzhuan/RuoYi-Vue3)(前端 Vue3 版)、[dromara/RuoYi-Vue-Plus](https://github.com/dromara/RuoYi-Vue-Plus)(双 token / 多端登录参考)
> 用途:作为 jfast 按自身规范重写系统管理功能(用户/角色/菜单/日志/字典/文件上传/认证)时的对照底稿。**只参照设计,不复制代码。**

---

## 1. 用户 / 角色 / 菜单(按钮级权限)

### 1.1 表结构

来源:`sql/ry_20260417.sql`([GitHub 链接](https://github.com/yangzongzhuan/RuoYi-Vue/blob/master/sql/ry_20260417.sql),行号为该文件 master 版行号)。

**sys_user(用户信息表,L42-76)** — 核心字段:

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | bigint PK auto_increment | 用户ID |
| dept_id | bigint | 部门ID |
| user_name | varchar(30) not null | 登录账号 |
| nick_name | varchar(30) | 昵称 |
| user_type | varchar(2) default '00' | 用户类型(00 系统用户) |
| email / phonenumber | varchar(50)/(11) | 邮箱 / 手机号 |
| sex | char(1) | 0男 1女 2未知(走字典) |
| avatar | varchar(100) | 头像地址 |
| password | varchar(100) | BCrypt 哈希(种子数据为 `$2a$10$...`) |
| status | char(1) | 0正常 1停用 |
| del_flag | char(1) | 0存在 2删除(逻辑删除) |
| login_ip / login_date | varchar(128)/datetime | 最后登录 IP/时间 |
| pwd_update_date | datetime | 密码最后更新时间 |
| create_by/create_time/update_by/update_time/remark | 审计五件套 | 所有业务表通用 |

**sys_role(角色信息表,L105-128)** — 核心字段:role_id(PK)、role_name、**role_key(角色权限字符串,如 `admin`、`common`)**、role_sort、**data_scope(char(1):1全部 2自定 3本部门 4本部门及以下 5仅本人——数据权限范围)**、menu_check_strictly / dept_check_strictly(树勾选是否父子关联)、status、del_flag + 审计字段。

**sys_menu(菜单权限表,L134-158)** — 核心字段:

| 字段 | 类型 | 说明 |
|---|---|---|
| menu_id | bigint PK | 菜单ID |
| menu_name / parent_id / order_num | — | 树结构(parent_id 默认 0 为根) |
| path | varchar(200) | 路由地址 |
| component | varchar(255) | 前端组件路径 |
| query / route_name | — | 路由参数 / 路由名称 |
| is_frame / is_cache | int(1) | 是否外链 / 是否缓存 |
| **menu_type** | char(1) | **M目录 C菜单 F按钮** |
| visible / status | char(1) | 显示/隐藏、正常/停用 |
| **perms** | varchar(100) | **权限标识,如 `system:user:list`** |
| icon | varchar(100) | 图标 |

**关联表**:`sys_user_role`(user_id+role_id 联合主键,L268-273)、`sys_role_menu`(role_id+menu_id 联合主键,L285-290)。另有 sys_role_dept(自定义数据权限用)、sys_user_post(岗位)。

菜单种子数据中,按钮(menu_type='F')挂在功能菜单下,perms 取值为 `system:user:list`、`system:user:add`、`system:user:edit`、`system:user:remove`、`system:user:export` 等,即「模块:实体:动作」三段式冒号串。

### 1.2 按钮级权限的校验链路

来源:DeepWiki 对 RuoYi-Vue 的代码分析(类文件路径见下)。

1. 后端在 Controller 方法上标注 `@PreAuthorize("@ss.hasPermi('system:user:list')")`。
2. `ss` 是 `PermissionService` 的 Spring Bean 名(`@Service("ss")`,位于 `ruoyi-framework/.../web/service/PermissionService.java`)。`hasPermi()` 从 SecurityContext 取 LoginUser,检查其权限集合是否含 `*:*:*`(超管全量标识)或目标 perms 串。
3. 权限集合由 `SysPermissionService.getMenuPermission(user)` 装配:超管直接给 `*:*:*`;否则按角色走 `sys_menu ⋈ sys_role_menu ⋈ sys_user_role ⋈ sys_role` 联查取 perms,逗号拆分后放入 `Set<String>`。
4. 前端(Vue2 `ruoyi-ui/src/directive/permission/hasPermi.js`;Vue3 同构)自定义指令 `v-hasPermi="['system:user:add']"`,从 Vuex/Pinia 的 `permissions` 读取,无权限则 `el.parentNode.removeChild(el)` 直接从 DOM 移除按钮。

要点:**前后端双校验**——后端注解是安全边界,前端指令只做展示裁剪;权限集合在登录/刷新用户信息时一次性下发。

### 1.3 关键接口形态(RESTful 约定)

以 SysUserController 为代表(角色/菜单同构):

| 操作 | 方法与路径 | 权限 | 出入参 |
|---|---|---|---|
| 分页列表 | GET `/system/user/list` | system:user:list | 查询实体作 query 参数;返回 `TableDataInfo{rows,total}` |
| 详情 | GET `/system/user/{userId}` | system:user:query | 返回 `AjaxResult{code,msg,data}` |
| 新增 | POST `/system/user` | system:user:add | `@RequestBody` 实体;返回 AjaxResult |
| 修改 | PUT `/system/user` | system:user:edit | 同上 |
| 删除 | DELETE `/system/user/{userIds}` | system:user:remove | 路径变量支持 id 数组(逗号分隔) |
| 导出 | POST `/system/user/export` | system:user:export | 流式输出 Excel |

约定:`AjaxResult`(code/msg/data)为通用响应;分页由 PageHelper 配合 `startPage()` 完成;新增/修改走 `@Validated` 分组校验;业务前置校验(账号唯一、admin 保护)在 Controller 层完成。

---

## 2. 操作日志 / 登录日志

### 2.1 表结构

来源:`sql/ry_20260417.sql`。

**sys_oper_log(操作日志记录,L420-445)**:oper_id(PK)、title(模块标题)、business_type(int:0其它 1新增 2修改 3删除…,对应字典 sys_oper_type)、method(方法全名)、request_method(GET/POST…)、operator_type(0其它 1后台 2手机端)、oper_name、dept_name、oper_url、oper_ip、oper_location(归属地)、oper_param(varchar 2000,截断)、json_result(varchar 2000)、status(0正常 1异常)、error_msg、oper_time、cost_time(bigint,毫秒)。索引:business_type、status、oper_time。

**sys_logininfor(系统访问记录,L563-578)**:info_id(PK)、user_name、ipaddr、login_location、browser、os、status(char(1):0成功 1失败)、msg、login_time。索引:status、login_time。

### 2.2 切面实现思路(操作日志)

来源:DeepWiki 代码分析。

- 自定义注解 `@Log(title="用户管理", businessType=BusinessType.INSERT, ...)`,字段含 title、businessType、operatorType、isSaveRequestData、isSaveResponseData、excludeParamNames(敏感参数排除)。
- 切面 `LogAspect`(`ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/LogAspect.java`):`@Before` 记录起始时间;`@AfterReturning` / `@AfterThrowing` 统一走 `handleLog`,组装 SysOperLog(IP、URL、请求方式、方法名、操作人、请求参数——密码等敏感字段过滤——返回值、耗时、异常信息)。
- **异步落库**:`AsyncManager.me().execute(AsyncFactory.recordOper(operLog))`,用 TimerTask 提交到独立线程池,日志不阻塞主请求。

### 2.3 登录日志记录链路

- `SysLoginService`(`ruoyi-framework/.../web/service/SysLoginService.java`):认证成功后 `AsyncFactory.recordLogininfor(username, LOGIN_SUCCESS, ...)`;各认证异常分支记录 LOGIN_FAIL;同时 `userService.updateLoginInfo` 回写 sys_user 的 login_ip/login_date。
- 退出:`LogoutSuccessHandlerImpl`(`ruoyi-framework/.../security/handle/LogoutSuccessHandlerImpl.java`)记录 LOGOUT。
- `AsyncFactory.recordLogininfor` 组装 IP 归属地、浏览器、OS(UA 解析)后由 `ISysLogininforService.insertLogininfor` 落库。
- 查询端:`SysLogininforController`(`ruoyi-admin/.../controller/monitor/SysLogininforController.java`)提供 list/export/删除/清空;另有「解锁用户」类管理动作。

**重写要点**:注解驱动 + AOP + 异步队列,三者解耦;日志表只追加、高频查询字段建索引;敏感参数在切面层过滤。

---

## 3. 字典(sys_dict_type / sys_dict_data)

### 3.1 表结构

来源:`sql/ry_20260417.sql`。

**sys_dict_type(字典类型表,L449-465)**:dict_id(PK)、dict_name、**dict_type(varchar(100),唯一键,如 `sys_user_sex`、`sys_normal_disable`)**、status + 审计字段。

**sys_dict_data(字典数据表,L480-501)**:dict_code(PK)、dict_sort、dict_label(显示文本)、dict_value(存储值)、dict_type(外联类型串)、css_class、**list_class(表格回显样式,如 primary/danger,直接对应前端 tag 类型)**、is_default(Y/N)、status + 审计字段。

### 3.2 前后端用法

来源:DeepWiki 对 RuoYi-Vue / RuoYi-Vue3 的代码分析。

后端:
- `DictUtils` 用 Redis 缓存,key = `CacheConstants.SYS_DICT_KEY + dictType`;启动时 `SysDictTypeServiceImpl.loadingDictCache()` 预热;增删改时失效对应 key。
- 前端消费主接口:**GET `/system/dict/data/type/{dictType}`**(免鉴权或登录即可,按类型取启用项),缓存未命中回源数据库并回填。

前端(Vue2 ruoyi-ui):
- 组件声明 `dicts: ['sys_normal_disable']`,由全局插件自动加载;模板用 `dict.type.sys_normal_disable` 渲染选项;表格列用全局组件 `<dict-tag :options="dict.type.xxx" :value="row.status"/>` 按 list_class 上色回显。

前端(Vue3):
- `useDict('sys_user_sex', ...)` 组合式函数(`src/utils/dict.js`):先查 Pinia `useDictStore` 缓存,未命中调接口、规整为 `{label, value, elTagType...}` 后入 store;`<dict-tag>` 组件同样按 list_class/css_class 回显;字典变更后 `removeDict(dictType)` 失效前端缓存。

**重写要点**:类型串为逻辑外键;两级缓存(后端 Redis + 前端 store);label/value/elTagType 三元组是前后端契约。

---

## 4. 本地文件上传

来源:DeepWiki 代码分析(类路径见下)。

- 配置:`application.yml` 的 `ruoyi.profile` 指定本地上传根目录(如 `D:/ruoyi/uploadPath`),经 `RuoYiConfig.getProfile()/getUploadPath()/getAvatarPath()` 读取。
- 上传工具 `FileUploadUtils.upload(baseDir, file, allowedExtensions)`:
  1. 文件名长度 ≤ 100、大小 ≤ 50MB(`DEFAULT_MAX_SIZE`);
  2. 扩展名白名单(`MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION` / `IMAGE_EXTENSION`);
  3. 文件名重生成:`yyyy/MM/dd` 日期目录 + 原文件名+序列号,或纯 UUID(头像强制 UUID);
  4. 落盘后返回 **`Constants.RESOURCE_PREFIX`(`/profile`)+ 相对路径** 作为 URL。
- 资源映射:`ResourcesConfig.addResourceHandlers` 把 **`/profile/**` 映射到本地 profile 目录**,上传文件即可直接通过 URL 访问。
- 接口:
  - 通用上传:`CommonController` POST `/common/upload`(单文件)/`/common/uploads`(多文件),返回 `{url, fileName, newFileName, originalFilename}`;
  - 资源下载:`/common/download/resource`;
  - 头像:`SysProfileController` POST `/system/user/profile/avatar`(限图片扩展名,成功后更新 sys_user.avatar 并删除旧文件)。
- 前端:`ImageUpload`/`FileUpload`/`Editor` 组件统一打 `/common/upload`。

**重写要点**:磁盘路径与 URL 前缀解耦(便于将来切对象存储);扩展名白名单 + 大小限制在工具层强制;返回相对 URL 而非绝对路径。

---

## 5. 双 token JWT(access + refresh)

### 5.1 若依原版:单 token 方案(对照基线)

来源:DeepWiki 对 `TokenService` 的分析(`ruoyi-framework/.../web/service/TokenService.java`)。

- 登录成功生成 UUID,`LoginUser` 以 `login_tokens:{uuid}` 为 key 存 Redis,有效期 `token.expireTime`(默认 30 分钟);JWT 本身只携带 uuid 引用与用户名,签名密钥在 `token.secret`。
- 请求头 `Authorization: Bearer <jwt>`;每次请求 `verifyToken` 时若剩余有效期 < 20 分钟则自动续期(刷新 Redis 过期时间),即「滑动过期」。
- 退出删除 Redis 记录使 JWT 失效。
- **没有独立 refresh token,也没有多端概念**——同一账号后登录覆盖/共存取决于配置,本质是「服务端会话 + JWT 引用」。

### 5.2 RuoYi-Vue-Plus 的做法(Sa-Token 体系)

来源:DeepWiki 对 dromara/RuoYi-Vue-Plus 的分析。

- 弃自研 TokenService,改用 **Sa-Token + JWT**(`StpLogicJwtForSimple`,配置于 `SaTokenConfig`)。
- 登录走策略模式:`AuthController /auth/login` 按 `grantType` 选择 `IAuthStrategy`(密码/短信/邮箱/小程序/社交),校验客户端(clientId)与租户后 `LoginHelper.login()` → `StpUtil.login()`。
- **`LoginVo` 同时返回 `accessToken/expireIn` 与 `refreshToken/refreshExpireIn` 字段**——双 token 契约的体现。
- **多端并存**:`SaLoginParameter.deviceType` 区分登录设备,配合 sa-token 的 `is-concurrent` 配置实现同账号多设备各自持 token。
- 会话持久化:自定义 `PlusSaTokenDao`,**Caffeine(本地)+ Redis(分布式)二级缓存**。

### 5.3 社区常见双 token 改造要点(通用实践归纳)

以下为业界通行做法的综合,供 jfast 自研时参考(非某一仓库逐字事实):

1. **职责划分**:access token 短效(常见 15min–2h),无状态校验;refresh token 长效(7–30 天),仅用于换新。
2. **refresh 落库/落 Redis**:以 `refresh_tokens:{userId}:{deviceId}` 形式存储(哈希后),支持主动吊销与「踢下线」;落库则便于审计与多端管理页展示。
3. **多端并存**:登录时携带 device/client 标识,token 与设备绑定;同设备重登覆盖,异设备共存;可在用户中心列出在线设备并单独注销。
4. **刷新接口**:`POST /auth/refresh` 仅接受 refresh token,校验通过签发新 access(推荐同时旋转 refresh——refresh rotation,旧 refresh 立即作废,重放即判定盗用并吊销全链)。
5. **退出/改密**:删除对应设备的 refresh 记录;access 因短效自然过期(或维护短 TTL 黑名单)。
6. **对比若依原版**:从「Redis 会话 + 滑动续期」迁到双 token,主要收益是移动端长登录、服务端可精确吊销、多端管理;代价是要实现 refresh 存储与旋转逻辑。

---

## 6. MIT 许可证下的标注义务

### 6.1 许可证原文关键条款

来源:[RuoYi-Vue/LICENSE](https://github.com/yangzongzhuan/RuoYi-Vue/blob/master/LICENSE):

```
The MIT License (MIT)
Copyright (c) 2018 RuoYi
...
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

义务唯一且明确:**当你分发该软件的副本或「实质部分」(substantial portions)时,必须附上上述版权声明与许可声明**。MIT 不禁止商用、修改、再许可,也不要求衍生作品开源。

### 6.2 分场景结论

| 场景 | 义务 |
|---|---|
| 逐字复制 RuoYi 源码文件/大段代码 | **必须**在该文件保留原版权头(或在文件头注明出处并附 MIT 声明),并在仓库级保留其 LICENSE 文本 |
| 复制少量片段(如某个工具方法) | 稳妥起见同样标注:`// 源自 RuoYi-Vue (MIT), https://github.com/yangzongzhuan/RuoYi-Vue, Copyright (c) 2018 RuoYi` |
| **仅参照设计重写(本项目目标)**:表结构思路、perms 串约定、接口形态、AOP 日志思路等**思想与功能设计** | 版权不保护思想/方法/功能性设计,自行重写的代码在法律上不构成「副本或实质部分」,**无强制标注义务**;但为避免争议并示尊重,**建议**在仓库 NOTICE 中作归属声明 |

注意:表结构(字段名/类型)处于思想与表达的灰色地带,直接整段搬运 DDL 脚本(含注释文案)更接近「复制表达」,应尽量避免逐字照抄;按 jfast 规范重新设计字段命名与注释即可。本结论为工程实践建议,非正式法律意见。

### 6.3 建议的标注模板

仓库级 `NOTICE`(或 README 致谢段):

```
本项目的系统管理模块(用户/角色/菜单权限模型、操作日志切面、字典、文件上传等)
在设计上参照了 RuoYi-Vue(https://github.com/yangzongzhuan/RuoYi-Vue),
其版权归 RuoYi 所有,按 MIT License 发布。本项目未直接复制其源代码。
Copyright (c) 2018 RuoYi (MIT License)
```

文件级(仅当某文件确实改写自 RuoYi 源码时):

```java
/*
 * 本文件改写自 RuoYi-Vue 的 <原文件路径>
 * 原项目: https://github.com/yangzongzhuan/RuoYi-Vue
 * Copyright (c) 2018 RuoYi, MIT License
 */
```

纯参照思路、全新编写的文件**不需要**文件头标注,靠仓库级 NOTICE 覆盖即可。

---

## 7. 对 jfast 重写的落地建议(摘要)

1. 权限模型沿用「用户-角色-菜单(含按钮)+ perms 三段串」,前后端双校验;数据权限(data_scope)按需要取舍。
2. 日志:注解 + AOP + 异步落库;oper_log/logininfor 只追加,索引照抄思路(业务类型/状态/时间)。
3. 字典:type 串为逻辑外键,后端缓存 + 前端 store 两级,{label, value, tagType} 契约。
4. 上传:profile 根目录 + URL 前缀映射解耦,白名单/大小在工具层强制,返回相对 URL。
5. 认证:若需移动端长登录与多端管理,采用 §5.3 双 token + refresh 旋转;否则若依原版「JWT 引用 + Redis 会话 + 滑动续期」更简单。
6. 许可证:加仓库级 NOTICE(§6.3 模板);不逐字搬运 DDL/代码即可规避文件级义务。

---

## 8. 来源清单

| # | 来源 | 内容 |
|---|---|---|
| 1 | [RuoYi-Vue `sql/ry_20260417.sql`](https://github.com/yangzongzhuan/RuoYi-Vue/blob/master/sql/ry_20260417.sql) | 全部表结构 DDL 与种子数据(L42-76 sys_user、L105-128 sys_role、L134-158 sys_menu、L268-290 关联表、L420-445 sys_oper_log、L449-501 字典两表、L563-578 sys_logininfor) |
| 2 | DeepWiki:[yangzongzhuan/RuoYi-Vue](https://deepwiki.com/yangzongzhuan/RuoYi-Vue) 问答 | perms 校验链路(PermissionService/SysPermissionService/hasPermi.js)、LogAspect/AsyncFactory 日志链路、FileUploadUtils/ResourcesConfig 上传链路、TokenService 单 token 机制、DictUtils 缓存(对应源码路径:ruoyi-framework 模块) |
| 3 | DeepWiki:[yangzongzhuan/RuoYi-Vue3](https://deepwiki.com/yangzongzhuan/RuoYi-Vue3) 问答 | Vue3 端 useDict/dict-tag/Pinia 字典缓存 |
| 4 | DeepWiki:[dromara/RuoYi-Vue-Plus](https://deepwiki.com/dromara/RuoYi-Vue-Plus) 问答 | Sa-Token+JWT、LoginVo 双 token 字段、deviceType 多端、PlusSaTokenDao 二级缓存 |
| 5 | [RuoYi-Vue LICENSE](https://github.com/yangzongzhuan/RuoYi-Vue/blob/master/LICENSE) | MIT 原文,「copyright notice + permission notice 须包含于所有副本或实质部分」 |
| 6 | §5.3 为业界通行实践归纳 | 非单一仓库事实,已在文中标注 |
