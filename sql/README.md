# workspace · PostgreSQL 快速参考

目标：PostgreSQL 17.x。当前单租户，`tenant_id` 默认 0，多租户仅预留。

## 初始化入口

- 完整结构和基础种子：`workspace-admin/src/main/resources/db/migration/V1__baseline.sql`
- Generator 菜单权限：`V2__generator_menu.sql`
- 本地 admin/test：`sql/seed_user.sql`（只存 BCrypt 哈希，不删除其他账号）
- 后续结构变化：新增递增迁移，不修改已发布旧版本。

需要手工初始化时：

```bash
"/c/Program Files/PostgreSQL/17/bin/psql.exe" \
  -h localhost -U postgres -d workspace -c "CREATE SCHEMA system AUTHORIZATION workspace;"

"/c/Program Files/PostgreSQL/17/bin/psql.exe" \
  -h localhost -U workspace -d workspace -v ON_ERROR_STOP=1 \
  -f workspace-admin/src/main/resources/db/migration/V1__baseline.sql

"/c/Program Files/PostgreSQL/17/bin/psql.exe" \
  -h localhost -U workspace -d workspace -v ON_ERROR_STOP=1 \
  -f sql/seed_user.sql
```

也可以直接启动后端，由 Flyway 执行 V1。

## 账号职责

- owner/migrator：创建 schema、执行 DDL 和 Flyway 迁移；
- `workspace`：应用运行账号，只负责业务 DML。

对应配置：

```text
DB_URL / DB_USERNAME / DB_PASSWORD
DB_MIGRATION_USERNAME / DB_MIGRATION_PASSWORD
```

## 表用途（26 张）

### 平台、组织与权限

| 表 | 用途 |
|---|---|
| `sys_tenant` | 默认公司/租户；当前只有 `id=0` 的默认租户 |
| `sys_app` | 微前端应用配置：路由、模块入口、样式、访问权限、启停 |
| `sys_tenant_app` | 租户与应用开通关系，预留 |
| `sys_dept` | 部门树；总公司为受保护的内置节点 |
| `sys_user` | 登录账号、资料、状态、tokenVersion、登录锁定信息 |
| `sys_role` | 角色、权限字符、数据范围、显示顺序、菜单父子联动 |
| `sys_role_dept` | 自定义数据范围（`data_scope=2`）的角色部门关联 |
| `sys_user_role` | 用户与角色关联 |
| `sys_menu` | 目录、菜单、按钮、路由、组件和权限码 |
| `sys_role_menu` | 角色与菜单/按钮权限关联 |

### 字典

| 表 | 用途 |
|---|---|
| `sys_dict_type` | 字典类型，如用户性别、通用状态 |
| `sys_dict_data` | 字典选项的显示文本、存储值和排序 |

### 会话与日志

| 表 | 用途 |
|---|---|
| `sys_login_log` | 登录成功、失败、退出记录 |
| `sys_oper_log` | 后端操作审计、请求参数、耗时和异常 |
| `sys_user_session` | 登录会话、过期时间、最后活跃时间、撤销状态 |
| `sys_token_blacklist` | 已撤销 access token 的 jti 与原过期时间 |

### 定时任务

| 表 | 用途 |
|---|---|
| `sys_job` | cron、Handler、参数、失火策略和启停；RAMJobStore 的恢复真源 |
| `sys_job_log` | 任务开始/结束时间、耗时、结果和异常 |

Quartz 使用 RAMJobStore 时不需要 `QRTZ_*` 内部表。

### 通知与文件

| 表 | 用途 |
|---|---|
| `sys_notice` | 通知/待办内容、发布对象、提醒和模板 |
| `sys_message` | 用户消息、已读、完成和提醒状态 |
| `sys_notification_preference` | 用户免打扰时段与时区 |
| `sys_file` | 文件元数据、存储 key、大小、SHA-256、上传人 |
| `sys_notice_attachment` | 通知与文件的附件关联及排序 |

### 协作文档

| 表 | 用途 |
|---|---|
| `sys_document` | 流程图、脑图、UML、白板的内容、类型、所有者和 revision |
| `sys_document_member` | 文档成员及 READ/WRITE 权限 |
| `sys_document_operation` | 文档操作记录、revision、操作人和操作类型 |

## 初始化原则

- SUPER_ADMIN 初始化平台菜单和按钮；
- 普通角色不因为拥有列表权限而自动获得增删改权限；
- 应用访问权限由 `sys_app.access_permission` 对应菜单 BUTTON，再由管理员分配；
- admin、SUPER_ADMIN、总公司为内置只读对象，前后端均禁止变更；
- 业务模块复用平台用户、角色和菜单，不重复建设一套账号体系。
