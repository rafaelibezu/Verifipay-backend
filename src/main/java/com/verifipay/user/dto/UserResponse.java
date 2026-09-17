package com.verifipay.user.dto;

import com.verifipay.common.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(

        Long id,

        Long companyId,
        String companyName,

        Long branchId,
        String branchName,

        String name,
        String email,

        UserRole role,
        Boolean active,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}