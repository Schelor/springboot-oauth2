## 摘要

总体理解一下这套技术方案。
client_credentials模式解决的是保护服务端的API，防止被外部恶意调用。怎么保护呢？比如客户端A要调用服务端B的敏感API接口，例如获取订单/order，流程如下
1.AuthServer颁发一个client_id/client_secret给客户端A
2.客户端调用服务端B时,先向AuthServer申请access_token（jwt格式)即调用/oauth2/token接口
3.客户端A请求服务端B的/order,在Header中带上申请到的access_token，然后发起http调用/order
4.服务端B的/order接口收到调用请求后,需要先取出Header中的access_token 向AuthServer验证这个access_token是否有效,如果OK,认为是合法调用,可以继续执行后续流程
> 核心修正：服务端 B 真的需要去“调用” AuthServer 验证吗？
答案是：不需要！这也是为什么我们要用 JWT 的根本原因。
如果你的理解（第 4 点）成立，即服务端 B 每次收到请求都要拿 Token 去 AuthServer 问一下“这玩意儿合法吗？”，那 AuthServer 会成为极其严重的性能瓶颈和单点故障源。这叫 “ opaque token (不透明令牌) ”模式，是早期的、落后的做法。
JWT (JSON Web Token) 的精髓就在于“自包含”：
JWT 的 Payload 里已经写死了客户端 A 的 ID、拥有的权限、过期时间，并且用 AuthServer 的私钥签了名。
服务端 B 只要在本地存一份 AuthServer 的公钥，自己就能验算签名。如果签名对得上，且没过期，服务端 B 就认为 Token 合法。整个验证过程完全是本地计算，服务端 B 和 AuthServer 之间零网络通信！

5.Spring Security + Oauth2.0 把这套方案做成了框架,提供了横切面拦截的模式隐藏了申请Token或验证Token的过程
> 具体是怎么隐藏的呢？
在服务端 B（资源服务器）的 Spring Boot 工程里，你只需要引入 spring-boot-starter-oauth2-resource-server 依赖。Spring Security 会自动注入一个过滤器叫 BearerTokenAuthenticationFilter。
你不需要写任何验证 Token 的代码，这个过滤器会自动：
拦截请求，找 Authorization: Bearer xxx。
调用内部的 JwtDecoder（里面藏着从 AuthServer 异步拉过来的公钥）。
本地完成 RSA 验签。
如果成功，把它包装成一个 Authentication 对象塞进 SecurityContext。
后续你的 /order Controller 就能直接用了（比如用 @PreAuthorize("hasAuthority('read')") 控制权限）。

## 简要流程图
[流程图](diagram.svg)


## REF

OAuth2TokenEndpointFilter

OAuth2ClientCredentialsAuthenticationProvider