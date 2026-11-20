CREATE TABLE IF NOT EXISTS system.sys_license (
    id               int8 NOT NULL,
    license_id       varchar(64),
    serial           int8 NOT NULL DEFAULT 0,
    license_status   varchar(24),
    machine_code     varchar(128),
    issued_at        timestamp(6),
    expires_at       timestamp(6),
    features         varchar(1000),
    license_json     text,
    max_serial_seen  int8 NOT NULL DEFAULT 0,
    installation_id  varchar(64),
    last_verified_at timestamp(6),
    last_seen_epoch  int8 NOT NULL DEFAULT 0,
    tamper_flag      int2 NOT NULL DEFAULT 0,
    update_time      timestamp(6),
    CONSTRAINT sys_license_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE system.sys_license IS '平台授权：当前 License、安装实例与防回滚序列';

CREATE TABLE IF NOT EXISTS system.sys_license_audit (
    id          int8 NOT NULL,
    seq         int8 NOT NULL,
    event       varchar(64) NOT NULL,
    detail      varchar(2000),
    prev_hash   varchar(64),
    hash        varchar(64) NOT NULL,
    create_time timestamp(6) DEFAULT now() NOT NULL,
    CONSTRAINT sys_license_audit_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_sys_license_audit_seq
    ON system.sys_license_audit USING btree (seq DESC);
COMMENT ON TABLE system.sys_license_audit IS '平台授权审计：哈希链记录';
