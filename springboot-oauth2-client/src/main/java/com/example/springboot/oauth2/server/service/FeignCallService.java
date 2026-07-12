package com.example.springboot.oauth2.server.service;

import com.example.springboot.oauth2.server.feign.ResourceServerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 基于 OpenFeign 调用 Resource Server 的服务类。
 *
 * <p>对比 ApiCallService (JDK HTTPClient + RestClient)：
 * <pre>
 * ApiCallService (命令式)           FeignCallService (声明式)
 * ─────────────────────────         ──────────────────────────
 * 手动 fetchAccessToken()           由 OAuth2FeignInterceptor 自动注入
 * UriComponentsBuilder 拼 URL       由 @QueryMap 注解自动完成
 * .header("Authorization", ...)    由拦截器统一处理
 * .retrieve().body(TypeRef)        由 JacksonDecoder 自动反序列化
 * </pre>
 * 业务代码只需调用一个方法，HTTP 细节完全透明。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeignCallService {

    private final ResourceServerClient resourceServerClient;

    /**
     * 通过 Feign 客户端调用 Resource Server 的 /hello 接口。
     * token 获取、URL 拼接、header 注入均由框架完成。
     */
    public Map<String, Object> callHello(Map<String, String> params) {
        log.debug("[Feign] Calling /hello with params: {}", params);
        return resourceServerClient.hello(params);
    }
}