package com.verifipay.security;

import com.verifipay.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${security.jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generateAccessToken(User user) {

        Instant now = Instant.now();

        JwtClaimsSet.Builder claimsBuilder =
                JwtClaimsSet.builder()
                        .issuer("verifipay")
                        .issuedAt(now)
                        .expiresAt(
                                now.plus(
                                        expirationMinutes,
                                        ChronoUnit.MINUTES
                                )
                        )
                        .subject(user.getEmail())
                        .claim("userId", user.getId())
                        .claim(
                                "companyId",
                                user.getCompany().getId()
                        )
                        .claim(
                                "role",
                                user.getRole().name()
                        );

        if (user.getBranch() != null) {
            claimsBuilder.claim(
                    "branchId",
                    user.getBranch().getId()
            );
        }

        JwtClaimsSet claims =
                claimsBuilder.build();

        JwsHeader header =
                JwsHeader
                        .with(MacAlgorithm.HS256)
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }
}