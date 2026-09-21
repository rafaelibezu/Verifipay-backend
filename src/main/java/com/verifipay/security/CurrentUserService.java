package com.verifipay.security;

import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.user.entity.User;
import com.verifipay.user.repository.UserRepository;
import com.verifipay.common.enums.UserRole;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {

        if (!(SecurityContextHolder
                .getContext()
                .getAuthentication()
                instanceof JwtAuthenticationToken authentication)) {

            throw new IllegalStateException(
                    "Usuario no autenticado"
            );
        }

        Long userId =
                authentication
                        .getToken()
                        .getClaim("userId");

        return userRepository
                .findByIdWithTenantContext(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario autenticado no encontrado"
                        )
                );
    }

    public User requireRole(UserRole role) {
        User user = getCurrentUser();
        if (user.getRole() != role || !Boolean.TRUE.equals(user.getActive())) {
            throw new AccessDeniedException("No tienes permisos para realizar esta operacion");
        }
        return user;
    }
}
