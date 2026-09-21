package com.verifipay.paymentrequest.service;

import com.verifipay.common.enums.UserRole;
import com.verifipay.company.entity.Company;
import com.verifipay.company.repository.CompanyRepository;
import com.verifipay.paymentrequest.dto.CreatePaymentRequestRequest;
import com.verifipay.paymentrequest.repository.PaymentRequestRepository;
import com.verifipay.security.CurrentUserService;
import com.verifipay.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentRequestServiceTest {
    @Test
    void expirationUsesConfiguredMinutesAndClock() {
        var repository = mock(PaymentRequestRepository.class);
        var currentUser = mock(CurrentUserService.class);
        var company = Company.builder().id(1L).active(true).build();
        var companies = mock(CompanyRepository.class);
        when(companies.findForUpdate(1L)).thenReturn(Optional.of(company));
        var cashier = User.builder().id(2L).company(company).role(UserRole.CASHIER).active(true).build();
        when(currentUser.getCurrentUser()).thenReturn(cashier);
        when(currentUser.requireRole(UserRole.CASHIER)).thenReturn(cashier);
        when(repository.findByCashierIdAndClientRequestId(2L, "retry")).thenReturn(Optional.empty());
        var now = Instant.parse("2026-09-20T23:59:59Z");
        var service = new PaymentRequestService(repository, currentUser, companies, 7, Clock.fixed(now, ZoneOffset.UTC));
        // Stop after the insert; inspect the timestamps passed to persistence.
        assertThrows(IllegalStateException.class,
                () -> service.create(new CreatePaymentRequestRequest(new BigDecimal("80.00"), "retry")));
        verify(repository).insertIfAbsent(1L, null, 2L, new BigDecimal("80.00"), "retry", now, now.plusSeconds(420));
    }

    @Test
    void rejectsNonPositiveExpirationConfiguration() {
        for (long minutes : new long[]{0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> new PaymentRequestService(
                    mock(PaymentRequestRepository.class), mock(CurrentUserService.class), mock(CompanyRepository.class), minutes));
        }
    }
}
