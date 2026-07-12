
## See

- BearerTokenAuthenticationFilter
- JwtAuthenticationProvider
- JwtAuthenticationToken

## Simple Overview
```text
---                                                                                                                                                                            
  入口：两行配置触发了一切                                                                                                                                                       
                                                                                                                                                                                 
  springboot-oauth2-server/src/main/resources/application.yaml                                                                                                                   
  spring:                                                                                                                                                                        
    security:                                                                                                                                                                    
      oauth2:                                                                                                                                                                    
        resourceserver:                                                                                                                                                          
          jwt:
            jwk-set-uri: http://localhost:8080/auth/oauth2/jwks  # ← 去哪拿公钥
            issuer-uri:  http://localhost:8080/auth              # ← JWT 的 iss 必须等于这个

  springboot-oauth2-server/src/main/java/.../config/SecurityConfig.java
  http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
  //                         ↑ 这一行注册了整个 JWT 验证管道

---
  完整的验证链（Spring Security 框架内部）

  HTTP 请求
    │
    ▼
  BearerTokenAuthenticationFilter          ← 从 Authorization: Bearer xxx 提取 token
    │  (spring-security-oauth2-resource-server jar)
    │
    ▼
  JwtAuthenticationProvider               ← 调用 JwtDecoder 验证
    │
    ▼
  NimbusJwtDecoder                        ← 核心：签名验证在这里
    ├─ 第一次：GET http://localhost:8080/auth/oauth2/jwks  → 拿到 RSA 公钥，缓存
    ├─ 此后：直接用缓存的公钥，本地验证签名（不再网络请求）
    ├─ 验证签名（RSA-256）
    ├─ 验证 exp（是否过期）
    └─ 验证 iss（必须 = issuer-uri）
    │
    ▼
  JwtGrantedAuthoritiesConverter          ← 从 scope claim 提取权限
    │
    ▼
  SecurityContextHolder.setAuthentication(...)
    │
    ▼
  HelloController.hello()                 ← 到这里才是你的业务代码

  ---
  你要找的"验证源码"在哪

  这些都是 Spring Security 的 jar 内部类，不在你的项目里，但可以在 IDE 里直接跳进去看：

  ┌──────────────────────┬───────────────────────────────────────────────────────┐
  │         职责         │                  Spring Security 类                   │
  ├──────────────────────┼───────────────────────────────────────────────────────┤
  │ 从 Header 提取 token │ BearerTokenAuthenticationFilter                       │
  ├──────────────────────┼───────────────────────────────────────────────────────┤
  │ 拉取 JWKS + 缓存公钥 │ NimbusJwtDecoder（由 jwk-set-uri 自动创建）           │
  ├──────────────────────┼───────────────────────────────────────────────────────┤
  │ RSA 签名验证         │ NimbusJwtDecoder#decode() → 内部委托给 Nimbus JOSE 库 │
  ├──────────────────────┼───────────────────────────────────────────────────────┤
  │ exp / iss 校验       │ JwtValidators.createDefaultWithIssuer()               │
  ├──────────────────────┼───────────────────────────────────────────────────────┤
  │ 注册整个管道         │ OAuth2ResourceServerConfigurer#configure()            │
  └──────────────────────┴───────────────────────────────────────────────────────┘

  在 IDE 里找到它们

  在 IntelliJ 里按 Shift+Shift，搜索 NimbusJwtDecoder，双击打开，找 decode() 方法，这就是签名验证的真正执行点。

  ---
  关键细节：JWKS 只拉一次

  Server 启动
    │
    ├─ Spring 读取 jwk-set-uri 配置
    ├─ 创建 NimbusJwtDecoder bean
    │   └─ 内部持有一个 JWKSource，惰性加载（第一次 decode 时才真正请求）
    │
  第一次请求进来
    ├─ decode() 发现缓存为空
    ├─ GET http://localhost:8080/auth/oauth2/jwks → 拿到公钥
    ├─ 缓存公钥
    └─ 用公钥验证签名 ✓

  后续所有请求
    └─ 直接用缓存公钥，本地 RSA 验证，不再请求 AuthServer

  所以你在 debug 里看不到每次请求都有对 /jwks 的调用——它只在第一次发生，之后完全本地验证。如果你想看到这个网络请求，在首次调用 /hello 时在 NimbusJwtDecoder#decode()
  打断点就能捕捉到。

```