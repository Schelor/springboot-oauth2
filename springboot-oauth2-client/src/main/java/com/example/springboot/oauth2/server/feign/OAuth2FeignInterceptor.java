package com.example.springboot.oauth2.server.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/**
 * Feign RequestInterceptor：在每次 HTTP 请求发出前，统一注入 Bearer Token。
 *
 * <p>这是 OpenFeign 的核心优势之一：把横切关注点（认证）从业务代码中剥离出去。
 * ApiCallService 需要手动调用 fetchAccessToken() 并拼接 header；
 * 这里只需注册一次拦截器，所有 Feign 接口的调用都自动带上 token。</p>
 *
 * <p>token 由 OAuth2AuthorizedClientManager 获取和缓存，逻辑与原 ApiCallService 完全一致。</p>
 */
@Slf4j
public class OAuth2FeignInterceptor implements RequestInterceptor {

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2FeignInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public void apply(RequestTemplate template) {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId("auth-server")
                .principal("system")
                .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Failed to obtain access token from AuthServer");
        }

        String token = authorizedClient.getAccessToken().getTokenValue();
        log.debug("[Feign] Injecting Bearer token: {}...", token.substring(0, 20));
        template.header("Authorization", "Bearer " + token);
    }
}