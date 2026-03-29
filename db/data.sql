-- Fish SSO 测试数据插入脚本

-- 插入测试用户
-- 密码: password123 (BCrypt加密后的哈希值)
INSERT INTO sso_user (id, username, password_hash, display_name, email) VALUES
('user-001', 'alice', '$2a$10$Duc30Cxpx4/Trh09a0SZT.Rh1KdyeKtOSCNAmH6406WZPUPK2wYPK', '爱丽丝', 'alice@example.com'),
('user-002', 'bob', '$2a$10$Duc30Cxpx4/Trh09a0SZT.Rh1KdyeKtOSCNAmH6406WZPUPK2wYPK', '鲍勃', 'bob@example.com'),
('user-003', 'charlie', '$2a$10$Duc30Cxpx4/Trh09a0SZT.Rh1KdyeKtOSCNAmH6406WZPUPK2wYPK', '查理', 'charlie@example.com');

-- 插入测试客户端
-- 客户端密钥: secret123 (BCrypt加密后的哈希值)
INSERT INTO sso_client (client_id, client_secret_hash, home_url) VALUES
('test-client-1', '$2a$10$EFFqocQPlc06ODlZBG9B.OlazGQy03Za0srhThLCFuaKPvPvV//Ke', 'http://localhost:8080'),
('test-client-2', '$2a$10$EFFqocQPlc06ODlZBG9B.OlazGQy03Za0srhThLCFuaKPvPvV//Ke', 'http://localhost:3000');

-- 插入客户端回调地址
INSERT INTO sso_client_redirect_uri (client_id, redirect_uri) VALUES
('test-client-1', 'http://localhost:8080/callback'),
('test-client-1', 'http://localhost:8080/auth/callback'),
('test-client-2', 'http://localhost:3000/callback');

-- 插入客户端权限范围
INSERT INTO sso_client_scope (client_id, scope) VALUES
('test-client-1', 'openid'),
('test-client-1', 'profile'),
('test-client-1', 'email'),
('test-client-2', 'openid'),
('test-client-2', 'profile');
