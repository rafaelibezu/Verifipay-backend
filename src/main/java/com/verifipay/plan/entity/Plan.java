package com.verifipay.plan.entity;

import com.verifipay.common.enums.BillingInterval;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "plans")
@Getter
@NoArgsConstructor
public class Plan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false, length = 30)
    private String code;
    @Setter @Column(nullable = false, length = 150)
    private String name;
    @Setter @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Setter @Column(nullable = false, length = 3)
    private String currency;
    @Setter @Enumerated(EnumType.STRING) @Column(name = "billing_interval", nullable = false, length = 20)
    private BillingInterval billingInterval;
    @Setter @Column(name = "max_cashiers", nullable = false)
    private Integer maxCashiers;
    @Setter @Column(name = "max_receiver_devices", nullable = false)
    private Integer maxReceiverDevices;
    @Setter @Column(nullable = false)
    private Boolean active;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Plan(String code) { this.code = code; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
