package com.example.demogradle.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Token 查询结果 DTO，用于 /admin/tokens 接口的 JSON 响应。
 */
@Data
@Builder
public class TokenInfoDTO {

    /** Spring Authorization Server 内部生成的 UUID，非 JWT 的 jti */
    private String authorizationId;

    /** client_credentials 场景下即 client_id */
    private String principalName;

    private String registeredClientId;
    private String grantType;
    private Set<String> authorizedScopes;

    private Instant issuedAt;
    private Instant expiresAt;

    /** true = 未过期，false = 已过期 */
    private boolean active;

    /** JWT 字符串（eyJhbGci...） */
    private String tokenValue;

    /** JWT payload claims（sub / iss / exp / jti 等），仅 findByTokenValue 返回 */
    private Map<String, Object> claims;
}