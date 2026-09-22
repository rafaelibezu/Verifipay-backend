package com.verifipay.billing.entity;

import com.verifipay.common.enums.PaymentOrderStatus;
import com.verifipay.common.enums.PaymentProvider;
import com.verifipay.company.entity.Company;
import com.verifipay.subscription.entity.Subscription;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_orders")
@Getter @NoArgsConstructor
public class PaymentOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "company_id", nullable = false) private Company company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subscription_id", nullable = false) private Subscription subscription;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentOrderStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentProvider provider;
    @Column(name = "external_reference", nullable = false, unique = true, length = 100) private String externalReference;
    @Column(name = "provider_transaction_id", length = 150) private String providerTransactionId;
    @Column(name = "idempotency_key", nullable = false, length = 100) private String idempotencyKey;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "failed_at") private Instant failedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "expires_at") private Instant expiresAt;
    @Version private Long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public PaymentOrder(Company company, Subscription subscription, String idempotencyKey, String externalReference, PaymentProvider provider) {
        this.company = company; this.subscription = subscription; this.amount = subscription.getContractedPrice();
        this.currency = subscription.getCurrency(); this.idempotencyKey = idempotencyKey; this.externalReference = externalReference;
        this.provider = provider; this.status = PaymentOrderStatus.PENDING;
    }
    public void markPaid(Instant now) { status = PaymentOrderStatus.PAID; paidAt = now; }
    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
