INSERT INTO system.sys_role
    (id, tenant_id, role_code, role_name, data_scope, role_sort, menu_check_strictly,
     status, app_id, create_time, update_time, is_deleted)
SELECT 3900000000000000001, 0, 'ADMIN', '系统管理员', 1, 2, true, 1,
       (SELECT id FROM system.sys_app WHERE code = 'workspace' AND is_deleted = 0 LIMIT 1),
       now(), now(), 0
WHERE NOT EXISTS (
    SELECT 1
    FROM system.sys_role r
    JOIN system.sys_app a ON a.id = r.app_id
    WHERE r.role_code = 'ADMIN' AND a.code = 'workspace' AND r.is_deleted = 0
);

INSERT INTO system.sys_role_menu (id, tenant_id, role_id, menu_id, create_time)
SELECT 4000000000000000000 + ROW_NUMBER() OVER (ORDER BY m.id), 0, r.id, m.id, now()
FROM system.sys_menu m
JOIN system.sys_role r ON r.role_code = 'ADMIN' AND r.is_deleted = 0
JOIN system.sys_app a ON a.id = r.app_id AND a.code = 'workspace' AND a.is_deleted = 0
WHERE m.app_code = 'workspace'
  AND m.is_deleted = 0
  AND COALESCE(m.permission, '') NOT LIKE 'sys:license%'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS system.sys_api_client (
    id          int8 NOT NULL,
    tenant_id   int8 DEFAULT 0 NOT NULL,
    client_id   varchar(64) NOT NULL,
    client_name varchar(100) NOT NULL,
    secret_hash varchar(200) NOT NULL,
    scopes      varchar(1000),
    status      smallint DEFAULT 1 NOT NULL,
    expires_at  timestamp(6),
    last_used_at timestamp(6),
    create_by   int8,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    update_by   int8,
    update_time timestamp(6),
    is_deleted  smallint DEFAULT 0 NOT NULL,
    deleted_by  int8,
    deleted_time timestamp(6),
    remark      varchar(500),
    CONSTRAINT sys_api_client_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_api_client_id
    ON system.sys_api_client USING btree (client_id)
    WHERE is_deleted = 0;
COMMENT ON TABLE system.sys_api_client IS '调用方专用凭证：clientId + 密钥哈希，用于 DSH/子系统调用';
COMMENT ON COLUMN system.sys_api_client.scopes IS '允许调用的权限域，逗号分隔';
