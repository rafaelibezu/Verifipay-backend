package com.verifipay.config;

import com.verifipay.common.enums.UserRole;
import com.verifipay.user.entity.User;
import com.verifipay.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class PlatformAdminBootstrapTest {

    private static final String RAW_PASSWORD = "VeryStrongPassword1";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Test
    void disabledDoesNotCreateBootstrapBean() {
        new ApplicationContextRunner()
                .withUserConfiguration(BootstrapTestConfig.class, PlatformAdminBootstrap.class)
                .withPropertyValues("platform.bootstrap.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PlatformAdminBootstrap.class);
                    verifyNoInteractions(context.getBean(UserRepository.class));
                });
    }

    @Test
    void enabledWithoutPlatformAdminCreatesOne() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = encoder();
        PlatformBootstrapLock lock = mock(PlatformBootstrapLock.class);
        when(users.countByRole(UserRole.PLATFORM_ADMIN)).thenReturn(0L);
        when(users.existsByEmailIgnoreCase("admin@verifipay.test")).thenReturn(false);

        bootstrap(users, encoder, lock).run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(lock).lock();
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.PLATFORM_ADMIN);
        assertThat(saved.getName()).isEqualTo("Admin VerifiPay");
        assertThat(saved.getEmail()).isEqualTo("admin@verifipay.test");
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    void createdPlatformAdminHasNoCompanyOrBranchAndEncodedPassword() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = encoder();
        when(users.countByRole(UserRole.PLATFORM_ADMIN)).thenReturn(0L);
        when(users.existsByEmailIgnoreCase(any())).thenReturn(false);

        bootstrap(users, encoder, mock(PlatformBootstrapLock.class)).run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getCompany()).isNull();
        assertThat(saved.getBranch()).isNull();
        assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(saved.getPassword()).isNotEqualTo(RAW_PASSWORD);
        verify(encoder).encode(RAW_PASSWORD);
    }

    @Test
    void enabledWithExistingPlatformAdminDoesNotCreateSecond(CapturedOutput output) {
        UserRepository users = mock(UserRepository.class);
        when(users.countByRole(UserRole.PLATFORM_ADMIN)).thenReturn(1L);

        bootstrap(users, encoder(), mock(PlatformBootstrapLock.class)).run();

        verify(users, never()).save(any());
        assertThat(output).contains("Bootstrap de PLATFORM_ADMIN omitido");
        assertThat(output).doesNotContain(RAW_PASSWORD);
        assertThat(output).doesNotContain(ENCODED_PASSWORD);
    }

    @Test
    void restartIsIdempotentAndLeavesOnlyOneCreatedAutomatically() {
        UserRepository users = mock(UserRepository.class);
        when(users.countByRole(UserRole.PLATFORM_ADMIN)).thenReturn(0L, 1L);
        when(users.existsByEmailIgnoreCase(any())).thenReturn(false);
        PlatformAdminBootstrap bootstrap = bootstrap(users, encoder(), mock(PlatformBootstrapLock.class));

        bootstrap.run();
        bootstrap.run();

        verify(users, times(1)).save(any());
    }

    @Test
    void invalidConfigurationFailsAndDoesNotCreateUser() {
        UserRepository users = mock(UserRepository.class);
        when(users.countByRole(UserRole.PLATFORM_ADMIN)).thenReturn(0L);
        PlatformAdminBootstrap bootstrap = bootstrap(users, encoder(), mock(PlatformBootstrapLock.class));
        ReflectionTestUtils.setField(bootstrap, "email", "not-an-email");

        assertThrows(IllegalStateException.class, bootstrap::run);

        verify(users, never()).save(any());
    }

    private PlatformAdminBootstrap bootstrap(
            UserRepository users,
            PasswordEncoder encoder,
            PlatformBootstrapLock lock
    ) {
        PlatformAdminBootstrap bootstrap = new PlatformAdminBootstrap(users, encoder, lock);
        ReflectionTestUtils.setField(bootstrap, "email", " Admin@VerifiPay.Test ");
        ReflectionTestUtils.setField(bootstrap, "name", " Admin VerifiPay ");
        ReflectionTestUtils.setField(bootstrap, "password", RAW_PASSWORD);
        return bootstrap;
    }

    private PasswordEncoder encoder() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        return encoder;
    }

    @Configuration(proxyBeanMethods = false)
    static class BootstrapTestConfig {
        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }

        @Bean
        PlatformBootstrapLock platformBootstrapLock() {
            return mock(PlatformBootstrapLock.class);
        }
    }
}
