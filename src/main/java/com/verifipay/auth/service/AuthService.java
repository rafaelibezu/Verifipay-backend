package com.verifipay.auth.service;

import com.verifipay.auth.dto.AuthUserResponse;
import com.verifipay.auth.dto.LoginRequest;
import com.verifipay.auth.dto.LoginResponse;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.security.JwtService;
import com.verifipay.user.entity.User;
import com.verifipay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import com.verifipay.auth.dto.CurrentUserResponse;
import com.verifipay.security.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    @Value("${security.jwt.expiration-minutes}")
    private long expirationMinutes;

    public LoginResponse login(LoginRequest request) {

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim().toLowerCase(),
                            request.password()
                    )
            );

        } catch (AuthenticationException ex) {

            throw new BusinessException(
                    "Correo o contraseña incorrectos"
            );
        }

        User user = userRepository
                .findByEmailIgnoreCase(
                        request.email().trim()
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "Correo o contraseña incorrectos"
                        )
                );

        String accessToken =
                jwtService.generateAccessToken(user);

        Long companyId = user.getCompany() == null ? null : user.getCompany().getId();
        Long branchId = user.getBranch() == null ? null : user.getBranch().getId();

        AuthUserResponse userResponse =
                new AuthUserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        companyId,
                        branchId
                );

        return new LoginResponse(
                accessToken,
                "Bearer",
                expirationMinutes * 60,
                userResponse
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me() {

        User user =
                currentUserService.getCurrentUser();

        Long branchId = null;
        String branchName = null;

        if (user.getBranch() != null) {
            branchId = user.getBranch().getId();
            branchName = user.getBranch().getName();
        }

        return new CurrentUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),

                user.getCompany() == null ? null : user.getCompany().getId(),
                user.getCompany() == null ? null : user.getCompany().getName(),

                branchId,
                branchName
        );
    }
}
