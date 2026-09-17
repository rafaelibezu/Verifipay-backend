package com.verifipay.audit.dto;

import com.verifipay.common.enums.AuditAction;

import java.time.LocalDateTime;

public record AuditLogResponse(

        Long id,

        Long userId,
        String userName,

        AuditAction action,

        String entity,
        Long entityId,

        String details,

        LocalDateTime createdAt

) {
}