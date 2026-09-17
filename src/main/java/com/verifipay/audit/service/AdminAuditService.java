package com.verifipay.audit.service;

import com.verifipay.audit.dto.AuditLogResponse;
import com.verifipay.audit.entity.AuditLog;
import com.verifipay.audit.mapper.AuditLogMapper;
import com.verifipay.audit.repository.AuditLogRepository;
import com.verifipay.common.api.PageResponse;
import com.verifipay.security.CurrentUserService;
import com.verifipay.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findAll(
            int page,
            int size
    ) {

        User admin =
                currentUserService.getCurrentUser();

        Long companyId =
                admin.getCompany().getId();

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<AuditLog> result =
                auditLogRepository
                        .findAllByCompanyId(
                                companyId,
                                pageable
                        );

        Page<AuditLogResponse> mapped =
                result.map(
                        auditLogMapper::toResponse
                );

        return PageResponse.from(mapped);
    }
}