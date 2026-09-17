package com.verifipay.admin.dashboard.service;

import com.verifipay.admin.dashboard.dto.DashboardResponse;
import com.verifipay.branch.repository.BranchRepository;
import com.verifipay.common.enums.UserRole;
import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.security.CurrentUserService;
import com.verifipay.user.entity.User;
import com.verifipay.user.repository.UserRepository;
import com.verifipay.verification.dto.VerificationResponse;
import com.verifipay.verification.entity.Verification;
import com.verifipay.verification.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.verifipay.verification.mapper.VerificationMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VerificationRepository verificationRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final VerificationMapper verificationMapper;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {

        User admin = currentUserService.getCurrentUser();

        Long companyId = admin.getCompany().getId();

        LocalDate today = LocalDate.now();

        LocalDateTime start =
                today.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1).atStartOfDay();

        long total =
                verificationRepository
                        .countByCompanyIdAndCreatedAtBetween(
                                companyId,
                                start,
                                end
                        );

        long detected =
                verificationRepository
                        .countByCompanyIdAndStatusAndCreatedAtBetween(
                                companyId,
                                VerificationStatus.DETECTED,
                                start,
                                end
                        );

        long notFound =
                verificationRepository
                        .countByCompanyIdAndStatusAndCreatedAtBetween(
                                companyId,
                                VerificationStatus.NOT_FOUND,
                                start,
                                end
                        );

        long ambiguous =
                verificationRepository
                        .countByCompanyIdAndStatusAndCreatedAtBetween(
                                companyId,
                                VerificationStatus.AMBIGUOUS,
                                start,
                                end
                        );

        long activeBranches =
                branchRepository
                        .countByCompanyIdAndActiveTrue(companyId);

        long activeCashiers =
                userRepository
                        .countByCompanyIdAndRoleAndActiveTrue(
                                companyId,
                                UserRole.CASHIER
                        );

        BigDecimal totalAmount =
                verificationRepository
                        .sumAmountByCompanyAndStatusAndCreatedAtBetween(
                                companyId,
                                VerificationStatus.DETECTED,
                                start,
                                end
                        );

        List<VerificationResponse> recent =
                verificationRepository
                        .findTop10ByCompanyIdOrderByCreatedAtDesc(companyId)
                        .stream()
                        .map(verificationMapper::toResponse)
                        .toList();

        return new DashboardResponse(
                total,
                detected,
                notFound,
                ambiguous,
                activeBranches,
                activeCashiers,
                totalAmount,
                recent
        );
    }

}