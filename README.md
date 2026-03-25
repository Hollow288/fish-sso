# Fish SSO

一个基于 `Spring Boot 3` 的轻量级 OAuth2 授权码模式 SSO 服务，提供登录、授权同意、令牌签发与用户信息查询能力。

## 技术栈

- Java 21
- Spring Boot 3.5.x
- Spring Web / Spring Security
- Spring Data JPA（MySQL）
- Spring Data Redis（会话、授权码、令牌缓存）
- Maven

## 项目结构

```text
fish-sso
├─ db
│  ├─ schema.sql
│  ├─ login_block_event.sql
│  └─ data.sql
├─ src
│  ├─ main
│  │  ├─ java/com/hollow/fishsso
│  │  │  ├─ config          # 安全与配置绑定
│  │  │  ├─ controller      # 认证/同意/健康检查接口
│  │  │  ├─ service         # 应用服务与核心 SSO 逻辑
│  │  │  ├─ repository      # JPA/Redis 读写适配
│  │  │  ├─ model           # 领域模型
│  │  │  ├─ exception       # 统一异常与错误返回
│  │  │  └─ util            # Cookie 等工具类
│  │  └─ resources
│  │     ├─ application.yml
│  │     └─ application.yml.example
│  └─ test
│     └─ java/com/hollow/fishsso/FishSsoApplicationTests.java
└─ pom.xml
```

## 核心能力

- OAuth2 授权码流程：`/sso/authorize` -> `/consent` -> `/sso/token`
- 登录接口：`/sso/login`（登录成功后下发 `SSO_SESSION` Cookie）
- 用户信息接口：`/sso/userinfo`（Bearer Token）
- 授权同意记录持久化（MySQL）与自动授权
- 登录防护（账号/IP/账号+IP 多维限流与临时封禁，事件落库）

## 运行前准备

1. 安装依赖
- JDK 21
- Maven 3.9+
- MySQL 8+
- Redis 6+

2. 创建数据库

```sql
CREATE DATABASE fish_sso DEFAULT CHARACTER SET utf8mb4;
```

3. 执行初始化脚本

```bash
mysql -u <user> -p fish_sso < db/schema.sql
mysql -u <user> -p fish_sso < db/login_block_event.sql
mysql -u <user> -p fish_sso < db/data.sql
```

`db/data.sql` 为测试数据（可选），包含示例用户和客户端。

## 配置说明

推荐从样例配置开始：

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

关键配置项：

- `spring.datasource.*`：MySQL 连接信息
- `spring.data.redis.*`：Redis 连接信息
- `app.sso.issuer`：SSO 服务签发者地址
- `app.sso.session-ttl`：会话有效期
- `app.sso.auth-code-ttl`：授权码有效期
- `app.sso.access-token-ttl`：访问令牌有效期
- `app.sso.login-protection.*`：登录失败统计与封禁阈值

注意：请勿将真实数据库/Redis 密码提交到仓库。

## 启动方式

开发启动：

```bash
mvn spring-boot:run
```

打包运行：

```bash
mvn clean package
java -jar target/fish-sso-0.0.1-SNAPSHOT.jar
```

默认端口：`9000`

健康检查：

```bash
curl http://localhost:9000/health
```

## 主要接口

1. `GET /sso/authorize`
- 参数：`client_id`、`redirect_uri`、`scope`（可选）、`state`（可选）
- 行为：已满足条件时重定向回业务系统并携带 `code`；否则重定向到 `/consent`

2. `POST /sso/login`
- JSON 入参：`username`、`password`、`return_to`（可选）
- 行为：登录成功后设置 `SSO_SESSION` Cookie，返回会话信息或 303 跳转

3. `GET /consent`
- 参数：`client_id`、`redirect_uri`、`scope`（可选）、`state`（可选）
- 返回：授权同意上下文；未登录时返回 `login_required`

4. `POST /consent`
- JSON 入参：`client_id`、`redirect_uri`、`scope`、`state`、`action`
- `action=approve` 生成授权码；其他值按拒绝处理并返回错误重定向地址

5. `POST /sso/token`
- 表单入参：`grant_type=authorization_code`、`code`、`redirect_uri`、`client_id`、`client_secret`
- 返回：`access_token`、`token_type`、`expires_in`、`scope`

6. `GET /sso/userinfo`
- Header：`Authorization: Bearer <access_token>`
- 返回：`sub`、`username`、`name`、`email`

## 快速联调示例

测试数据（来自 `db/data.sql`）：

- 用户：`alice` / `password123`
- 客户端：`test-client-1` / `secret123`
- 回调：`http://localhost:8080/callback`

1. 发起授权请求（浏览器访问）：

```text
http://localhost:9000/sso/authorize?client_id=test-client-1&redirect_uri=http://localhost:8080/callback&scope=openid%20profile&state=abc123
```

2. 登录（示例）：

```bash
curl -i -X POST http://localhost:9000/sso/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"password123\"}"
```

3. 提交授权同意：

```bash
curl -X POST http://localhost:9000/consent \
  -H "Content-Type: application/json" \
  -d "{\"client_id\":\"test-client-1\",\"redirect_uri\":\"http://localhost:8080/callback\",\"scope\":\"openid profile\",\"state\":\"abc123\",\"action\":\"approve\"}"
```

4. 使用 `code` 换取访问令牌：

```bash
curl -X POST "http://localhost:9000/sso/token" \
  -d "grant_type=authorization_code" \
  -d "code=<code>" \
  -d "redirect_uri=http://localhost:8080/callback" \
  -d "client_id=test-client-1" \
  -d "client_secret=secret123"
```

5. 查询用户信息：

```bash
curl -H "Authorization: Bearer <access_token>" http://localhost:9000/sso/userinfo
```

## 错误返回

服务统一返回：

```json
{
  "error": "invalid_grant",
  "error_description": "授权码无效"
}
```

