package com.example.demogradle.entity;


import jakarta.persistence.*;
import lombok.*;

/**
 * 自定义 OAuth2 客户端实体类
 * 对应数据库表：sys_oauth_client
 */
@Data
@Entity
@Table(name = "sys_oauth_client")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysOAuthClientPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 客户端ID (对应请求时的 client_id)
     * 必须唯一
     */
    @Column(nullable = false, unique = true, length = 100)
    private String clientId;

    /**
     * 客户端密文 (对应请求时的 client_secret)
     * 【重要】Spring Authorization Server 强制要求此字段存储的是 BCrypt 加密后的密文！
     */
    @Column(nullable = false, length = 200)
    private String clientSecret;

    /**
     * 授权范围，多个逗号分隔 (例如: "read,write")
     * 在 client_credentials 模式下，代表该客户端能访问的资源范围
     */
    @Column(length = 500)
    private String scopes;

    /**
     * 客户端描述/名称，方便在 H2 控制台里辨认
     */
    @Column(length = 255)
    private String description;
}
