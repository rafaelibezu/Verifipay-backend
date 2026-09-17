package com.verifipay.verification.mapper;

import com.verifipay.payment.entity.PaymentEvent;
import com.verifipay.verification.dto.VerificationResponse;
import com.verifipay.verification.entity.Verification;
import org.springframework.stereotype.Component;

@Component
public class VerificationMapper {

    public VerificationResponse toResponse(
            Verification verification
    ) {

        PaymentEvent paymentEvent =
                verification.getPaymentEvent();

        return new VerificationResponse(
                verification.getId(),
                verification.getMethod(),
                verification.getAmount(),
                verification.getStatus(),

                paymentEvent != null
                        ? paymentEvent.getReference()
                        : verification.getReference(),

                paymentEvent != null
                        ? paymentEvent.getSecurityCode()
                        : verification.getSecurityCode(),

                paymentEvent != null
                        ? paymentEvent.getOccurredAt()
                        : null,

                verification.getVerifiedAt(),

                verification.getCashier().getName(),
                verification.getBranch().getName()
        );
    }
}