package com.verifipay.auth.dto;

public record LoginResponse(

        String accessToken,
        String tokenType,
        Long expiresIn,
        AuthUserResponse user

) {
}