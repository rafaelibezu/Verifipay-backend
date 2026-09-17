package com.verifipay.audit.mapper;

import com.verifipay.audit.dto.AuditLogResponse;
import com.verifipay.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(
            AuditLog auditLog
    ) {

        Long userId = null;
        String userName = "Sistema";

        if (auditLog.getUser() != null) {
            userId = auditLog.getUser().getId();
            userName = auditLog.getUser().getName();
        }

        return new AuditLogResponse(
                auditLog.getId(),
                userId,
                userName,
                auditLog.getAction(),
                auditLog.getEntity(),
                auditLog.getEntityId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}