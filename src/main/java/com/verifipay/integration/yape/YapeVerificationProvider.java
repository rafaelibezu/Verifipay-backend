package com.verifipay.integration.yape;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.payment.entity.PaymentEvent;
import com.verifipay.payment.repository.PaymentEventRepository;
import com.verifipay.user.entity.User;
import com.verifipay.verification.dto.CreateVerificationRequest;
import com.verifipay.verification.provider.PaymentVerificationProvider;
import com.verifipay.verification.provider.VerificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class YapeVerificationProvider
        implements PaymentVerificationProvider {

    private static final long SEARCH_WINDOW_MINUTES = 5;

    private final PaymentEventRepository paymentEventRepository;

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.YAPE;
    }

    @Override
    public VerificationResult verify(
            CreateVerificationRequest request,
            User cashier
    ) {

        LocalDateTime from =
                LocalDateTime.now()
                        .minusMinutes(
                                SEARCH_WINDOW_MINUTES
                        );

        List<PaymentEvent> candidates;

        String securityCode =
                normalizeSecurityCode(
                        request.securityCode()
                );

        /*
         * Si tenemos código de seguridad,
         * PostgreSQL busca directamente
         * monto + código.
         */
        if (securityCode != null) {

            candidates =
                    paymentEventRepository
                            .findAllByCompanyIdAndBranchIdAndMethodAndAmountAndSecurityCodeAndConsumedFalseAndOccurredAtAfterOrderByOccurredAtDesc(
                                    cashier.getCompany().getId(),
                                    cashier.getBranch().getId(),
                                    PaymentMethod.YAPE,
                                    request.amount(),
                                    securityCode,
                                    from
                            );

        } else {

            /*
             * Sin código buscamos por monto.
             *
             * Si existen varios pagos iguales,
             * el resultado será AMBIGUOUS.
             */
            candidates =
                    paymentEventRepository
                            .findAllByCompanyIdAndBranchIdAndMethodAndAmountAndConsumedFalseAndOccurredAtAfterOrderByOccurredAtDesc(
                                    cashier.getCompany().getId(),
                                    cashier.getBranch().getId(),
                                    PaymentMethod.YAPE,
                                    request.amount(),
                                    from
                            );
        }

        if (candidates.isEmpty()) {

            return new VerificationResult(
                    VerificationStatus.NOT_FOUND,
                    null
            );
        }

        if (candidates.size() > 1) {

            return new VerificationResult(
                    VerificationStatus.AMBIGUOUS,
                    null
            );
        }

        return new VerificationResult(
                VerificationStatus.DETECTED,
                candidates.getFirst()
        );
    }

    private String normalizeSecurityCode(
            String securityCode
    ) {

        if (securityCode == null) {
            return null;
        }

        String normalized =
                securityCode.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}