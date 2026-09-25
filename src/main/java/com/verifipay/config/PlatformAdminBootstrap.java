package com.verifipay.config;

import com.verifipay.common.enums.UserRole;
import com.verifipay.user.entity.User;
import com.verifipay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "platform.bootstrap.enabled", havingValue = "true")
public class PlatformAdminBootstrap implements CommandLineRunner {
    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_EMAIL_LENGTH = 150;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 100;
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PlatformBootstrapLock bootstrapLock;

    @Value("${platform.bootstrap.email:}")
    private String email;

    @Value("${platform.bootstrap.name:}")
    private String name;

    @Value("${platform.bootstrap.password:}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) {
        bootstrapLock.lock();

        if (users.countByRole(UserRole.PLATFORM_ADMIN) > 0) {
            log.info("Bootstrap de PLATFORM_ADMIN omitido porque ya existe al menos un PLATFORM_ADMIN");
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        String normalizedName = normalizeName(name);
        validatePassword(password);

        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException("El email configurado para bootstrap ya pertenece a otro usuario");
        }

        users.save(
                User.builder()
                        .name(normalizedName)
                        .email(normalizedEmail)
                        .password(passwordEncoder.encode(password))
                        .role(UserRole.PLATFORM_ADMIN)
                        .company(null)
                        .branch(null)
                        .active(true)
                        .build()
        );
        log.info("PLATFORM_ADMIN inicial creado por bootstrap");
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("PLATFORM_BOOTSTRAP_EMAIL es obligatorio");
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.length() > MAX_EMAIL_LENGTH || !normalized.matches(EMAIL_PATTERN)) {
            throw new IllegalStateException("PLATFORM_BOOTSTRAP_EMAIL no es valido");
        }
        return normalized;
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("PLATFORM_BOOTSTRAP_NAME es obligatorio");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalStateException("PLATFORM_BOOTSTRAP_NAME no es valido");
        }
        return normalized;
    }

    private void validatePassword(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("PLATFORM_BOOTSTRAP_PASSWORD es obligatorio");
        }
        if (value.length() < MIN_PASSWORD_LENGTH || value.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalStateException("PLATFORM_BOOTSTRAP_PASSWORD no cumple la politica configurada");
        }
    }
}
