package com.example.springboot.oauth2.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
public class ApiCallService {

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final RestClient restClient;
    private final String helloUrl;

    public ApiCallService(OAuth2AuthorizedClientManager authorizedClientManager,
                          @Value("${resource-server.hello-url}") String helloUrl) {
        this.authorizedClientManager = authorizedClientManager;
        // 显式使用 JDK HTTP Client，避免 Jetty 的 HttpClient（因为 spring-boot-starter-jetty
        // 在 classpath 上会被 ClientHttpRequestFactoryBuilder.detect() 自动选中）。
        // Jetty 的客户端严格遵守 RFC：收到没有 WWW-Authenticate 头的 401 时直接抛异常。
        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
        this.helloUrl = helloUrl;
    }

    /**
     * 向 AuthServer 申请 access_token（内部自动缓存，过期前自动刷新），
     * 然后携带 Bearer Token 调用 Resource Server 的 /hello 接口。
     */
    public Map<String, Object> callHello(Map<String, String> params) {
        String accessToken = fetchAccessToken();
        log.debug("Calling Resource Server with token: {}...", accessToken.substring(0, 20));

        // 将 Map 中的参数附加为 Query String
        // fromUriString 会正确解析 scheme/host/port/path，fromPath 只适合路径片段
        // fromHttpUrl 在 Spring Framework 7 (Spring Boot 4) 中已移除
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(helloUrl);
        params.forEach(uriBuilder::queryParam);

        return restClient.get()
                .uri(uriBuilder.build().toUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private String fetchAccessToken() {
        // client_credentials 无用户上下文，principal 仅作为 Token 缓存 key
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId("auth-server")
                .principal("system")
                .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Failed to obtain access token from AuthServer");
        }
        return authorizedClient.getAccessToken().getTokenValue();
    }
}
