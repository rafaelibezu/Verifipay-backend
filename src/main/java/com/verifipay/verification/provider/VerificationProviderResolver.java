package com.verifipay.verification.provider;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class VerificationProviderResolver {

    private final Map<
            PaymentMethod,
            PaymentVerificationProvider
            > providers;

    public VerificationProviderResolver(
            List<PaymentVerificationProvider> providers
    ) {

        this.providers =
                new EnumMap<>(PaymentMethod.class);

        providers.forEach(provider ->
                this.providers.put(
                        provider.supportedMethod(),
                        provider
                )
        );
    }

    public PaymentVerificationProvider resolve(
            PaymentMethod method
    ) {

        PaymentVerificationProvider provider =
                providers.get(method);

        if (provider == null) {

            throw new BusinessException(
                    "Método de pago no soportado"
            );
        }

        return provider;
    }
}