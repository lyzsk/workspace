CREATE INDEX IF NOT EXISTS idx_sys_user_sync_update
    ON system.sys_user USING btree (update_time, id)
    WHERE is_deleted = 0;
