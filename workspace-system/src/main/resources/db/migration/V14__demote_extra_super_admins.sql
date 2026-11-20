UPDATE system.sys_user u
SET token_version = token_version + 1, update_time = now()
WHERE u.is_deleted = 0
  AND u.username <> 'superadmin'
  AND EXISTS (
      SELECT 1
      FROM system.sys_user_role ur
      JOIN system.sys_role r ON r.id = ur.role_id
      WHERE ur.user_id = u.id AND ur.is_deleted = 0
        AND r.role_code = 'SUPER_ADMIN' AND r.is_deleted = 0
  );

DELETE FROM system.sys_user_role ur
USING system.sys_user u, system.sys_role r
WHERE ur.user_id = u.id
  AND ur.role_id = r.id
  AND r.role_code = 'SUPER_ADMIN'
  AND u.username <> 'superadmin';

INSERT INTO system.sys_user_role (id, tenant_id, user_id, role_id, create_time)
SELECT 3900000000000000103, 0, u.id, r.id, now()
FROM system.sys_user u
JOIN system.sys_role r ON r.role_code = 'TEST' AND r.is_deleted = 0 AND r.status = 1
JOIN system.sys_app a ON a.id = r.app_id AND a.code = 'workspace' AND a.is_deleted = 0
WHERE u.username = 'biruiwen' AND u.is_deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO system.sys_user_app (id, tenant_id, user_id, app_id, status, create_time)
SELECT 3900000000000000104, 0, u.id, a.id, 1, now()
FROM system.sys_user u
JOIN system.sys_app a ON a.code = 'workspace' AND a.is_deleted = 0
WHERE u.username = 'biruiwen' AND u.is_deleted = 0
ON CONFLICT DO NOTHING;
