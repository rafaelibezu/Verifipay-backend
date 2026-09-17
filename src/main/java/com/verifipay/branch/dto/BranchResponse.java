package com.verifipay.branch.dto;

import java.time.LocalDateTime;

public record BranchResponse(

        Long id,
        Long companyId,
        String companyName,
        String name,
        String address,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}