package com.verifipay.config;

import com.verifipay.common.enums.UserRole;
import com.verifipay.user.entity.User;
import com.verifipay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "platform.bootstrap.enabled", havingValue = "true")
public class PlatformAdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    @Value("${platform.bootstrap.email}") private String email;
    @Value("${platform.bootstrap.name}") private String name;
    @Value("${platform.bootstrap.password}") private String password;

    @Override
    @Transactional
    public void run(String... args) {

        if (email == null || email.isBlank()
                || name == null || name.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Las variables de bootstrap de PLATFORM_ADMIN son obligatorias"
            );
        }

        String normalized = email.trim().toLowerCase();

        var existing = users.findByEmailIgnoreCase(normalized);

        if (existing.isPresent()) {
            if (existing.get().getRole() == UserRole.PLATFORM_ADMIN) {
                return;
            }

            throw new IllegalStateException(
                    "El email configurado para bootstrap ya pertenece a otro usuario"
            );
        }

        users.save(
                User.builder()
                        .name(name.trim())
                        .email(normalized)
                        .password(passwordEncoder.encode(password))
                        .role(UserRole.PLATFORM_ADMIN)
                        .active(true)
                        .build()
        );
    }
}
