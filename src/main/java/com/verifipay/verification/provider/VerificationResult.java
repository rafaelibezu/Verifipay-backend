package com.verifipay.verification.provider;

import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.payment.entity.PaymentEvent;

public record VerificationResult(

        VerificationStatus status,
        PaymentEvent paymentEvent

) {
}