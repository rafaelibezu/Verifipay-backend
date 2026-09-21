package com.verifipay.subscription.entity;

import com.verifipay.common.enums.BillingInterval;
import com.verifipay.common.enums.SubscriptionStatus;
import com.verifipay.company.entity.Company;
import com.verifipay.plan.entity.Plan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private Plan plan;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private SubscriptionStatus status;
    @Column(name = "contracted_price", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal contractedPrice;
    @Column(nullable = false, updatable = false, length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, updatable = false, length = 20)
    private BillingInterval billingInterval;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @Version
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Subscription(Company company, Plan plan) {
        this.company = company;
        this.plan = plan;
        this.status = SubscriptionStatus.PENDING_PAYMENT;
        this.contractedPrice = plan.getPrice();
        this.currency = plan.getCurrency();
        this.billingInterval = plan.getBillingInterval();
    }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
