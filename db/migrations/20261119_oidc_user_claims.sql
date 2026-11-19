-- MySQL 8+，适用于仍含 username、display_name 列的现有数据库；只执行一次。
-- 停止旧版本后端，备份数据库，执行本脚本，然后同时部署新版前后端。
-- 必须先迁移再启动新版后端，不能依赖 Hibernate ddl-auto=update 识别列重命名。
-- RENAME COLUMN 保留原值和唯一索引；id 及全部引用它的外键保持不变。
ALTER TABLE sso_user
    RENAME COLUMN username TO preferred_username,
    RENAME COLUMN display_name TO name,
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '邮箱所有权是否已经验证';

-- 不根据邮箱非空推断验证状态；已有账号默认 false，完成所有权验证后才能更新。
-- 验收：检查登录名、显示名和用户 ID 均保持原值。
SELECT id, preferred_username, name, email, email_verified FROM sso_user;
