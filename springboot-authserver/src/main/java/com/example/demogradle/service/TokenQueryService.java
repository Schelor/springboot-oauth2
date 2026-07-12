package com.example.demogradle.service;

import com.example.demogradle.dto.TokenInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenQueryService {

    /** Spring Authorization Server 提供的服务，支持 findById / findByToken */
    private final OAuth2AuthorizationService authorizationService;

    /**
     * OAuth2AuthorizationService 没有 findByPrincipal 方法，
     * 直接用 JdbcTemplate 按 principal_name 索引列查询
     */
    private final JdbcTemplate jdbcTemplate;

    // ─────────────────────────────────────────────────────────
    // 方式1：通过 OAuth2AuthorizationService API 查
    // ─────────────────────────────────────────────────────────

    /**
     * 按内部 authorization id 查询（Spring Authorization Server 内部 UUID，非 JWT jti）。
     *
     * @return 找不到时返回 null
     */
    public TokenInfoDTO findById(String id) {
        OAuth2Authorization auth = authorizationService.findById(id);
        if (auth == null) {
            log.debug("No authorization found for id: {}", id);
            return null;
        }
        return toDTO(auth);
    }

    /**
     * 按 JWT token 字符串查询。
     * 内部调用 JdbcOAuth2AuthorizationService，它会扫描 access_token_value 列匹配。
     *
     * @return 找不到时返回 null
     */
    public TokenInfoDTO findByTokenValue(String tokenValue) {
        OAuth2Authorization auth = authorizationService.findByToken(
                tokenValue, OAuth2TokenType.ACCESS_TOKEN);
        if (auth == null) {
            log.debug("No authorization found for token: {}...", tokenValue.substring(0, 20));
            return null;
        }
        return toDTO(auth);
    }

    // ─────────────────────────────────────────────────────────
    // 方式2：按 principal_name 查（JdbcTemplate + 索引列）
    // ─────────────────────────────────────────────────────────

    /**
     * 按 principal name 查询所有 token 记录，按签发时间倒序。
     * client_credentials 场景下 principal_name = client_id。
     */
    public List<TokenInfoDTO> findByPrincipal(String principalName) {
        String sql = """
                SELECT id, principal_name, registered_client_id,
                       authorization_grant_type,
                       access_token_value, access_token_issued_at,
                       access_token_expires_at, access_token_scopes
                FROM   oauth2_authorization
                WHERE  principal_name = ?
                ORDER  BY access_token_issued_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp issuedTs  = rs.getTimestamp("access_token_issued_at");
            Timestamp expiresTs = rs.getTimestamp("access_token_expires_at");
            Instant   expiresAt = expiresTs != null ? expiresTs.toInstant() : null;

            String scopesRaw = rs.getString("access_token_scopes");
            Set<String> scopes = (scopesRaw != null && !scopesRaw.isBlank())
                    ? new HashSet<>(Arrays.asList(scopesRaw.split("\\s+")))
                    : Set.of();

            return TokenInfoDTO.builder()
                    .authorizationId(rs.getString("id"))
                    .principalName(rs.getString("principal_name"))
                    .registeredClientId(rs.getString("registered_client_id"))
                    .grantType(rs.getString("authorization_grant_type"))
                    .authorizedScopes(scopes)
                    .issuedAt(issuedTs != null ? issuedTs.toInstant() : null)
                    .expiresAt(expiresAt)
                    .active(expiresAt != null && expiresAt.isAfter(Instant.now()))
                    .tokenValue(rs.getString("access_token_value"))
                    // claims 不在 SQL 里，findByPrincipal 不返回（避免解析 NVARCHAR(MAX) metadata 列）
                    .build();
        }, principalName);
    }

    // ─────────────────────────────────────────────────────────
    // 私有工具
    // ─────────────────────────────────────────────────────────

    private TokenInfoDTO toDTO(OAuth2Authorization auth) {
        OAuth2Authorization.Token<OAuth2AccessToken> tokenObj = auth.getAccessToken();
        OAuth2AccessToken accessToken = tokenObj.getToken();
        Instant expiresAt = accessToken.getExpiresAt();

        // getClaims() 从 token metadata 中取出 JWT payload claims（sub/iss/exp/jti 等）
        Map<String, Object> claims = tokenObj.getClaims();

        return TokenInfoDTO.builder()
                .authorizationId(auth.getId())
                .principalName(auth.getPrincipalName())
                .registeredClientId(auth.getRegisteredClientId())
                .grantType(auth.getAuthorizationGrantType().getValue())
                .authorizedScopes(accessToken.getScopes())
                .issuedAt(accessToken.getIssuedAt())
                .expiresAt(expiresAt)
                .active(expiresAt != null && expiresAt.isAfter(Instant.now()))
                .tokenValue(accessToken.getTokenValue())
                .claims(claims)
                .build();
    }
}