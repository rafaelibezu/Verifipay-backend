package com.verifipay.payment.entity;

import com.verifipay.branch.entity.Branch;
import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.enums.PaymentEventStatus;
import com.verifipay.paymentrequest.entity.PaymentRequest;
import com.verifipay.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;

@Entity
@Table(name = "payment_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String reference;

    @Column(name = "security_code", length = 20)
    private String securityCode;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private Boolean consumed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "source_event_id", length = 150, unique = true)
    private String sourceEventId;

    // NULL identifies legacy events; they never enter automatic matching.
    @Enumerated(EnumType.STRING)
    @Column(name = "matching_status", length = 30)
    @Setter(AccessLevel.NONE)
    private PaymentEventStatus matchingStatus;

    @Column(name = "occurred_at_instant")
    private Instant occurredAtInstant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_request_id", unique = true)
    @Setter(AccessLevel.NONE)
    private PaymentRequest paymentRequest;

    public void match(PaymentRequest request) {
        requireUnmatched();
        paymentRequest = request;
        matchingStatus = PaymentEventStatus.MATCHED;
    }

    public void markAmbiguous() {
        requireUnmatched();
        matchingStatus = PaymentEventStatus.AMBIGUOUS;
    }

    private void requireUnmatched() {
        if (matchingStatus != PaymentEventStatus.UNMATCHED || paymentRequest != null) {
            throw new IllegalStateException("El evento no admite matching");
        }
    }

    @PrePersist
    public void prePersist() {

        this.consumed =
                this.consumed != null
                        ? this.consumed
                        : false;

        this.createdAt = LocalDateTime.now();
    }
}
