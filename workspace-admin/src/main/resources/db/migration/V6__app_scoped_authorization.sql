BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM system.sys_app WHERE id = 1000 AND code <> 'workspace') THEN
        RAISE EXCEPTION 'sys_app id=1000 is occupied by another application';
    END IF;
END $$;

INSERT INTO system.sys_app
    (id, code, name, icon, base_path, module_entry, style_entry, access_permission,
     sort_no, status, create_time, remark)
SELECT 1000, 'workspace', '工作台', 'ri:apps-line', '/', '', NULL, 'workspace:access',
       0, 1, now(), '平台主应用'
WHERE NOT EXISTS (SELECT 1 FROM system.sys_app WHERE code = 'workspace');

ALTER TABLE system.sys_role ADD COLUMN IF NOT EXISTS app_id int8 NULL;
UPDATE system.sys_role
SET app_id = (SELECT id FROM system.sys_app WHERE code = 'workspace' AND is_deleted = 0)
WHERE app_id IS NULL;

DROP INDEX IF EXISTS system.uk_sys_role_code;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_code
    ON system.sys_role USING btree (tenant_id, app_id, role_code)
    WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_role_app
    ON system.sys_role USING btree (app_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username_global
    ON system.sys_user USING btree (username)
    WHERE is_deleted = 0;

ALTER TABLE system.sys_menu ALTER COLUMN app_code SET DEFAULT 'workspace';
UPDATE system.sys_menu SET app_code = 'workspace' WHERE app_code = 'platform';

COMMENT ON COLUMN system.sys_role.app_id IS '所属应用 ID；workspace 为平台角色';

CREATE TABLE IF NOT EXISTS system.sys_user_app (
    id             int8 NOT NULL,
    tenant_id      int8 DEFAULT 0 NOT NULL,
    user_id        int8 NOT NULL,
    app_id         int8 NOT NULL,
    status         int2 DEFAULT 1 NOT NULL,
    data_scope     int2 NULL,
    create_by      int8 NULL,
    create_time    timestamp(6) DEFAULT now() NOT NULL,
    update_by      int8 NULL,
    update_time    timestamp(6) NULL,
    is_deleted     int2 DEFAULT 0 NOT NULL,
    deleted_by     int8 NULL,
    deleted_time   timestamp(6) NULL,
    remark         varchar(500) NULL,
    CONSTRAINT sys_user_app_pkey PRIMARY KEY (id),
    CONSTRAINT ck_sys_user_app_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_sys_user_app_deleted CHECK (is_deleted IN (0, 1)),
    CONSTRAINT ck_sys_user_app_scope CHECK (data_scope IS NULL OR data_scope IN (1, 2, 3, 4, 5))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_app
    ON system.sys_user_app USING btree (tenant_id, user_id, app_id)
    WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_user_app_access
    ON system.sys_user_app USING btree (app_id, status, user_id)
    WHERE is_deleted = 0;
COMMENT ON TABLE system.sys_user_app IS '用户—应用授权：同一用户在各应用中的启停与数据范围覆盖';
COMMENT ON COLUMN system.sys_user_app.data_scope IS 'NULL=跟随本应用角色；1=全部，2=指定部门，3=本部门，4=本部门及以下，5=仅本人';

CREATE TABLE IF NOT EXISTS system.sys_user_app_dept (
    id             int8 NOT NULL,
    tenant_id      int8 DEFAULT 0 NOT NULL,
    user_app_id    int8 NOT NULL,
    dept_id        int8 NOT NULL,
    create_by      int8 NULL,
    create_time    timestamp(6) DEFAULT now() NOT NULL,
    update_by      int8 NULL,
    update_time    timestamp(6) NULL,
    is_deleted     int2 DEFAULT 0 NOT NULL,
    deleted_by     int8 NULL,
    deleted_time   timestamp(6) NULL,
    remark         varchar(500) NULL,
    CONSTRAINT sys_user_app_dept_pkey PRIMARY KEY (id),
    CONSTRAINT ck_sys_user_app_dept_deleted CHECK (is_deleted IN (0, 1))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_app_dept
    ON system.sys_user_app_dept USING btree (tenant_id, user_app_id, dept_id)
    WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_user_app_dept_dept
    ON system.sys_user_app_dept USING btree (tenant_id, dept_id);

CREATE SEQUENCE IF NOT EXISTS system.workspace_migration_id_seq
    START WITH 1000000000000000000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

INSERT INTO system.sys_user_app
    (id, tenant_id, user_id, app_id, status, data_scope, create_time)
SELECT nextval('system.workspace_migration_id_seq'), u.tenant_id, u.id, a.id, 1, NULL::int2, now()
FROM system.sys_user u
CROSS JOIN system.sys_app a
WHERE u.is_deleted = 0
  AND a.code = 'workspace'
  AND a.is_deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_user_app
    (id, tenant_id, user_id, app_id, status, data_scope, create_time)
SELECT DISTINCT nextval('system.workspace_migration_id_seq'), u.tenant_id, u.id, a.id, 1, NULL::int2, now()
FROM system.sys_app a
JOIN system.sys_menu m ON m.permission = a.access_permission
    AND m.is_deleted = 0 AND m.status = 1
JOIN system.sys_role_menu rm ON rm.menu_id = m.id AND rm.is_deleted = 0
JOIN system.sys_role r ON r.id = rm.role_id AND r.is_deleted = 0 AND r.status = 1
JOIN system.sys_user_role ur ON ur.role_id = r.id AND ur.is_deleted = 0
JOIN system.sys_user u ON u.id = ur.user_id AND u.is_deleted = 0
WHERE a.is_deleted = 0
  AND a.code <> 'workspace'
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_user_app
    (id, tenant_id, user_id, app_id, status, data_scope, create_time)
SELECT DISTINCT nextval('system.workspace_migration_id_seq'), u.tenant_id, u.id, a.id, 1, NULL::int2, now()
FROM system.sys_user u
JOIN system.sys_user_role ur ON ur.user_id = u.id AND ur.is_deleted = 0
JOIN system.sys_role r ON r.id = ur.role_id AND r.is_deleted = 0
CROSS JOIN system.sys_app a
WHERE u.is_deleted = 0
  AND r.role_code = 'SUPER_ADMIN'
  AND a.is_deleted = 0
  AND a.code <> 'workspace'
ON CONFLICT DO NOTHING;

DROP SEQUENCE system.workspace_migration_id_seq;

INSERT INTO system.sys_menu
    (id, app_code, menu_code, parent_id, menu_name, menu_type, path, component,
     permission, icon, sort_no, hidden, status, create_time)
VALUES (106, 'workspace', 'system:user:app-grant', 101, '应用授权', 'BUTTON', NULL, NULL,
        'sys:user:app-grant', NULL, 5, 0, 1, now()),
       (107, 'workspace', 'system:user:assign', 101, '分配平台角色', 'BUTTON', NULL, NULL,
        'sys:user:assign', NULL, 6, 0, 1, now())
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_role_menu (id, tenant_id, role_id, menu_id, create_time)
SELECT 10000 + id, 0, 1, id, now()
FROM system.sys_menu
WHERE id IN (106, 107)
  AND EXISTS (SELECT 1 FROM system.sys_role WHERE id = 1 AND role_code = 'SUPER_ADMIN')
ON CONFLICT DO NOTHING;

COMMENT ON COLUMN system.sys_app.module_entry IS 'qiankun HTML 入口；旧 single-spa ESM 地址需迁移';

COMMIT;
