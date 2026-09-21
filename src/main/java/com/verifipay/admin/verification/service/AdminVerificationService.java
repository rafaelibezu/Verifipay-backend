package com.verifipay.admin.verification.service;

import com.verifipay.common.api.PageResponse;
import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.security.CurrentUserService;
import com.verifipay.user.entity.User;
import com.verifipay.common.enums.UserRole;
import com.verifipay.verification.dto.VerificationResponse;
import com.verifipay.verification.entity.Verification;
import com.verifipay.verification.mapper.VerificationMapper;
import com.verifipay.verification.repository.VerificationRepository;
import com.verifipay.verification.specification.VerificationSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminVerificationService {

    private final VerificationRepository verificationRepository;
    private final VerificationMapper verificationMapper;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PageResponse<VerificationResponse> findAll(
            Long branchId,
            Long cashierId,
            VerificationStatus status,
            PaymentMethod method,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {

        User admin = currentUserService.requireRole(UserRole.OWNER);

        Long companyId =
                admin.getCompany().getId();

        LocalDateTime fromDateTime =
                from != null
                        ? from.atStartOfDay()
                        : null;

        LocalDateTime toDateTime =
                to != null
                        ? to.plusDays(1).atStartOfDay()
                        : null;

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Specification<Verification> specification =
                VerificationSpecifications
                        .belongsToCompany(companyId)

                        .and(
                                VerificationSpecifications
                                        .hasBranch(branchId)
                        )

                        .and(
                                VerificationSpecifications
                                        .hasCashier(cashierId)
                        )

                        .and(
                                VerificationSpecifications
                                        .hasStatus(status)
                        )

                        .and(
                                VerificationSpecifications
                                        .hasMethod(method)
                        )

                        .and(
                                VerificationSpecifications
                                        .createdFrom(fromDateTime)
                        )

                        .and(
                                VerificationSpecifications
                                        .createdBefore(toDateTime)
                        );

        Page<Verification> result =
                verificationRepository.findAll(
                        specification,
                        pageable
                );

        Page<VerificationResponse> mapped =
                result.map(
                        verificationMapper::toResponse
                );

        return PageResponse.from(mapped);
    }
}
