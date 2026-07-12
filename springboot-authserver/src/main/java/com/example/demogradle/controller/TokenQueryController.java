package com.example.demogradle.controller;

import com.example.demogradle.dto.TokenInfoDTO;
import com.example.demogradle.service.TokenQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Token 查询接口，供 Postman 调试使用。
 * 已在 SecurityConfig 中将 /admin/** 加入 permitAll，无需登录即可访问。
 */
@RestController
@RequestMapping("/admin/tokens")
@RequiredArgsConstructor
public class TokenQueryController {

    private final TokenQueryService tokenQueryService;

    /**
     * 按 principal name 查询 token 列表（走 idx_oauth2_auth_principal 索引）。
     *
     * Postman: GET http://localhost:8080/auth/admin/tokens?principal=test-client
     */
    @GetMapping
    public ResponseEntity<List<TokenInfoDTO>> findByPrincipal(
            @RequestParam String principal) {
        return ResponseEntity.ok(tokenQueryService.findByPrincipal(principal));
    }

    /**
     * 按内部 authorization id 查询单条记录（走 OAuth2AuthorizationService.findById）。
     * id 是 Spring Authorization Server 内部生成的 UUID，可从 /admin/tokens?principal=xxx 的响应中获取。
     *
     * Postman: GET http://localhost:8080/auth/admin/tokens/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TokenInfoDTO> findById(@PathVariable String id) {
        TokenInfoDTO result = tokenQueryService.findById(id);
        return result != null
                ? ResponseEntity.ok(result)
                : ResponseEntity.notFound().build();
    }

    /**
     * 按 token 值查询单条记录，同时返回 JWT claims（走 OAuth2AuthorizationService API）。
     *
     * Postman: POST http://localhost:8080/auth/admin/tokens/lookup
     * Body (JSON): { "tokenValue": "eyJhbGci..." }
     */
    @PostMapping("/lookup")
    public ResponseEntity<TokenInfoDTO> findByTokenValue(
            @RequestBody Map<String, String> body) {
        String tokenValue = body.get("tokenValue");
        if (tokenValue == null || tokenValue.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        TokenInfoDTO result = tokenQueryService.findByTokenValue(tokenValue);
        return result != null
                ? ResponseEntity.ok(result)
                : ResponseEntity.notFound().build();
    }
}