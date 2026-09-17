package com.verifipay.payment.repository;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.payment.entity.PaymentEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentEventRepository
        extends JpaRepository<PaymentEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PaymentEvent>
    findAllByCompanyIdAndBranchIdAndMethodAndAmountAndConsumedFalseAndOccurredAtAfterOrderByOccurredAtDesc(
            Long companyId,
            Long branchId,
            PaymentMethod method,
            BigDecimal amount,
            LocalDateTime occurredAfter
    );
}