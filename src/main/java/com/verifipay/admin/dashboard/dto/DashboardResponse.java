package com.verifipay.admin.dashboard.dto;

import com.verifipay.verification.dto.VerificationResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(

        long verificationsToday,
        long detectedToday,
        long notFoundToday,
        long ambiguousToday,

        long activeBranches,
        long activeCashiers,

        BigDecimal totalAmountDetectedToday,

        List<VerificationResponse> recentVerifications
) {
}