package com.verifipay.paymentrequest.entity;

import com.verifipay.branch.entity.Branch;
import com.verifipay.common.enums.PaymentRequestStatus;
import com.verifipay.company.entity.Company;
import com.verifipay.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_requests", uniqueConstraints =
        @UniqueConstraint(name = "uq_payment_requests_cashier_client", columnNames = {"cashier_id", "client_request_id"}))
@Getter
@NoArgsConstructor
public class PaymentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentRequestStatus status;

    @Column(name = "client_request_id", nullable = false, length = 150)
    private String clientRequestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public void cancel(Instant now) {
        if (status != PaymentRequestStatus.WAITING) {
            throw new IllegalStateException("Solo se puede cancelar un cobro WAITING");
        }
        status = PaymentRequestStatus.CANCELLED;
        cancelledAt = java.util.Objects.requireNonNull(now);
    }

    public boolean isEligibleAt(Instant occurredAt) {
        boolean eligibleStatus = status == PaymentRequestStatus.WAITING
                || (status == PaymentRequestStatus.CANCELLED && cancelledAt != null
                    && occurredAt.isBefore(cancelledAt));
        return eligibleStatus && !occurredAt.isBefore(createdAt) && occurredAt.isBefore(expiresAt);
    }

    public void markPaid(Instant occurredAt, Instant now) {
        requireEligibleAt(occurredAt);
        status = PaymentRequestStatus.PAID;
        paidAt = java.util.Objects.requireNonNull(now);
    }

    public void markAmbiguous(Instant occurredAt) {
        requireEligibleAt(occurredAt);
        status = PaymentRequestStatus.AMBIGUOUS;
    }

    private void requireEligibleAt(Instant occurredAt) {
        if (!isEligibleAt(occurredAt)) {
            throw new IllegalStateException("El cobro no es elegible para este evento");
        }
    }
}
