package com.verifipay.auth.controller;

import com.verifipay.auth.dto.CurrentUserResponse;
import com.verifipay.auth.dto.LoginRequest;
import com.verifipay.auth.dto.LoginResponse;
import com.verifipay.auth.service.AuthService;
import com.verifipay.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Inicio de sesión correcto",
                        authService.login(request)
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Usuario autenticado obtenido correctamente",
                        authService.me()
                )
        );
    }
}