package com.verifipay.paymentrequest.repository;

import com.verifipay.paymentrequest.entity.PaymentRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from PaymentRequest p
            where p.company.id = :companyId and p.amount = :amount
              and p.createdAt <= :occurredAt and :occurredAt < p.expiresAt
              and (p.status = com.verifipay.common.enums.PaymentRequestStatus.WAITING
                   or (p.status = com.verifipay.common.enums.PaymentRequestStatus.CANCELLED
                       and p.cancelledAt is not null and :occurredAt < p.cancelledAt))
            order by p.id
            """)
    List<PaymentRequest> findCandidatesForUpdate(@Param("companyId") Long companyId,
            @Param("amount") BigDecimal amount, @Param("occurredAt") Instant occurredAt);

    Optional<PaymentRequest> findByCashierIdAndClientRequestId(Long cashierId, String clientRequestId);

    Page<PaymentRequest> findAllByCashierIdAndCompanyId(Long cashierId, Long companyId, Pageable pageable);

    Optional<PaymentRequest> findByIdAndCashierIdAndCompanyId(Long id, Long cashierId, Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentRequest p where p.id = :id and p.cashier.id = :cashierId and p.company.id = :companyId")
    Optional<PaymentRequest> findOwnedForUpdate(@Param("id") Long id,
            @Param("cashierId") Long cashierId, @Param("companyId") Long companyId);

    // The unique constraint arbitrates concurrent retries without aborting the transaction.
    @Modifying
    @Query(value = """
            INSERT INTO payment_requests
                (company_id, branch_id, cashier_id, amount, status, client_request_id, created_at, expires_at)
            VALUES (:companyId, :branchId, :cashierId, :amount, 'WAITING', :clientRequestId, :createdAt, :expiresAt)
            ON CONFLICT ON CONSTRAINT uq_payment_requests_cashier_client DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("companyId") Long companyId, @Param("branchId") Long branchId,
            @Param("cashierId") Long cashierId, @Param("amount") BigDecimal amount,
            @Param("clientRequestId") String clientRequestId,
            @Param("createdAt") Instant createdAt, @Param("expiresAt") Instant expiresAt);
}
