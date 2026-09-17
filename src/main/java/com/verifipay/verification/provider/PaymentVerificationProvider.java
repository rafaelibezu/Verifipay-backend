package com.verifipay.verification.provider;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.verification.dto.CreateVerificationRequest;
import com.verifipay.user.entity.User;

public interface PaymentVerificationProvider {

    PaymentMethod supportedMethod();

    VerificationResult verify(
            CreateVerificationRequest request,
            User cashier
    );
}