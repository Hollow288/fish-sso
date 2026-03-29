-- Fish SSO 数据库建表脚本

-- 用户表
CREATE TABLE sso_user (
    id VARCHAR(64) PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(128) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    display_name VARCHAR(128) COMMENT '显示名称',
    email VARCHAR(255) COMMENT '邮箱地址'
) COMMENT='用户账号表';

-- 客户端表
CREATE TABLE sso_client (
    client_id VARCHAR(128) PRIMARY KEY COMMENT '客户端ID',
    client_secret_hash VARCHAR(255) NOT NULL COMMENT '客户端密钥哈希值',
    home_url VARCHAR(512) COMMENT '客户端首页地址'
) COMMENT='客户端注册表';

-- 客户端回调地址表
CREATE TABLE sso_client_redirect_uri (
    client_id VARCHAR(128) NOT NULL COMMENT '客户端ID',
    redirect_uri VARCHAR(512) NOT NULL COMMENT '回调地址',
    FOREIGN KEY (client_id) REFERENCES sso_client(client_id) ON DELETE CASCADE
) COMMENT='客户端回调地址表';

-- 客户端权限范围表
CREATE TABLE sso_client_scope (
    client_id VARCHAR(128) NOT NULL COMMENT '客户端ID',
    scope VARCHAR(128) NOT NULL COMMENT '权限范围',
    FOREIGN KEY (client_id) REFERENCES sso_client(client_id) ON DELETE CASCADE
) COMMENT='客户端权限范围表';

-- 用户授权同意记录表
CREATE TABLE sso_consent_grant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '授权记录ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    client_id VARCHAR(128) NOT NULL COMMENT '客户端ID',
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    CONSTRAINT uk_sso_consent_user_client UNIQUE (user_id, client_id),
    FOREIGN KEY (user_id) REFERENCES sso_user(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES sso_client(client_id) ON DELETE CASCADE
) COMMENT='用户授权同意记录表';

-- 用户授权同意范围表
CREATE TABLE sso_consent_grant_scope (
    consent_id BIGINT NOT NULL COMMENT '授权记录ID',
    scope VARCHAR(128) NOT NULL COMMENT '权限范围',
    PRIMARY KEY (consent_id, scope),
    FOREIGN KEY (consent_id) REFERENCES sso_consent_grant(id) ON DELETE CASCADE
) COMMENT='用户授权同意范围表';

-- 创建索引
CREATE INDEX idx_user_username ON sso_user(username);
CREATE INDEX idx_client_redirect_uri ON sso_client_redirect_uri(client_id);
CREATE INDEX idx_client_scope ON sso_client_scope(client_id);
CREATE INDEX idx_consent_grant_user_client ON sso_consent_grant(user_id, client_id);
CREATE INDEX idx_consent_grant_scope ON sso_consent_grant_scope(consent_id);
