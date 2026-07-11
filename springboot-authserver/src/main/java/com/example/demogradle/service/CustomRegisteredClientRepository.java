package com.example.demogradle.service;

import com.example.demogradle.entity.SysOAuthClientPO; // 使用你的 PO 命名
import com.example.demogradle.repository.SysOAuthClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component // 交给 Spring 容器管理，SAS 会自动发现它
@RequiredArgsConstructor
public class CustomRegisteredClientRepository implements RegisteredClientRepository {

    private final SysOAuthClientRepository clientRepository;

    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Save operation is not supported");
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        log.debug("Finding client by ID: {}", id);
        return clientRepository.findById(Long.valueOf(id))
                .map(this::convertToRegisteredClient)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        log.debug("Finding client by clientId: {}", clientId);
        return clientRepository.findByClientId(clientId)
                .map(this::convertToRegisteredClient)
                .orElse(null);
    }

    private RegisteredClient convertToRegisteredClient(SysOAuthClientPO clientPO) {
        log.debug("Converting client PO to RegisteredClient: {}", clientPO.getClientId());

        Set<String> scopes;
        if (clientPO.getScopes() != null && !clientPO.getScopes().isEmpty()) {
            scopes = Arrays.stream(clientPO.getScopes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } else {
            scopes = new HashSet<>();
        }

        return RegisteredClient.withId(String.valueOf(clientPO.getId()))
                .clientId(clientPO.getClientId())
                .clientSecret(clientPO.getClientSecret())
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scopes(scopeSet -> scopeSet.addAll(scopes))
                // 修正点：去掉了 reuseRefreshTokens 和 refreshTokenTimeToLive
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(2))
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .build();
    }
}