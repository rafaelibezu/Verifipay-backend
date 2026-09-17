package com.verifipay.company.dto;

import java.time.LocalDateTime;

public record CompanyResponse(

        Long id,
        String name,
        String ruc,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}