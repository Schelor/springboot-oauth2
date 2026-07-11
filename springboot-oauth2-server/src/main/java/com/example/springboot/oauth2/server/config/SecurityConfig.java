package com.example.springboot.oauth2.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 所有请求都需要携带有效的 JWT Bearer Token
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            // 无状态：不创建 Session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 启用 OAuth2 Resource Server，自动注入 BearerTokenAuthenticationFilter
            // 该 Filter 会从 Authorization: Bearer <token> 中提取 JWT，
            // 用 jwk-set-uri 拉取的公钥做本地 RSA 验签，无需访问 AuthServer
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
