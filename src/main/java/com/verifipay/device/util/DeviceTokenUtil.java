package com.verifipay.device.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class DeviceTokenUtil {

    private final SecureRandom secureRandom =
            new SecureRandom();

    public String generateToken() {

        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return HexFormat
                .of()
                .formatHex(bytes);
    }

    public String hash(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "No se pudo procesar el token del dispositivo",
                    ex
            );
        }
    }
}