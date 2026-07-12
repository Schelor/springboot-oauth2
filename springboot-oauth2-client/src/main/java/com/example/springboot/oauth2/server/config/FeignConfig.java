package com.example.springboot.oauth2.server.config;

import com.example.springboot.oauth2.server.feign.OAuth2FeignInterceptor;
import com.example.springboot.oauth2.server.feign.ResourceServerClient;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import okhttp3.ConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

import java.util.concurrent.TimeUnit;

/**
 * OpenFeign 客户端配置。
 *
 * <p>使用 Feign.builder() 以编程方式构建客户端（不依赖 Spring Cloud @EnableFeignClients）：
 * <ul>
 *   <li>client: OkHttpClient —— 内置连接池，TCP 连接复用，精细超时控制，生产推荐</li>
 *   <li>encoder/decoder: JacksonEncoder/JacksonDecoder —— 处理 JSON 序列化</li>
 *   <li>requestInterceptor: OAuth2FeignInterceptor —— 统一注入 Bearer Token</li>
 *   <li>target: 绑定接口与 Resource Server 基础 URL</li>
 * </ul>
 * </p>
 */
@Configuration
public class FeignConfig {

    @Bean
    public ResourceServerClient resourceServerClient(
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${resource-server.base-url}") String baseUrl) {

        okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        return Feign.builder()
                // OkHttp：连接池复用 TCP 连接，比 JDK HttpClient 有更完善的生产特性
                .client(new OkHttpClient(okHttpClient))
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                // 每次请求前自动注入 "Authorization: Bearer <token>"
                .requestInterceptor(new OAuth2FeignInterceptor(authorizedClientManager))
                .target(ResourceServerClient.class, baseUrl);
    }
}