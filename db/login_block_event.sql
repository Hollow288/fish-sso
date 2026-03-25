-- Login block event table for security alert persistence
CREATE TABLE sso_login_block_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    dimension VARCHAR(32) NOT NULL COMMENT 'account/ip/account_ip',
    account VARCHAR(128) NOT NULL COMMENT 'Normalized account',
    ip_address VARCHAR(64) NOT NULL COMMENT 'Normalized source IP',
    user_agent VARCHAR(256) NOT NULL COMMENT 'Normalized user-agent',
    failures BIGINT NOT NULL COMMENT 'Failure count when block triggered',
    threshold_value INT NOT NULL COMMENT 'Configured max failures threshold',
    window_seconds BIGINT NOT NULL COMMENT 'Failure window in seconds',
    block_duration_seconds BIGINT NOT NULL COMMENT 'Block duration in seconds',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Event creation time'
) COMMENT='Login protection block events';

CREATE INDEX idx_login_block_event_created_at ON sso_login_block_event(created_at);
CREATE INDEX idx_login_block_event_account ON sso_login_block_event(account);
CREATE INDEX idx_login_block_event_ip ON sso_login_block_event(ip_address);
