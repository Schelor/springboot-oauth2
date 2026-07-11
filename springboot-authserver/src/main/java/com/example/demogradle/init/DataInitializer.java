package com.example.demogradle.init;

import com.example.demogradle.entity.SysOAuthClientPO;
import com.example.demogradle.repository.SysOAuthClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysOAuthClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 检查是否已存在测试客户端
        if (clientRepository.findByClientId("test-client").isEmpty()) {
            log.info("Initializing test client data...");

            // 注意：client_secret 必须使用 BCrypt 加密后存储
            SysOAuthClientPO client = SysOAuthClientPO.builder()
                    .clientId("test-client")
                    .clientSecret(passwordEncoder.encode("test-secret"))  // BCrypt加密
                    .scopes("read,write,admin")
                    .description("测试客户端 - client_credentials模式")
                    .build();

            clientRepository.save(client);
            log.info("Test client created successfully!");
            log.info("Client ID: test-client");
            log.info("Client Secret: test-secret");
        } else {
            log.info("Test client already exists, skipping initialization.");
        }

        // 打印所有客户端信息
        clientRepository.findAll().forEach(c -> {
            log.info("Existing client: id={}, clientId={}, scopes={}",
                    c.getId(), c.getClientId(), c.getScopes());
        });
    }
}