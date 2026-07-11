package com.example.springboot.oauth2.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Client 自身的端点不需要鉴权，对外开放即可。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * 使用 AuthorizedClientServiceOAuth2AuthorizedClientManager（适合 client_credentials 无用户上下文场景）。
     *
     * 不需要自定义 RestClient：
     * - ClientCredentialsOAuth2AuthorizedClientProvider 内部默认使用
     *   RestClientClientCredentialsTokenResponseClient，它已正确配置了
     *   OAuth2AccessTokenResponseHttpMessageConverter，能正常解析 token 响应。
     * - Jetty 的严格校验（401 无 WWW-Authenticate 抛异常）只在 token 请求失败时触发。
     *   修复 client-authentication-method: client_secret_post 后，Auth Server 正常返回 200，
     *   Jetty 不再抛异常，此处无需绕过 Jetty 的 HTTP Client。
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        ClientCredentialsOAuth2AuthorizedClientProvider credentialsProvider =
                new ClientCredentialsOAuth2AuthorizedClientProvider();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(credentialsProvider);
        return manager;
    }
}
