package com.verifipay.auth.dto;

import com.verifipay.common.enums.UserRole;

public record AuthUserResponse(

        Long id,

        String name,
        String email,

        UserRole role,

        Long companyId,
        Long branchId

) {
}