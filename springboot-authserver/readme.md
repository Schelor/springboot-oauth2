


## DDL
###  自动建表
```text
会自动建，已经配置好了。 原理如下：                                                                                                                                            
                                                                                                                                                                                 
  ---                                                                                                                                                                            
  为什么会自动建                                                                                                                                                                 
                                                                                                                                                                                 
  SysOAuthClientPO 上有 @Entity + @Table(name = "sys_oauth_client")，Hibernate 会读取这个注解，在启动时对照数据库里的实际表结构执行 DDL。                                        
                                                                                                                                                                               
  控制这个行为的就是 application.yaml 里这一行：

  spring:
    jpa:
      hibernate:
        ddl-auto: update    # ← 这里

  ---
  ddl-auto 各值的含义

  ┌─────────────┬────────────────────────────────────────────────────────────────┬──────────────────────┐
  │     值      │                              行为                              │       适合场景       │
  ├─────────────┼────────────────────────────────────────────────────────────────┼──────────────────────┤
  │ update      │ 表不存在则新建；已存在则对比 Entity 定义，自动加列（不删列）   │ 开发 / 测试          │
  ├─────────────┼────────────────────────────────────────────────────────────────┼──────────────────────┤
  │ create      │ 每次启动删表重建                                               │ 单测                 │
  ├─────────────┼────────────────────────────────────────────────────────────────┼──────────────────────┤
  │ create-drop │ 启动时建表，关闭时删表                                         │ 单测                 │
  ├─────────────┼────────────────────────────────────────────────────────────────┼──────────────────────┤
  │ validate    │ 只校验表结构是否与 Entity 一致，不做任何改动，不一致则启动失败 │ 生产（推荐）         │
  ├─────────────┼────────────────────────────────────────────────────────────────┼──────────────────────┤
  │ none        │ 完全不管，啥也不做                                             │ 生产（手动管理 DDL） │
  └─────────────┴────────────────────────────────────────────────────────────────┴──────────────────────┘

  ---
  当前配置下的启动行为

  AuthServer 启动
    │
    ├─ Spring sql.init 执行 schema.sql
    │    └─ IF OBJECT_ID IS NULL → 建 oauth2_authorization 表
    │
    ├─ Hibernate ddl-auto: update 扫描所有 @Entity
    │    └─ sys_oauth_client 不存在 → 自动建表
    │         CREATE TABLE sys_oauth_client (
    │             id BIGINT IDENTITY(1,1) PRIMARY KEY,
    │             client_id NVARCHAR(100) NOT NULL UNIQUE,
    │             client_secret NVARCHAR(200) NOT NULL,
    │             scopes NVARCHAR(500),
    │             description NVARCHAR(255)
    │         )
    │
    └─ DataInitializer.run() 插入测试数据
         └─ INSERT test-client / test-secret (如果不存在)

  ---
  生产环境建议

  上线前把 ddl-auto 改为 validate，让 Hibernate 只做校验不做修改，表结构改动改为手写 SQL 脚本（或 Flyway/Liquibase）显式管理：

  spring:
    jpa:
      hibernate:
        ddl-auto: validate   # 生产：只校验，不自动改表

```

### 手动建表
```text
schema.sql — 新增 sys_oauth_client 建表语句

  列定义直接对照 SysOAuthClientPO 的注解翻译：

  ┌──────────────────────────────────┬─────────────────────────────┐
  │           Entity 注解            │       SQL Server 类型       │
  ├──────────────────────────────────┼─────────────────────────────┤
  │ @GeneratedValue(IDENTITY)        │ BIGINT IDENTITY(1,1)        │
  ├──────────────────────────────────┼─────────────────────────────┤
  │ @Column(unique=true, length=100) │ NVARCHAR(100) + UNIQUE 约束 │
  ├──────────────────────────────────┼─────────────────────────────┤
  │ @Column(length=200)              │ NVARCHAR(200)               │
  ├──────────────────────────────────┼─────────────────────────────┤
  │ @Column(length=500)              │ NVARCHAR(500)               │
  └──────────────────────────────────┴─────────────────────────────┘

  ---
  application.yaml — ddl-auto: update → none

  这一步是关键。如果保留 update，启动时会有两个来源同时操作 sys_oauth_client：
  - schema.sql（Spring sql.init）：CREATE TABLE
  - Hibernate（ddl-auto）：也尝试 CREATE / ALTER TABLE

  改成 none 后 Hibernate 只做 ORM 映射，不碰 DDL，表结构完全由 schema.sql 说了算。

```

###  最佳实践
现在有2张表,一个是通过JdbcOAuth2AuthorizationService来管理，底层应该是用的JDBCTempalte来操作的，没有走ORM，另一个应该是用了Hibernate
ORM,这里不一致，有什么区别，最佳实践是什么?  
```text
  这是个很好的架构问题，这个"不一致"实际上是有意为之的，但确实值得理清楚。                                                                                                     
                                                                                                                                                                                 
  ---                                                                                                                                                                            
  两种方式的本质区别                                                                                                                                                             
                                                                                                                                                                                 
  oauth2_authorization — JdbcTemplate（纯 SQL）                                                                                                                                

  JdbcOAuth2AuthorizationService
    └─ JdbcTemplate.query() / update()
         └─ 手写 SQL，直接操作 ResultSet
              └─ 自定义 RowMapper 把列值反序列化回 OAuth2Authorization 对象

  - Spring Authorization Server 内部写死的实现，不经过 ORM
  - OAuth2Authorization 不是简单的 POJO，含 token 值、元数据等复杂嵌套结构，框架用自己的序列化逻辑（Jackson JSON）写入 NVARCHAR(MAX) 列
  - 你无法（也不应该）改它，直接用即可

  sys_oauth_client — JPA/Hibernate（ORM）

  SysOAuthClientRepository (JpaRepository)
    └─ Hibernate
         └─ 自动生成 SQL（SELECT/INSERT/UPDATE/DELETE）
              └─ 把 ResultSet 映射回 SysOAuthClientPO 对象

  - 你自己的业务实体，用 ORM 减少样板代码
  - Hibernate 管理对象生命周期、缓存、关联关系

  ---
  这种"不一致"是框架边界，不是设计缺陷

  ┌─────────────────────────────────────────┐
  │           你的业务域                      │
  │   SysOAuthClientPO  ←→  JPA/Hibernate   │  ← 你控制，用 ORM 合理
  └─────────────────────────────────────────┘
  ┌─────────────────────────────────────────┐
  │         Spring Authorization Server     │
  │   OAuth2Authorization ←→ JdbcTemplate  │  ← 框架控制，不要动
  └─────────────────────────────────────────┘

  两张表属于不同的职责层，技术选型不同是正常的。强行统一反而是反模式。

  ---
  最佳实践：按职责分层选型

  ┌─────────────────────────────────────────────┬──────────────────────────────────┬──────────────────────────────────┐
  │                    场景                     │             推荐方案             │               原因               │
  ├─────────────────────────────────────────────┼──────────────────────────────────┼──────────────────────────────────┤
  │ 框架内置服务（Spring Authorization Server） │ 用框架提供的实现（JdbcTemplate） │ 框架自己管理序列化逻辑，不要绕过 │
  ├─────────────────────────────────────────────┼──────────────────────────────────┼──────────────────────────────────┤
  │ 简单业务实体（CRUD 为主）                   │ JPA/Hibernate                    │ 省代码，对象映射清晰             │
  ├─────────────────────────────────────────────┼──────────────────────────────────┼──────────────────────────────────┤
  │ 复杂查询、高性能、多表 JOIN                 │ MyBatis 或 JdbcTemplate          │ ORM 生成 SQL 难以调优            │
  └─────────────────────────────────────────────┴──────────────────────────────────┴──────────────────────────────────┘

  当前项目的实践就是最佳实践：
  - sys_oauth_client：JPA，因为它是你的业务实体，CRUD 简单
  - oauth2_authorization：JdbcTemplate，因为那是框架的地盘

  ---
  如果你真的想统一（进阶场景）

  统一为 JPA：需要自己实现 OAuth2AuthorizationService，把 OAuth2Authorization 拆解成 JPA 实体持久化，工作量大，意义不大。

  统一为 JdbcTemplate/MyBatis：把 sys_oauth_client 也改成手写 SQL，放弃 SysOAuthClientRepository。适合团队有明确"禁止 JPA"规范的情况。

  对于当前这个项目，没有统一的必要——两者各司其职，边界清晰。

```

## 数据库连接
```text
HikariCP 是什么                                                                                                                                                                
                                                                                                                                                                               
  作者：Brett Wooldridge，2012 年开源，现由社区维护。                                                                                                                            
                  
  定位：Java 生态中最快的 JDBC 连接池，Spring Boot 2.0 起将其设为默认连接池，完全取代了 C3P0 和 DBCP。                                                                           
                                                                                                                                                                                 
  为什么快：                                                                                                                                                                     
                                                                                                                                                                               
  ┌────────────────────────────┬────────────────────────────┐
  │          设计决策            │            效果            │
  ├────────────────────────────┼────────────────────────────┤
  │ 极简代码（~130KB jar）     │ JVM JIT 热编译效率高       │
  ├────────────────────────────┼────────────────────────────┤
  │ 用 FastList 替代 ArrayList │ 减少范围检查开销           │
  ├────────────────────────────┼────────────────────────────┤
  │ 用 ConcurrentBag 管理连接  │ 无锁竞争，高并发下性能稳定 │
  ├────────────────────────────┼────────────────────────────┤
  │ 字节码级别代理 Connection  │ 拦截开销趋近于零           │
  └────────────────────────────┴────────────────────────────┘

  与同类对比（获取连接耗时，越低越好）：
  HikariCP  ~  28µs
  Tomcat    ~  89µs
  C3P0      ~ 540µs

  最重要的：你的项目已经有它了。spring-boot-starter-data-jpa 传递依赖了 HikariCP，默认就在用，现在只需要显式配置参数。

  ---                                                                                                                                                                            
  关键参数说明
                                                                                                                                                                                 
  minimum-idle(5) ←────────────── 连接池始终保有 5 条连接待命                                                                                                                  
                                   应用冷启动后第一个请求不用等建连                                                                                                            

  maximum-pool-size(20) ←───────── 上限 20 条，同时最多 20 个线程并发查库
                                    超出则排队等待，等满 connection-timeout 抛异常

  connection-timeout(30s) ←──────── 获取连接的等待上限
                                     超时说明数据库或网络有问题，快速失败比卡住好

  max-lifetime(30min) ←──────────── 连接到期强制回收重建
                                     SQL Server 默认会在一段时间后主动断开空闲连接
                                     max-lifetime 要小于数据库的 wait_timeout

  keepalive-time(60s) ←──────────── 定期发 SELECT 1 给数据库
                                     防止防火墙/负载均衡把"静默"连接强制关闭

  连接数怎么选？

  maximum-pool-size 并不是越大越好，HikariCP 官方推荐公式：

  最优连接数 ≈ CPU 核心数 × 2 + 有效磁盘数

  对于 AuthServer 这种低并发场景，10~20 完全够用，设太大反而因锁竞争降低吞吐量。

```

## Oauth2 Token

### GET JWT
http://localhost:8080/auth/oauth2/token

```json
{
    "access_token": "Header.Payload.Sig",
    "token_type": "Bearer",
    "expires_in": 7199
}

```

### GET JWK
http://localhost:8080/auth/oauth2/jwks
```json
{
    "keys": [
        {
            "kty": "RSA",
            "e": "AQAB",
            "kid": "f90f1013-d227-4b8c-9779-e9092c1adc70",
            "n": "n"
        }
    ]
}
```

### claims 术语
```text
claims 是 JWT / OAuth2 领域的标准术语。

  中文没有一个统一的官方译名，实际上有三种叫法并存：                                                                                                                             
   
  ┌──────────┬───────────────────────────────────────┐                                                                                                                           
  │   叫法   │               使用场景                │                                                                                                                         
  ├──────────┼───────────────────────────────────────┤
  │ 声明     │ 最常见的直译，RFC 7519 中文译本用这个 │
  ├──────────┼───────────────────────────────────────┤
  │ 载荷字段 │ 强调它在 JWT Payload 里的位置         │
  ├──────────┼───────────────────────────────────────┤
  │ 权利要求 │ 法律/金融背景下偶尔出现，非常生硬     │
  └──────────┴───────────────────────────────────────┘

  实际工作中：国内开发者日常交流几乎都直接说 "claims"，不翻译，和 token、scope、issuer 一样当作技术词汇直接用。你在代码注释、技术文档里写 "claims" 完全没问题，读者都能看懂。

  ---
  记忆方式：JWT 的三段结构

  eyJhbGciOiJSUzI1NiJ9          ← Header（头部）：算法、类型
  .
  eyJzdWIiOiJ0ZXN0LWNsaWVudCJ9  ← Payload（载荷）：就是 claims 的集合
  .
  SflKxwRJSMeKKF2QT4fw...        ← Signature（签名）：防篡改

  Payload 里每一个键值对就是一个 claim，比如：

  {
    "sub": "test-client",    ← Subject claim，主体是谁
    "iss": "http://...",     ← Issuer claim，谁签发的
    "exp": 1783843394,       ← Expiration claim，什么时候过期
    "jti": "fd7eb364-..."    ← JWT ID claim，这个 token 的唯一 ID
  }

  这些 sub / iss / exp / jti 都是 RFC 7519 定义的标准 claims（Registered Claims），开发者也可以往里加自定义字段（Custom Claims）。

```

### Token查询
```text

  ---
  Postman 使用方式

  接口 1 — 按 client 查 token 列表
  GET http://localhost:8080/auth/admin/tokens?principal=test-client

  接口 2 — 按 token 值查详情（含 JWT claims）
  POST http://localhost:8080/auth/admin/tokens/lookup
  Content-Type: application/json

  {
    "tokenValue": "eyJhbGci..."
  }

  ---
  两个接口的数据来源差异

  ┌─────────────┬──────────────────────────────────────────┬──────────────────────────────────────────┐
  │             │            GET /admin/tokens             │        POST /admin/tokens/lookup         │
  ├─────────────┼──────────────────────────────────────────┼──────────────────────────────────────────┤
  │ 查询方式     │ JdbcTemplate + idx_oauth2_auth_principal │ OAuth2AuthorizationService.findByToken() │
  ├─────────────┼──────────────────────────────────────────┼──────────────────────────────────────────┤
  │ 返回 claims │ ❌（避免解析 NVARCHAR(MAX) 列）          │ ✅（从 token metadata 反序列化）         │
  ├─────────────┼──────────────────────────────────────────┼──────────────────────────────────────────┤
  │ 适合场景    │ 查某个 client 的历史记录                 │ 验证某个具体 token 的内容                │
  └─────────────┴──────────────────────────────────────────┴──────────────────────────────────────────┘

```

### client_id/ client_secret
目前sys_oauth_client存储的test-client是明文，client_secret是密文，然后oauth-client模块配置的client_secret是明文，
有没有办法oauth-client配置密文? 例如client-secret: {cypher}xxxx
```text
 {cipher} 是什么

  你说的 {cipher}xxxx 是 Spring Cloud Config Server 的专属功能：

  Config Server（统一配置中心）
    ├─ 存储加密值：client-secret: "{cipher}AQBx9k..."
    ├─ 客户端 pull 配置时，Config Server 自动解密
    └─ 解密后把明文推送给各个微服务

  这个项目用不了，原因：没有 Config Server，oauth2-client 是独立的 Spring Boot 应用，直接读本地 application.yaml，没有解密的中间层。

  ---
  这个场景的正确选项

  选项1：Jasypt（最常用，侵入性最小）

  专门做 Spring Boot 属性加密，使用 ENC(密文) 前缀：

  # application.yaml
  spring:
    security:
      oauth2:
        client:
          registration:
            auth-server:
              client-secret: ENC(A3B9xKj7mNpQ2rTv...)  # 密文

  工作原理：
  应用启动
    → Jasypt PropertySource 拦截所有 ENC(...) 值
    → 用配置的密钥解密
    → 注入明文到 Spring 容器
    → OAuth2 客户端拿到明文去请求 token

  实现步骤：

  ① build.gradle 加依赖（springboot-oauth2-client）
  implementation 'com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5'

  ② 生成密文（用 Jasypt 提供的工具加密）
  # 用你选的加密密钥（masterKey）加密明文 test-secret
  java -cp jasypt-1.9.3.jar \
    org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
    input="test-secret" password="masterKey" algorithm=PBEWithMD5AndDES
  得到密文，填入 ENC(密文)。

  ③ 密钥怎么传入（绝对不能写在 yaml 里，否则白费）
  # 方式A：启动参数（本地开发）
  java -jar app.jar --jasypt.encryptor.password=masterKey

  # 方式B：环境变量（生产推荐）
  export JASYPT_ENCRYPTOR_PASSWORD=masterKey

  ---
  选项2：环境变量（最简单，生产最推荐）

  不改代码，直接把明文挪出 yaml：

  # application.yaml
  spring:
    security:
      oauth2:
        client:
          registration:
            auth-server:
              client-secret: ${OAUTH2_CLIENT_SECRET}  # 从环境变量读

  # 本地启动
  export OAUTH2_CLIENT_SECRET=test-secret
  ./gradlew bootRun

  # Docker/K8s 注入 Secret，yaml 里永远只有变量名，没有明文

  ---
  建议
  ┌──────────────────────────┬──────────────────────────────┐
  │           场景            │           推荐方案           │
  ├──────────────────────────┼──────────────────────────────┤
  │ 本地开发/测试              │ 明文就好，别过度设计         │
  ├──────────────────────────┼──────────────────────────────┤
  │ 需要把配置文件提交 Git      │ Jasypt ENC(...)              │
  ├──────────────────────────┼──────────────────────────────┤
  │ 容器化部署（Docker/K8s）   │ 环境变量 / K8s Secret        │
  ├──────────────────────────┼──────────────────────────────┤
  │ 有统一配置中心             │ Spring Cloud Config {cipher} │
  └──────────────────────────┴──────────────────────────────┘

```