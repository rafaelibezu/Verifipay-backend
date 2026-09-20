package com.verifipay.payment.repository;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.payment.entity.PaymentEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentEventRepository
        extends JpaRepository<PaymentEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PaymentEvent>
    findAllByCompanyIdAndBranchIdAndMethodAndAmountAndMatchingStatusIsNullAndConsumedFalseAndOccurredAtAfterOrderByOccurredAtDesc(
            Long companyId,
            Long branchId,
            PaymentMethod method,
            BigDecimal amount,
            LocalDateTime occurredAfter
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PaymentEvent>
    findAllByCompanyIdAndBranchIdAndMethodAndAmountAndSecurityCodeAndMatchingStatusIsNullAndConsumedFalseAndOccurredAtAfterOrderByOccurredAtDesc(
            Long companyId,
            Long branchId,
            PaymentMethod method,
            BigDecimal amount,
            String securityCode,
            LocalDateTime occurredAfter
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PaymentEvent e where e.sourceEventId = :sourceEventId")
    Optional<PaymentEvent> findBySourceEventIdForUpdate(@Param("sourceEventId") String sourceEventId);

    @Modifying
    @Query(value = """
            INSERT INTO payment_events
                (company_id, branch_id, method, amount, reference, security_code, occurred_at,
                 consumed, created_at, source_event_id, matching_status, occurred_at_instant)
            VALUES (:companyId, :branchId, 'YAPE', :amount, :sender, :securityCode, :occurredAt,
                    false, :createdAt, :sourceEventId, 'UNMATCHED', :occurredAtInstant)
            ON CONFLICT (source_event_id) WHERE source_event_id IS NOT NULL DO NOTHING
            """, nativeQuery = true)
    int insertDeviceEventIfAbsent(@Param("companyId") Long companyId, @Param("branchId") Long branchId,
            @Param("amount") BigDecimal amount, @Param("sender") String sender,
            @Param("securityCode") String securityCode, @Param("occurredAt") LocalDateTime occurredAt,
            @Param("createdAt") LocalDateTime createdAt, @Param("sourceEventId") String sourceEventId,
            @Param("occurredAtInstant") Instant occurredAtInstant);
}
