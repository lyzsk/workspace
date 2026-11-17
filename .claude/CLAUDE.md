# workspace · 项目级约束

## 0. 代码交付方式（最高优先）

- **涉及代码的改动，一律由用户手动复制粘贴。** AI 只生成 `docs/cv/` 下的代码与操作说明，不直接修改工程源码。
- **服务进程（后端/前端/数据库）由用户手动启动和停止。** AI 只构建、验证、给出启动命令，不代起、不代停。
- 代码里不要加解释性注释；说明写到 `docs/cv/`。保留现有 `@author/@since` 头。
- 改任何文件前先读同一个完整路径；不凭记忆猜结构。
- 不碰 `.claude/settings*`；git add/commit/push 归用户。
- 结论带置信度、依据、失效条件和不自信点。

## 1. 参照项目（读源码，不猜）

- youlai-boot：`C:\Users\admin\dev\youlai-boot`
- ruoyi-vue-pro：`C:\Users\admin\dev\ruoyi-vue-pro-master`
- RuoYi-Vue：`C:\Users\admin\dev\RuoYi-Vue-master`（若依 v3.9.2 后端，Spring Boot 4.x / JDK17，模块 ruoyi-admin/common/framework/system/quartz/generator；前端是独立仓库）
- art-design-pro：`C:\Users\admin\dev\art-design-pro-main`
- continew-admin：`C:\Users\admin\dev\continew-admin-dev`
- continew-starter：`C:\Users\admin\dev\continew-starter-dev`

## 2. 技术栈（项目统一版本）

- Java 17；Spring Boot 3.5.16；MyBatis-Plus 3.5.18.1；PostgreSQL 17.x；Maven 3.9.x
- 前端计划：Vue3 + Vite + TypeScript + Pinia + Element Plus + SCSS
- 版本只在根 `pom.xml` / `package.json` 写；子模块不写版本。

## 3. 模块与包

```
workspace-common     cn.sichu.common.{base,config,event,exception,id,log,result,utils}
workspace-system     cn.sichu.system.{app,auth,user,role,menu,dict,dept,log,online,notice,file,security,config,sync}
workspace-tool       cn.sichu.tool.{config,document}
workspace-quartz     cn.sichu.quartz.{component,config,controller,entity,handler,manager,mapper,model,service}
workspace-generator  cn.sichu.generator.{controller,model,service}
workspace-admin      cn.sichu.admin.Application
workspace-ui         Vue3 + Vite + TS + Pinia + Element Plus
```

依赖方向：`admin → {system, tool, quartz, generator}`；`tool → system`；`quartz/generator → common`。framework 与 system 合并，不单独建 framework 模块，但保留包级边界。
> 2026-11-14 用户决定删除 `workspace-core`（一直没有真实契约可放，YAGNI）；后续如出现第二个业务应用再按需新增。

system 业务包内分层：

```
controller / service（接口 I*Service）/ service/impl / mapper /
model{entity,form,query,vo} / converter / code
```

注意：目录名和包名用 `service`，不要用拼错的 `serivce`。

接口命名统一为 `I + 完整业务名 + Service`，实现类为 `...ServiceImpl`；业务名写全，不用缩写或省略业务域。示例：`INotificationPreferenceService` / `NotificationPreferenceServiceImpl`，不要写成 `INoticePrefService` / `NoticePrefServiceImpl`。

配置前缀同样用完整业务名，不要用 `workspace.eap` 这类缩写；环境变量同理，用应用全名的大写下划线形式（如 `EAP_TOOL_SERVICE_*`）。

## 4. 实体、ID、响应

- `BaseEntity`：9 个公共字段（id、create_by/time、update_by/time、is_deleted、deleted_by/time、remark）。
- `TenantEntity extends BaseEntity`：多一个 `tenant_id`（当前预留，默认 0）。
- 雪花 ID：`@TableId(type = IdType.ASSIGN_ID)` + `SnowflakeIdGenerator` + `IdConfig` 覆盖 `IdentifierGenerator`。
- 雪花 import：`com.baomidou.mybatisplus.spring.service.impl.ServiceImpl`（MP 3.5.18.1 的包路径，**不是** `extension`）。
- 雪花 EPOCH：`2026-11-14T00:00:00+08:00` = `1794585600000L`；全实例/全环境一致，生成 ID 后不再改。
- Long → String：`JacksonConfig` 已把 Long/ long 序列化为字符串；前端 ID 类型用 `string`。
- `ResultCode`：`SUCCESS=A0001`、`FAILED=A0002`；通用码 A0003~A0007；业务码 A1xxx=system。子系统错误码由子系统自定，不占 workspace 命名空间。
- `Result.failed(IResultCode)` 统一出口；`BusinessException` 只抛业务可预期错误；系统异常由 `GlobalExceptionHandler` 兜底。
- 业务异常 `warn`，只打 `code + msg`；系统异常 `error` + 堆栈；错误响应不返回堆栈/SQL。

## 5. 多租户（当前整体预留）

- **公司 = 租户 = `sys_tenant`**，不是组织节点；不建 `sys_company`。
- 当前公司内部单租户：`tenant_id=0`，`sys_tenant` 一条默认公司；**不启用** `TenantLineInnerInterceptor`。
- `TenantEntity`、`TenantContext`、`sys_tenant`、`TenantIgnoreTables` 保留给未来；当前权限先用 `dept + role`。
- 未来出现多公司/子公司/SaaS 时，再启用多租户插件；全局表（tenant/app/menu/dict）不含 `tenant_id`。

## 6. 数据库

- PostgreSQL 17，当前只建 `system` schema；`eap` / `cim` 后续再加。admin 连接 `currentSchema=system`。
- 主键 `BIGINT`（雪花），不是 IDENTITY/AUTO_INCREMENT。
- 审计字段 + 逻辑删除 `is_deleted`（0/1）；唯一索引带 `WHERE is_deleted = 0`。
- `sys_user`：`username` 登录账号（唯一）、`real_name` 真实姓名（展示/审批）、`gender`（0未知/1男/2女）、`status`（1启用/0停用）、`dept_id` 可为空；**没有 nickname**。
- `sys_role.data_scope`：1=全部、2=指定部门（`sys_role_dept`）、3=本部门、4=本部门及以下、5=仅本人；默认 5。
- `sys_role_dept`：角色-部门关联，只给 `data_scope=2` 用。
- 全局表（`sys_tenant`/`sys_app`/`sys_menu`/字典）不含 `tenant_id`；租户表（user/role/dept/关联表/业务表）含 `tenant_id` 预留。
- 完整结构只在 `workspace-admin/src/main/resources/db/migration/V1__baseline.sql`；后续结构变化新增递增迁移，不改已发布旧版本。
- 本地 admin/test 只由 `sql/seed_user.sql` 初始化，不进入 V1，不记录明文口令；migrator 与运行期 DML 账号分离。

## 7. 当前已完成：common/system/tool/quartz/generator/admin 模块划分、RBAC 与按钮权限、微前端宿主、Quartz RAMJobStore、Generator 元数据到预览/ZIP、Flyway 单一 V1 与本地 seed。

下一步以 `docs/TODO.md` 为准：浏览器人工验收、Generator 模板扩展、微前端子应用契约完善。

## 8. 前端约定（待建）

- `workspace-ui`：Vue3 + Vite + TS + Pinia + Element Plus + SCSS；先单应用，不搞 monorepo。
- 登录页参考 `art-design-pro/src/views/auth/login/index.vue`；主布局参考 `src/views/index/index.vue` 和 `src/components/core/layouts/`。
- 路由单链路：`/login` + `/` 布局；守卫只做登录态和 404；store 不 import router。
- 请求：axios baseURL `/api`，Vite proxy 到 `http://localhost:61114`；请求头 `Authorization: Bearer <token>`；401 清 token 回登录页。
- 颜色用 CSS 变量，暗色才能生效；不写死 `#fff`。
