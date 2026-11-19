ALTER TABLE system.sys_user_session
    ADD COLUMN IF NOT EXISTS session_code varchar(64),
    ADD COLUMN IF NOT EXISTS refresh_family varchar(64),
    ADD COLUMN IF NOT EXISTS token_version int8 DEFAULT 0 NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_session_session_code
    ON system.sys_user_session USING btree (session_code)
    WHERE session_code IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_session_refresh_family
    ON system.sys_user_session USING btree (refresh_family)
    WHERE refresh_family IS NOT NULL;

CREATE TABLE IF NOT EXISTS system.sys_refresh_token (
    id               int8 NOT NULL,
    session_id       int8 NOT NULL,
    token_hash       varchar(64) NOT NULL,
    issued_at        timestamp(6) DEFAULT now() NOT NULL,
    expires_at       timestamp(6) NOT NULL,
    consumed_at      timestamp(6),
    revoked_at       timestamp(6),
    replaced_by_hash varchar(64),
    CONSTRAINT sys_refresh_token_pkey PRIMARY KEY (id),
    CONSTRAINT ck_sys_refresh_token_hash CHECK (char_length(token_hash) = 64),
    CONSTRAINT ck_sys_refresh_token_replacement_hash
        CHECK (replaced_by_hash IS NULL OR char_length(replaced_by_hash) = 64)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_refresh_token_hash
    ON system.sys_refresh_token USING btree (token_hash);
CREATE INDEX IF NOT EXISTS idx_sys_refresh_token_session
    ON system.sys_refresh_token USING btree (session_id, issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_sys_refresh_token_expires
    ON system.sys_refresh_token USING btree (expires_at);

COMMENT ON TABLE system.sys_refresh_token IS '不透明 refresh token 哈希、轮换与撤销元数据';
COMMENT ON COLUMN system.sys_user_session.session_code IS '不透明认证会话标识，新会话与 jti 相同';
COMMENT ON COLUMN system.sys_user_session.refresh_family IS 'refresh token 轮换族标识';
COMMENT ON COLUMN system.sys_user_session.token_version IS '签发会话时的用户 tokenVersion';
COMMENT ON COLUMN system.sys_refresh_token.token_hash IS 'refresh token 的 SHA-256 十六进制哈希';
COMMENT ON COLUMN system.sys_refresh_token.consumed_at IS '完成轮换的时间，非空记录再次使用时撤销整个会话';
