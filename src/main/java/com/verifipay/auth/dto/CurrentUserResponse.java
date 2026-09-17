package com.verifipay.auth.dto;

import com.verifipay.common.enums.UserRole;

public record CurrentUserResponse(

        Long id,
        String name,
        String email,

        UserRole role,

        Long companyId,
        String companyName,

        Long branchId,
        String branchName

) {
}