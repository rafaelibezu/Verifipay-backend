package com.verifipay.auth.dto;

public record LoginResponse(

        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        AuthUserResponse user

) {
}
