package com.verifipay.audit.service;

import com.verifipay.audit.entity.AuditLog;
import com.verifipay.audit.repository.AuditLogRepository;
import com.verifipay.common.enums.AuditAction;
import com.verifipay.company.entity.Company;
import com.verifipay.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void register(
            Company company,
            User user,
            AuditAction action,
            String entity,
            Long entityId,
            String details
    ) {

        AuditLog auditLog = AuditLog.builder()
                .company(company)
                .user(user)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }
}