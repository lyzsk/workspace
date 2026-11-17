BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'system') THEN
        CREATE SCHEMA system;
    END IF;
END $$;

-- "system".sys_tenant definition



CREATE TABLE IF NOT EXISTS "system".sys_tenant ( id int8 NOT NULL, code varchar(50) NOT NULL, "name" varchar(100) NOT NULL, status int2 DEFAULT 1 NOT NULL, expire_time timestamp(6) NULL, max_users int4 NULL, contact varchar(100) NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_tenant_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_tenant_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_tenant_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_tenant_code ON system.sys_tenant USING btree (code) WHERE (is_deleted = 0);

COMMENT ON TABLE "system".sys_tenant IS '租户清单';

-- Column comments

COMMENT ON COLUMN "system".sys_tenant.code IS '租户编码，全局唯一';
COMMENT ON COLUMN "system".sys_tenant.status IS '1=启用，0=停用';
COMMENT ON COLUMN "system".sys_tenant.is_deleted IS '0=未删除，1=已删除';


-- "system".sys_app definition



CREATE TABLE IF NOT EXISTS "system".sys_app ( id int8 NOT NULL, code varchar(50) NOT NULL, "name" varchar(100) NOT NULL, icon varchar(100) NULL, base_path varchar(100) NOT NULL, module_entry varchar(500) NOT NULL, style_entry varchar(500) NULL, access_permission varchar(100) NOT NULL, sort_no int4 DEFAULT 0 NOT NULL, status int2 DEFAULT 1 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_app_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_app_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_app_pkey PRIMARY KEY (id));
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_app_code ON system.sys_app USING btree (code) WHERE (is_deleted = 0);
COMMENT ON TABLE "system".sys_app IS '应用注册表：一级应用栏';

-- Column comments

COMMENT ON COLUMN "system".sys_app.code IS '应用编码';
COMMENT ON COLUMN "system".sys_app.base_path IS '前端路由前缀，如 /apps/ies';
COMMENT ON COLUMN "system".sys_app.module_entry IS '微前端 ES module 入口';
COMMENT ON COLUMN "system".sys_app.style_entry IS '微前端样式入口';
COMMENT ON COLUMN "system".sys_app.access_permission IS '应用访问权限码';

-- "system".sys_tenant_app definition



CREATE TABLE IF NOT EXISTS "system".sys_tenant_app ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, app_id int8 NOT NULL, status int2 DEFAULT 1 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_tenant_app_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_tenant_app_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_tenant_app_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_tenant_app ON system.sys_tenant_app USING btree (tenant_id, app_id) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_tenant_app_app ON system.sys_tenant_app USING btree (app_id);

-- Column comments

COMMENT ON COLUMN "system".sys_tenant_app.tenant_id IS '租户 ID';
COMMENT ON COLUMN "system".sys_tenant_app.app_id IS 'system.sys_app.id';


-- "system".sys_dept definition



CREATE TABLE IF NOT EXISTS "system".sys_dept ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, dept_code varchar(50) NOT NULL, dept_name varchar(100) NOT NULL, parent_id int8 DEFAULT 0 NOT NULL, sort_no int4 DEFAULT 0 NOT NULL, status int2 DEFAULT 1 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_dept_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_dept_parent CHECK ((parent_id <> id)), CONSTRAINT ck_sys_dept_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_dept_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dept_code ON system.sys_dept USING btree (tenant_id, dept_code) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_dept_tree ON system.sys_dept USING btree (tenant_id, parent_id, sort_no);

-- Column comments

COMMENT ON COLUMN "system".sys_dept.dept_code IS '部门编码，同一租户内唯一';
COMMENT ON COLUMN "system".sys_dept.parent_id IS '上级部门；0=根';
COMMENT ON COLUMN "system".sys_dept.status IS '1=启用，0=停用';


-- "system".sys_user definition



CREATE TABLE IF NOT EXISTS "system".sys_user ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, username varchar(50) NOT NULL, password varchar(100) NOT NULL, real_name varchar(50) NOT NULL, gender int2 DEFAULT 0 NOT NULL, email varchar(100) NULL, mobile varchar(20) NULL, avatar varchar(500) NULL, dept_id int8 NULL, status int2 DEFAULT 1 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, token_version int8 DEFAULT 0 NOT NULL, login_fail_count int2 DEFAULT 0 NOT NULL, locked_until timestamp(6) NULL, CONSTRAINT ck_sys_user_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_user_gender CHECK ((gender = ANY (ARRAY[0, 1, 2]))), CONSTRAINT ck_sys_user_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_user_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON system.sys_user USING btree (tenant_id, username) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_user_dept ON system.sys_user USING btree (tenant_id, dept_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_mobile ON system.sys_user USING btree (tenant_id, mobile);
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON system.sys_user USING btree (tenant_id, status);

-- Column comments

COMMENT ON COLUMN "system".sys_user.tenant_id IS '租户 ID';
COMMENT ON COLUMN "system".sys_user.username IS '登录账号，拼音';
COMMENT ON COLUMN "system".sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN "system".sys_user.gender IS '0=未知，1=男，2=女';
COMMENT ON COLUMN "system".sys_user.dept_id IS '部门 ID';
COMMENT ON COLUMN "system".sys_user.status IS '1=启用，0=停用';
COMMENT ON COLUMN "system".sys_user.token_version IS 'token 版本，改密/停用/删除后 +1，旧 token 全部失效';
COMMENT ON COLUMN "system".sys_user.login_fail_count IS '连续登录失败次数';
COMMENT ON COLUMN "system".sys_user.locked_until IS '锁定截止时间，NULL=未锁定';


-- "system".sys_role definition



CREATE TABLE IF NOT EXISTS "system".sys_role ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, role_code varchar(50) NOT NULL, role_name varchar(100) NOT NULL, data_scope int2 DEFAULT 5 NOT NULL, role_sort int4 DEFAULT 0 NOT NULL, menu_check_strictly boolean DEFAULT true NOT NULL, status int2 DEFAULT 1 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_role_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_role_scope CHECK ((data_scope = ANY (ARRAY[1, 2, 3, 4, 5]))), CONSTRAINT ck_sys_role_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_role_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_code ON system.sys_role USING btree (tenant_id, role_code) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_role_status ON system.sys_role USING btree (tenant_id, status);

-- Column comments

COMMENT ON COLUMN "system".sys_role.role_code IS '角色编码，同一租户内唯一';
COMMENT ON COLUMN "system".sys_role.data_scope IS '1=全部，2=指定部门，3=本部门，4=本部门及以下，5=仅本人';
COMMENT ON COLUMN "system".sys_role.role_sort IS '角色显示顺序';
COMMENT ON COLUMN "system".sys_role.menu_check_strictly IS 'true=菜单父子联动，false=独立勾选';
COMMENT ON COLUMN "system".sys_role.status IS '1=启用，0=停用';


-- "system".sys_role_dept definition



CREATE TABLE IF NOT EXISTS "system".sys_role_dept ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, role_id int8 NOT NULL, dept_id int8 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_role_dept_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT sys_role_dept_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_dept ON system.sys_role_dept USING btree (tenant_id, role_id, dept_id) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_role_dept_dept ON system.sys_role_dept USING btree (tenant_id, dept_id);

-- Column comments

COMMENT ON COLUMN "system".sys_role_dept.tenant_id IS '租户 ID';
COMMENT ON COLUMN "system".sys_role_dept.role_id IS 'system.sys_role.id';
COMMENT ON COLUMN "system".sys_role_dept.dept_id IS 'system.sys_dept.id';


-- "system".sys_user_role definition



CREATE TABLE IF NOT EXISTS "system".sys_user_role ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, user_id int8 NOT NULL, role_id int8 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_user_role_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT sys_user_role_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_role ON system.sys_user_role USING btree (tenant_id, user_id, role_id) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role ON system.sys_user_role USING btree (tenant_id, role_id);

-- Column comments

COMMENT ON COLUMN "system".sys_user_role.user_id IS 'system.sys_user.id';
COMMENT ON COLUMN "system".sys_user_role.role_id IS 'system.sys_role.id';


-- "system".sys_menu definition



CREATE TABLE IF NOT EXISTS "system".sys_menu ( id int8 NOT NULL, app_code varchar(50) DEFAULT 'platform'::character varying NOT NULL, menu_code varchar(100) NOT NULL, parent_id int8 DEFAULT 0 NOT NULL, menu_name varchar(100) NOT NULL, menu_type varchar(20) DEFAULT 'MENU'::character varying NOT NULL, path varchar(200) NULL, component varchar(200) NULL, permission varchar(100) NULL, icon varchar(100) NULL, sort_no int4 DEFAULT 0 NOT NULL, hidden int2 DEFAULT 1 NOT NULL, status int2 DEFAULT 1 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_menu_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_menu_hidden CHECK ((hidden = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_menu_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_menu_type CHECK (((menu_type)::text = ANY ((ARRAY['DIR'::character varying, 'MENU'::character varying, 'BUTTON'::character varying])::text[]))), CONSTRAINT sys_menu_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_menu_code ON system.sys_menu USING btree (app_code, menu_code) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_menu_tree ON system.sys_menu USING btree (app_code, parent_id, sort_no);
CREATE INDEX IF NOT EXISTS idx_sys_menu_permission ON system.sys_menu USING btree (permission);

-- Column comments

COMMENT ON COLUMN "system".sys_menu.app_code IS '所属应用';
COMMENT ON COLUMN "system".sys_menu.menu_code IS '菜单编码，同一应用内唯一';
COMMENT ON COLUMN "system".sys_menu.menu_type IS 'DIR=目录，MENU=页面，BUTTON=按钮';
COMMENT ON COLUMN "system".sys_menu.component IS '前端组件相对路径，如 system/user/index';
COMMENT ON COLUMN "system".sys_menu.permission IS '权限码，如 sys:user:create';
COMMENT ON COLUMN "system".sys_menu.hidden IS '0=隐藏，1=显示';


-- "system".sys_role_menu definition



CREATE TABLE IF NOT EXISTS "system".sys_role_menu ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, role_id int8 NOT NULL, menu_id int8 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_role_menu_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT sys_role_menu_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_menu ON system.sys_role_menu USING btree (tenant_id, role_id, menu_id) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_menu ON system.sys_role_menu USING btree (tenant_id, menu_id);

-- Column comments

COMMENT ON COLUMN "system".sys_role_menu.role_id IS 'system.sys_role.id';
COMMENT ON COLUMN "system".sys_role_menu.menu_id IS 'system.sys_menu.id';


-- "system".sys_dict_type definition



CREATE TABLE IF NOT EXISTS "system".sys_dict_type ( id int8 NOT NULL, dict_type varchar(100) NOT NULL, dict_name varchar(100) NOT NULL, status int2 DEFAULT 1 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_dict_type_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_dict_type_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_dict_type_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dict_type ON system.sys_dict_type USING btree (dict_type) WHERE (is_deleted = 0);

-- Column comments

COMMENT ON COLUMN "system".sys_dict_type.dict_type IS '字典类型编码，全局唯一，如 sys_user_status';
COMMENT ON COLUMN "system".sys_dict_type.dict_name IS '字典类型名称';


-- "system".sys_dict_data definition



CREATE TABLE IF NOT EXISTS "system".sys_dict_data ( id int8 NOT NULL, dict_type varchar(100) NOT NULL, label varchar(100) NOT NULL, value varchar(100) NOT NULL, sort_no int4 DEFAULT 0 NOT NULL, status int2 DEFAULT 1 NOT NULL, create_by int8 NULL, create_time timestamp(6) DEFAULT now() NOT NULL, update_by int8 NULL, update_time timestamp(6) NULL, is_deleted int2 DEFAULT 0 NOT NULL, deleted_by int8 NULL, deleted_time timestamp(6) NULL, remark varchar(500) NULL, CONSTRAINT ck_sys_dict_data_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))), CONSTRAINT ck_sys_dict_data_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_dict_data_pkey PRIMARY KEY (id) );
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dict_data ON system.sys_dict_data USING btree (dict_type, value) WHERE (is_deleted = 0);
CREATE INDEX IF NOT EXISTS idx_sys_dict_data_type ON system.sys_dict_data USING btree (dict_type, sort_no);

-- Column comments

COMMENT ON COLUMN "system".sys_dict_data.dict_type IS '字典类型编码，对应 sys_dict_type.dict_type';
COMMENT ON COLUMN "system".sys_dict_data.label IS '显示文本';
COMMENT ON COLUMN "system".sys_dict_data.value IS '存储值，同一字典类型内唯一';


-- "system".sys_login_log definition



CREATE TABLE IF NOT EXISTS "system".sys_login_log ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, user_id int8 NULL, username varchar(50) NULL, login_type varchar(20) DEFAULT 'PASSWORD'::character varying NOT NULL, status int2 DEFAULT 1 NOT NULL, message varchar(500) NULL, ip varchar(64) NULL, user_agent varchar(500) NULL, login_time timestamp(6) DEFAULT now() NOT NULL, CONSTRAINT ck_sys_login_log_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_login_log_pkey PRIMARY KEY (id) );
CREATE INDEX IF NOT EXISTS idx_sys_login_log_time ON system.sys_login_log USING btree (tenant_id, login_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_login_log_user ON system.sys_login_log USING btree (tenant_id, user_id, login_time DESC);

-- Column comments

COMMENT ON COLUMN "system".sys_login_log.status IS '1=成功，0=失败';
COMMENT ON COLUMN "system".sys_login_log.login_type IS 'PASSWORD/SMS/AD 等';


-- "system".sys_oper_log definition



CREATE TABLE IF NOT EXISTS "system".sys_oper_log ( id int8 NOT NULL, tenant_id int8 DEFAULT 0 NOT NULL, user_id int8 NULL, username varchar(50) NULL, module varchar(100) NULL, operation varchar(200) NULL, request_method varchar(10) NULL, request_uri varchar(500) NULL, request_params text NULL, response_result text NULL, status int2 DEFAULT 1 NOT NULL, error_msg varchar(2000) NULL, cost_time int8 NULL, ip varchar(64) NULL, oper_time timestamp(6) DEFAULT now() NOT NULL, CONSTRAINT ck_sys_oper_log_status CHECK ((status = ANY (ARRAY[0, 1]))), CONSTRAINT sys_oper_log_pkey PRIMARY KEY (id) );
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_time ON system.sys_oper_log USING btree (tenant_id, oper_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_user ON system.sys_oper_log USING btree (tenant_id, user_id, oper_time DESC);

COMMENT ON TABLE "system".sys_oper_log IS '操作日志';

-- Column comments

COMMENT ON COLUMN "system".sys_oper_log.status IS '1=成功，0=失败';
COMMENT ON COLUMN "system".sys_oper_log.cost_time IS '耗时，毫秒';


-- "system".sys_user_session definition
-- 在线会话表：登录写入，登出/强制下线撤销

CREATE TABLE IF NOT EXISTS "system".sys_user_session (
    id             int8 NOT NULL,
    tenant_id      int8 DEFAULT 0 NOT NULL,
    user_id        int8 NOT NULL,
    jti            varchar(64) NOT NULL,
    login_time     timestamp(6) DEFAULT now() NOT NULL,
    last_seen_at   timestamp(6) DEFAULT now() NOT NULL,
    expires_at     timestamp(6) NOT NULL,
    ip             varchar(64) NULL,
    user_agent     varchar(500) NULL,
    revoked        int2 DEFAULT 0 NOT NULL,
    revoked_at     timestamp(6) NULL,
    CONSTRAINT ck_sys_user_session_revoked CHECK ((revoked = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_user_session_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_session_jti
    ON system.sys_user_session USING btree (jti);
CREATE INDEX IF NOT EXISTS idx_sys_user_session_user
    ON system.sys_user_session USING btree (tenant_id, user_id, expires_at DESC);

COMMENT ON TABLE "system".sys_user_session IS '在线会话表：登录写入，登出/强制下线撤销';
COMMENT ON COLUMN "system".sys_user_session.user_id IS 'system.sys_user.id';
COMMENT ON COLUMN "system".sys_user_session.jti IS 'access token 的 JWT 唯一标识';
COMMENT ON COLUMN "system".sys_user_session.expires_at IS '会话过期时间，与 access token 一致';
COMMENT ON COLUMN "system".sys_user_session.revoked IS '0=有效，1=已撤销';


-- "system".sys_token_blacklist definition
-- access token 黑名单：用于退出登录/强制下线后的即时失效

CREATE TABLE IF NOT EXISTS "system".sys_token_blacklist (
    id          int8 NOT NULL,
    jti         varchar(64) NOT NULL,
    expires_at  timestamp(6) NOT NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    CONSTRAINT sys_token_blacklist_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_token_blacklist_jti
    ON system.sys_token_blacklist USING btree (jti);
CREATE INDEX IF NOT EXISTS idx_sys_token_blacklist_expires
    ON system.sys_token_blacklist USING btree (expires_at);

COMMENT ON COLUMN "system".sys_token_blacklist.jti IS '被撤销的 access token jti';
COMMENT ON COLUMN "system".sys_token_blacklist.expires_at IS '原 token 过期时间，过期后可清理';


CREATE TABLE IF NOT EXISTS "system".sys_job (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    job_name varchar(100) NOT NULL,
    job_group varchar(50) DEFAULT 'SYSTEM' NOT NULL,
    cron_expression varchar(100) NOT NULL,
    handler_name varchar(100) NOT NULL,
    params text NULL,
    misfire_policy int2 DEFAULT 1 NOT NULL,
    status int2 DEFAULT 0 NOT NULL,
    create_by int8 NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by int8 NULL,
    update_time timestamp(6) NULL,
    is_deleted int2 DEFAULT 0 NOT NULL,
    deleted_by int8 NULL,
    deleted_time timestamp(6) NULL,
    remark varchar(500) NULL,
    CONSTRAINT ck_sys_job_status CHECK ((status = ANY (ARRAY[0, 1]))),
    CONSTRAINT ck_sys_job_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_job_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_job_name ON system.sys_job USING btree (tenant_id, job_group, job_name) WHERE (is_deleted = 0);


CREATE TABLE IF NOT EXISTS "system".sys_job_log (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    job_id int8 NULL,
    job_name varchar(100) NULL,
    handler_name varchar(100) NULL,
    params text NULL,
    status int2 DEFAULT 1 NOT NULL,
    error_msg varchar(2000) NULL,
    cost_time int8 NULL,
    start_time timestamp(6) DEFAULT now() NOT NULL,
    end_time timestamp(6) NULL,
    CONSTRAINT ck_sys_job_log_status CHECK ((status = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_job_log_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sys_job_log_job ON system.sys_job_log USING btree (tenant_id, job_id, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_job_log_time ON system.sys_job_log USING btree (tenant_id, start_time DESC);


-- 已有库增量：token 版本 + 登录失败锁定字段
CREATE TABLE IF NOT EXISTS "system".sys_notice (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    title varchar(200) NOT NULL,
    content text NOT NULL,
    notice_type varchar(20) DEFAULT 'TODO' NOT NULL,
    priority int2 DEFAULT 0 NOT NULL,
    target_type varchar(20) DEFAULT 'ALL' NOT NULL,
    target_ids text DEFAULT '[]' NOT NULL,
    status int2 DEFAULT 0 NOT NULL,
    publisher_id int8 NULL,
    publish_time timestamp(6) NULL,
    remind_time timestamp(6) NULL,
    expire_time timestamp(6) NULL,
    create_by int8 NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by int8 NULL,
    update_time timestamp(6) NULL,
    is_deleted int2 DEFAULT 0 NOT NULL,
    deleted_by int8 NULL,
    deleted_time timestamp(6) NULL,
    remark varchar(500) NULL,
    CONSTRAINT ck_sys_notice_type CHECK ((notice_type = ANY (ARRAY['TODO', 'ANNOUNCEMENT']))),
    CONSTRAINT ck_sys_notice_priority CHECK ((priority = ANY (ARRAY[0, 1, 2]))),
    CONSTRAINT ck_sys_notice_target CHECK ((target_type = ANY (ARRAY['ALL', 'ROLE', 'DEPT']))),
    CONSTRAINT ck_sys_notice_status CHECK ((status = ANY (ARRAY[0, 1, 2]))),
    CONSTRAINT ck_sys_notice_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_notice_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sys_notice_status_time
    ON system.sys_notice USING btree (tenant_id, status, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_notice_remind
    ON system.sys_notice USING btree (tenant_id, status, remind_time)
    WHERE is_deleted = 0 AND remind_time IS NOT NULL;

CREATE TABLE IF NOT EXISTS "system".sys_message (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    notice_id int8 NOT NULL,
    receiver_id int8 NOT NULL,
    category varchar(20) DEFAULT 'TODO' NOT NULL,
    level int2 DEFAULT 0 NOT NULL,
    link_url varchar(500) NULL,
    read_flag int2 DEFAULT 0 NOT NULL,
    read_time timestamp(6) NULL,
    done_flag int2 DEFAULT 0 NOT NULL,
    done_time timestamp(6) NULL,
    due_time timestamp(6) NULL,
    next_remind_time timestamp(6) NULL,
    last_remind_time timestamp(6) NULL,
    remind_count int4 DEFAULT 0 NOT NULL,
    create_by int8 NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by int8 NULL,
    update_time timestamp(6) NULL,
    is_deleted int2 DEFAULT 0 NOT NULL,
    deleted_by int8 NULL,
    deleted_time timestamp(6) NULL,
    remark varchar(500) NULL,
    CONSTRAINT ck_sys_message_category CHECK ((category = ANY (ARRAY['TODO', 'ANNOUNCEMENT', 'SYSTEM']))),
    CONSTRAINT ck_sys_message_level CHECK ((level = ANY (ARRAY[0, 1, 2]))),
    CONSTRAINT ck_sys_message_read CHECK ((read_flag = ANY (ARRAY[0, 1]))),
    CONSTRAINT ck_sys_message_done CHECK ((done_flag = ANY (ARRAY[0, 1]))),
    CONSTRAINT ck_sys_message_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_message_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_message_receiver
    ON system.sys_message USING btree (tenant_id, notice_id, receiver_id)
    WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_message_unread
    ON system.sys_message USING btree (tenant_id, receiver_id, read_flag, create_time DESC)
    WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_message_remind
    ON system.sys_message USING btree (next_remind_time, receiver_id)
    WHERE is_deleted = 0 AND read_flag = 0 AND next_remind_time IS NOT NULL;

ALTER TABLE system.sys_notice
    ADD COLUMN IF NOT EXISTS repeat_interval_seconds int8 DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS max_remind_count int4 DEFAULT 1 NOT NULL,
    ADD COLUMN IF NOT EXISTS template_code varchar(100) NULL,
    ADD COLUMN IF NOT EXISTS template_params text DEFAULT '{}' NOT NULL;

ALTER TABLE system.sys_notice DROP CONSTRAINT IF EXISTS ck_sys_notice_type;
ALTER TABLE system.sys_notice
    ADD CONSTRAINT ck_sys_notice_type CHECK ((notice_type = ANY (ARRAY['TODO', 'ANNOUNCEMENT', 'MESSAGE'])));
ALTER TABLE system.sys_message DROP CONSTRAINT IF EXISTS ck_sys_message_category;
ALTER TABLE system.sys_message
    ADD CONSTRAINT ck_sys_message_category CHECK ((category = ANY (ARRAY['TODO', 'ANNOUNCEMENT', 'MESSAGE', 'SYSTEM'])));

CREATE TABLE IF NOT EXISTS system.sys_notification_preference (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    user_id int8 NOT NULL,
    quiet_enabled int2 DEFAULT 0 NOT NULL,
    quiet_start time NULL,
    quiet_end time NULL,
    timezone varchar(64) DEFAULT 'Asia/Shanghai' NOT NULL,
    create_by int8 NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by int8 NULL,
    update_time timestamp(6) NULL,
    is_deleted int2 DEFAULT 0 NOT NULL,
    deleted_by int8 NULL,
    deleted_time timestamp(6) NULL,
    remark varchar(500) NULL,
    CONSTRAINT ck_sys_notification_pref_quiet CHECK ((quiet_enabled = ANY (ARRAY[0, 1]))),
    CONSTRAINT ck_sys_notification_pref_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_notification_preference_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_notification_preference_user
    ON system.sys_notification_preference USING btree (tenant_id, user_id)
    WHERE is_deleted = 0;

CREATE TABLE IF NOT EXISTS system.sys_file (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    original_name varchar(255) NOT NULL,
    stored_name varchar(255) NOT NULL,
    storage_key varchar(500) NOT NULL,
    content_type varchar(150) NULL,
    size_bytes int8 NOT NULL,
    sha256 varchar(64) NOT NULL,
    uploader_id int8 NULL,
    create_by int8 NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by int8 NULL,
    update_time timestamp(6) NULL,
    is_deleted int2 DEFAULT 0 NOT NULL,
    deleted_by int8 NULL,
    deleted_time timestamp(6) NULL,
    remark varchar(500) NULL,
    CONSTRAINT ck_sys_file_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_file_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_file_storage_key ON system.sys_file USING btree (storage_key)
    WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_file_uploader ON system.sys_file USING btree (tenant_id, uploader_id, create_time DESC);

CREATE TABLE IF NOT EXISTS system.sys_notice_attachment (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    notice_id int8 NOT NULL,
    file_id int8 NOT NULL,
    sort_no int4 DEFAULT 0 NOT NULL,
    create_by int8 NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by int8 NULL,
    update_time timestamp(6) NULL,
    is_deleted int2 DEFAULT 0 NOT NULL,
    deleted_by int8 NULL,
    deleted_time timestamp(6) NULL,
    remark varchar(500) NULL,
    CONSTRAINT ck_sys_notice_attachment_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_notice_attachment_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_notice_attachment_file
    ON system.sys_notice_attachment USING btree (tenant_id, notice_id, file_id)
    WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_notice_attachment_notice
    ON system.sys_notice_attachment USING btree (tenant_id, notice_id)
    WHERE is_deleted = 0;

ALTER TABLE "system".sys_user
    ADD COLUMN IF NOT EXISTS token_version int8 DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS login_fail_count int2 DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS locked_until timestamp(6) NULL;

COMMENT ON COLUMN "system".sys_user.token_version IS 'token 版本，改密/停用/删除后 +1，旧 token 全部失效';
COMMENT ON COLUMN "system".sys_user.login_fail_count IS '连续登录失败次数';
COMMENT ON COLUMN "system".sys_user.locked_until IS '锁定截止时间，NULL=未锁定';


CREATE TABLE IF NOT EXISTS system.sys_document (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    title varchar(200) NOT NULL,
    doc_type varchar(20) NOT NULL,
    owner_id int8 NOT NULL,
    content text NOT NULL,
    revision int8 DEFAULT 0 NOT NULL,
    create_by int8 NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by int8 NULL,
    update_time timestamp(6) NULL,
    is_deleted int2 DEFAULT 0 NOT NULL,
    deleted_by int8 NULL,
    deleted_time timestamp(6) NULL,
    remark varchar(500) NULL,
    CONSTRAINT ck_sys_document_type CHECK ((doc_type = ANY (ARRAY['FLOW', 'MIND', 'UML', 'WHITEBOARD']))),
    CONSTRAINT ck_sys_document_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_document_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sys_document_owner ON system.sys_document USING btree (tenant_id, owner_id, update_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_document_type ON system.sys_document USING btree (tenant_id, doc_type, update_time DESC);

CREATE TABLE IF NOT EXISTS system.sys_document_member (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    document_id int8 NOT NULL,
    user_id int8 NOT NULL,
    permission varchar(10) NOT NULL,
    create_by int8 NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by int8 NULL,
    update_time timestamp(6) NULL,
    is_deleted int2 DEFAULT 0 NOT NULL,
    deleted_by int8 NULL,
    deleted_time timestamp(6) NULL,
    remark varchar(500) NULL,
    CONSTRAINT ck_sys_document_member_permission CHECK ((permission = ANY (ARRAY['READ', 'WRITE']))),
    CONSTRAINT ck_sys_document_member_deleted CHECK ((is_deleted = ANY (ARRAY[0, 1]))),
    CONSTRAINT sys_document_member_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_document_member ON system.sys_document_member USING btree (tenant_id, document_id, user_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_document_member_user ON system.sys_document_member USING btree (tenant_id, user_id, document_id) WHERE is_deleted = 0;

CREATE TABLE IF NOT EXISTS system.sys_document_operation (
    id int8 NOT NULL,
    tenant_id int8 DEFAULT 0 NOT NULL,
    document_id int8 NOT NULL,
    revision int8 NOT NULL,
    op_type varchar(30) NOT NULL,
    target_type varchar(20) NULL,
    target_id varchar(100) NULL,
    payload text NULL,
    operator_id int8 NOT NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    CONSTRAINT ck_sys_document_operation_type CHECK ((op_type = ANY (ARRAY['UPSERT', 'DELETE', 'SCENE_REPLACE']))),
    CONSTRAINT sys_document_operation_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sys_document_operation_revision ON system.sys_document_operation USING btree (tenant_id, document_id, revision);

-- 默认公司/租户
INSERT INTO system.sys_tenant (id, code, name, status, create_time)
VALUES (0, 'default', '默认租户', 1, now())
ON CONFLICT (id) DO NOTHING;

-- 本地登录账号由 sql/seed_user.sql 单独初始化，不进入结构基线


-- 默认部门（可重复执行）
INSERT INTO system.sys_dept
    (id, tenant_id, dept_code, dept_name, parent_id, sort_no, status, create_time)
VALUES
    (1, 0, 'HQ', '总公司', 0, 0, 1, now()),
    (682170091831296, 0, 'DATACENTER', '数据中心', 1, 0, 1, now())
ON CONFLICT DO NOTHING;


-- 基础字典（可重复执行）

INSERT INTO system.sys_dict_type (id, dict_type, dict_name, status) VALUES
    (1, 'sys_user_gender', '用户性别', 1),
    (2, 'sys_user_status', '用户状态', 1),
    (3, 'sys_menu_type', '菜单类型', 1),
    (4, 'sys_log_status', '日志状态', 1),
    (5, 'sys_common_status', '通用状态', 1)
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_dict_data (id, dict_type, label, value, sort_no, status) VALUES
    (1, 'sys_user_gender', '未知', '0', 1, 1),
    (2, 'sys_user_gender', '男', '1', 2, 1),
    (3, 'sys_user_gender', '女', '2', 3, 1),
    (4, 'sys_user_status', '停用', '0', 1, 1),
    (5, 'sys_user_status', '启用', '1', 2, 1),
    (6, 'sys_menu_type', '目录', 'DIR', 1, 1),
    (7, 'sys_menu_type', '菜单', 'MENU', 2, 1),
    (8, 'sys_menu_type', '按钮', 'BUTTON', 3, 1),
    (9, 'sys_log_status', '失败', '0', 1, 1),
    (10, 'sys_log_status', '成功', '1', 2, 1),
    (11, 'sys_common_status', '停用', '0', 1, 1),
    (12, 'sys_common_status', '启用', '1', 2, 1)
ON CONFLICT DO NOTHING;


-- 微前端应用配置
INSERT INTO system.sys_app
    (id, code, name, icon, base_path, module_entry, style_entry, access_permission,
     sort_no, status, create_time)
VALUES
    (3001, 'eap-tool-service', 'IES（设备信息维护系统）', 'ri:server-line',
     '/apps/eap-tool-service', '/eap/src/micro.ts', NULL, 'eap-tool-service:access', 10, 1, now())
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_role
    (id, tenant_id, role_code, role_name, data_scope, role_sort, menu_check_strictly,
     status, create_time)
VALUES
    (1, 0, 'SUPER_ADMIN', '超级管理员', 1, 0, true, 1, now()),
    (2, 0, 'GENERAL', '普通用户', 5, 1, true, 1, now())
ON CONFLICT DO NOTHING;


-- 系统管理完整菜单/按钮（可重复执行）
-- SUPER_ADMIN 在代码里直接放行所有菜单与权限码；下面的 role_menu 只是让角色分配界面显示已勾选。

INSERT INTO system.sys_menu
    (id, app_code, menu_code, parent_id, menu_name, menu_type, path, component,
     permission, icon, sort_no, hidden, status, create_time)
VALUES
    (100, 'workspace', 'system', 0, '系统管理', 'DIR', '/system', NULL, NULL, 'ri:settings-3-line', 10, 1, 1, now()),
    (101, 'workspace', 'system:user', 100, '用户管理', 'MENU', '/system/user',
     'system/user/index', 'sys:user:list', 'ri:user-line', 1, 1, 1, now()),
    (102, 'workspace', 'system:user:list', 101, '用户查询', 'BUTTON', NULL, NULL,
     'sys:user:list', NULL, 1, 1, 1, now()),
    (103, 'workspace', 'system:user:create', 101, '用户新增', 'BUTTON', NULL, NULL,
     'sys:user:create', NULL, 2, 1, 1, now()),
    (104, 'workspace', 'system:user:update', 101, '用户修改', 'BUTTON', NULL, NULL,
     'sys:user:update', NULL, 3, 1, 1, now()),
    (105, 'workspace', 'system:user:delete', 101, '用户删除', 'BUTTON', NULL, NULL,
     'sys:user:delete', NULL, 4, 1, 1, now()),

    (110, 'workspace', 'system:role', 100, '角色管理', 'MENU', '/system/role',
     'system/role/index', 'sys:role:list', 'ri:team-line', 2, 1, 1, now()),
    (111, 'workspace', 'system:role:create', 110, '角色新增', 'BUTTON', NULL, NULL,
     'sys:role:create', NULL, 1, 1, 1, now()),
    (112, 'workspace', 'system:role:update', 110, '角色修改', 'BUTTON', NULL, NULL,
     'sys:role:update', NULL, 2, 1, 1, now()),
    (113, 'workspace', 'system:role:delete', 110, '角色删除', 'BUTTON', NULL, NULL,
     'sys:role:delete', NULL, 3, 1, 1, now()),
    (114, 'workspace', 'system:role:assign', 110, '分配权限', 'BUTTON', NULL, NULL,
     'sys:role:assign', NULL, 4, 1, 1, now()),

    (120, 'workspace', 'system:menu', 100, '菜单管理', 'MENU', '/system/menu',
     'system/menu/index', 'sys:menu:list', 'ri:menu-line', 3, 1, 1, now()),
    (121, 'workspace', 'system:menu:create', 120, '菜单新增', 'BUTTON', NULL, NULL,
     'sys:menu:create', NULL, 1, 1, 1, now()),
    (122, 'workspace', 'system:menu:update', 120, '菜单修改', 'BUTTON', NULL, NULL,
     'sys:menu:update', NULL, 2, 1, 1, now()),
    (123, 'workspace', 'system:menu:delete', 120, '菜单删除', 'BUTTON', NULL, NULL,
     'sys:menu:delete', NULL, 3, 1, 1, now()),

    (130, 'workspace', 'system:dept', 100, '部门管理', 'MENU', '/system/dept',
     'system/dept/index', 'sys:dept:list', 'ri:building-line', 4, 1, 1, now()),
    (131, 'workspace', 'system:dept:create', 130, '部门新增', 'BUTTON', NULL, NULL,
     'sys:dept:create', NULL, 1, 1, 1, now()),
    (132, 'workspace', 'system:dept:update', 130, '部门修改', 'BUTTON', NULL, NULL,
     'sys:dept:update', NULL, 2, 1, 1, now()),
    (133, 'workspace', 'system:dept:delete', 130, '部门删除', 'BUTTON', NULL, NULL,
     'sys:dept:delete', NULL, 3, 1, 1, now()),

    (140, 'workspace', 'system:dict', 100, '字典管理', 'MENU', '/system/dict',
     'system/dict/index', 'sys:dict:list', 'ri:book-2-line', 5, 1, 1, now()),
    (141, 'workspace', 'system:dict:create', 140, '字典新增', 'BUTTON', NULL, NULL,
     'sys:dict:create', NULL, 1, 1, 1, now()),
    (142, 'workspace', 'system:dict:update', 140, '字典修改', 'BUTTON', NULL, NULL,
     'sys:dict:update', NULL, 2, 1, 1, now()),
    (143, 'workspace', 'system:dict:delete', 140, '字典删除', 'BUTTON', NULL, NULL,
     'sys:dict:delete', NULL, 3, 1, 1, now()),

    (150, 'workspace', 'system:log', 100, '日志管理', 'DIR', '/system/log', NULL,
     NULL, 'ri:file-list-2-line', 6, 1, 1, now()),
    (151, 'workspace', 'system:log:login', 150, '登录日志', 'MENU', '/system/log/login',
     'system/log/login/index', 'sys:log:login:list', NULL, 1, 1, 1, now()),
    (152, 'workspace', 'system:log:oper', 150, '操作日志', 'MENU', '/system/log/oper',
     'system/log/oper/index', 'sys:log:oper:list', NULL, 2, 1, 1, now()),

    (160, 'workspace', 'monitor', 0, '系统监控', 'DIR', '/monitor', NULL, NULL,
     'ri:computer-line', 20, 1, 1, now()),
    (161, 'workspace', 'monitor:online', 160, '在线用户', 'MENU', '/monitor/online',
     'system/online/index', 'sys:online:list', 'ri:wifi-line', 1, 1, 1, now()),
    (162, 'workspace', 'monitor:online:kick', 161, '强制下线', 'BUTTON', NULL, NULL,
     'sys:online:kick', NULL, 1, 1, 1, now()),
    (163, 'workspace', 'monitor:job', 160, '定时任务', 'MENU', '/monitor/job',
     'system/job/index', 'sys:job:list', 'ri:time-line', 2, 1, 1, now()),
    (164, 'workspace', 'monitor:job:create', 163, '任务新增', 'BUTTON', NULL, NULL,
     'sys:job:create', NULL, 1, 1, 1, now()),
    (165, 'workspace', 'monitor:job:update', 163, '任务修改', 'BUTTON', NULL, NULL,
     'sys:job:update', NULL, 2, 1, 1, now()),
    (166, 'workspace', 'monitor:job:delete', 163, '任务删除', 'BUTTON', NULL, NULL,
     'sys:job:delete', NULL, 3, 1, 1, now()),
    (167, 'workspace', 'monitor:job:run', 163, '执行一次', 'BUTTON', NULL, NULL,
     'sys:job:run', NULL, 4, 1, 1, now()),
    (168, 'workspace', 'monitor:server', 160, '服务监控', 'MENU', '/monitor/server',
     'system/monitor/server/index', 'sys:monitor:server', 'ri:server-line', 3, 1, 1, now()),
    (169, 'workspace', 'monitor:cache', 160, '缓存监控', 'MENU', '/monitor/cache',
     'system/monitor/cache/index', 'sys:monitor:cache', 'ri:database-2-line', 4, 1, 1, now()),
    (170, 'workspace', 'monitor:cache:list', 160, '缓存列表', 'MENU', '/monitor/cache/list',
     'system/monitor/cache-list/index', 'sys:monitor:cache', 'ri:list-check', 5, 1, 1, now()),
    (200, 'workspace', 'apps', 0, '应用管理', 'DIR', '/apps', NULL, NULL,
     'ri:apps-line', 30, 1, 1, now()),
    (201, 'workspace', 'apps:list', 200, '应用列表', 'MENU', '/apps',
     'apps/index', NULL, 'ri:list-check', 1, 1, 1, now()),
    (203, 'workspace', 'apps:create', 201, '应用新增', 'BUTTON', NULL, NULL,
     'sys:app:create', NULL, 1, 1, 1, now()),
    (204, 'workspace', 'apps:update', 201, '应用修改', 'BUTTON', NULL, NULL,
     'sys:app:update', NULL, 2, 1, 1, now()),
    (205, 'workspace', 'apps:delete', 201, '应用删除', 'BUTTON', NULL, NULL,
     'sys:app:delete', NULL, 3, 1, 1, now()),
    (206, 'workspace', 'apps:eap-tool-service:access', 201, 'IES访问', 'BUTTON', NULL, NULL,
     'eap-tool-service:access', NULL, 4, 1, 1, now())
ON CONFLICT DO NOTHING;

-- 修正早期 seed_auth 里用户管理页的 permission/icon
UPDATE system.sys_menu
SET permission = 'sys:user:list', component = 'system/user/index', icon = 'ri:user-line'
WHERE id = 101 AND is_deleted = 0;

UPDATE system.sys_menu SET icon = 'ri:settings-3-line' WHERE id = 100 AND is_deleted = 0;

INSERT INTO system.sys_menu
    (id, app_code, menu_code, parent_id, menu_name, menu_type, path, component,
     permission, icon, sort_no, hidden, status, create_time)
VALUES
    (171, 'workspace', 'system:notice', 100, '通知管理', 'MENU', '/system/notice',
     'system/notice/index', 'system:notice:list', 'ri:notification-3-line', 7, 1, 1, now()),
    (172, 'workspace', 'system:notice:create', 171, '通知新增', 'BUTTON', NULL, NULL,
     'system:notice:create', NULL, 1, 1, 1, now()),
    (173, 'workspace', 'system:notice:publish', 171, '通知发布', 'BUTTON', NULL, NULL,
     'system:notice:publish', NULL, 2, 1, 1, now())
ON CONFLICT DO NOTHING;

UPDATE system.sys_menu
SET permission = REPLACE(permission, 'sys:notice:', 'system:notice:')
WHERE permission LIKE 'sys:notice:%';

INSERT INTO system.sys_menu
    (id, app_code, menu_code, parent_id, menu_name, menu_type, path, component,
     permission, icon, sort_no, hidden, status, create_time)
VALUES
    (210, 'workspace', 'tools', 0, '通用工具', 'DIR', '/tools', NULL, NULL,
     'ri:tools-line', 50, 1, 1, now()),
    (211, 'workspace', 'tools:flow', 210, '流程图', 'MENU', '/tools/flow',
     'tools/flow/index', 'system:document:list', 'ri:flow-chart', 1, 1, 1, now()),
    (212, 'workspace', 'tools:mind', 210, '脑图', 'MENU', '/tools/mind',
     'tools/mind/index', 'system:document:list', 'ri:mind-map', 2, 1, 1, now()),
    (213, 'workspace', 'tools:uml', 210, 'UML 图', 'MENU', '/tools/uml',
     'tools/uml/index', 'system:document:list', 'ri:organization-chart', 3, 1, 1, now()),
    (214, 'workspace', 'tools:whiteboard', 210, '白板', 'MENU', '/tools/whiteboard',
     'tools/whiteboard/index', 'system:document:list', 'ri:edit-box-line', 4, 1, 1, now()),
    (215, 'workspace', 'tools:document:create', 211, '文档新增', 'BUTTON', NULL, NULL,
     'system:document:create', NULL, 1, 1, 1, now()),
    (216, 'workspace', 'tools:document:update', 211, '文档修改', 'BUTTON', NULL, NULL,
     'system:document:update', NULL, 2, 1, 1, now()),
    (217, 'workspace', 'tools:document:delete', 211, '文档删除', 'BUTTON', NULL, NULL,
     'system:document:delete', NULL, 3, 1, 1, now()),
    (218, 'workspace', 'tools:document:share', 211, '文档共享', 'BUTTON', NULL, NULL,
     'system:document:share', NULL, 4, 1, 1, now())
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_menu
    (id, app_code, menu_code, parent_id, menu_name, menu_type, path, component,
     permission, icon, sort_no, hidden, status, create_time)
VALUES
    (202, 'workspace', 'apiDoc', 0, '接口文档', 'MENU', '/api-doc', 'tools/api-doc/index',
     'system:apidoc:view', 'ri:file-code-line', 40, 1, 1, now())
ON CONFLICT DO NOTHING;

UPDATE system.sys_menu
SET app_code = 'workspace', parent_id = 0, menu_name = '接口文档', menu_type = 'MENU',
    path = '/api-doc', component = 'tools/api-doc/index', permission = 'system:apidoc:view',
    icon = 'ri:file-code-line', sort_no = 40, hidden = 1, status = 1
WHERE menu_code = 'apiDoc' AND is_deleted = 0;

-- SUPER_ADMIN 勾选全部菜单（SUPER_ADMIN 本身在代码里放行）
INSERT INTO system.sys_role_menu (id, tenant_id, role_id, menu_id, create_time)
SELECT 10000 + id, 0, 1, id, now()
FROM system.sys_menu
WHERE is_deleted = 0
  AND id BETWEEN 100 AND 299
ON CONFLICT DO NOTHING;


COMMIT;
