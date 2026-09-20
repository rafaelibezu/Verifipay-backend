package com.verifipay.payment.entity;

import com.verifipay.paymentrequest.entity.PaymentRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_match_candidates", uniqueConstraints = @UniqueConstraint(
        name = "uq_payment_match_candidates_pair", columnNames = {"payment_event_id", "payment_request_id"}))
@Getter
@NoArgsConstructor
public class PaymentMatchCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_event_id", nullable = false)
    private PaymentEvent paymentEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_request_id", nullable = false)
    private PaymentRequest paymentRequest;

    public PaymentMatchCandidate(PaymentEvent paymentEvent, PaymentRequest paymentRequest) {
        this.paymentEvent = paymentEvent;
        this.paymentRequest = paymentRequest;
    }
}
