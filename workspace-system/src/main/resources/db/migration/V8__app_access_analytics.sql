CREATE TABLE IF NOT EXISTS system.sys_app_access_log (
    id          int8 NOT NULL,
    tenant_id   int8 DEFAULT 0 NOT NULL,
    user_id     int8 NOT NULL,
    app_id      int8 NOT NULL,
    access_time timestamp(6) DEFAULT now() NOT NULL,
    CONSTRAINT sys_app_access_log_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sys_app_access_time
    ON system.sys_app_access_log USING btree (access_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_app_access_user_time
    ON system.sys_app_access_log USING btree (user_id, access_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_app_access_app_time
    ON system.sys_app_access_log USING btree (app_id, access_time DESC);
COMMENT ON TABLE system.sys_app_access_log IS '应用成功进入事件，用于 PV/UV 与访问分析';
