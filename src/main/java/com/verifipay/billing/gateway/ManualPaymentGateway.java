package com.verifipay.billing.gateway;
import com.verifipay.common.enums.PaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(prefix = "billing.manual", name = "enabled", havingValue = "true")
public class ManualPaymentGateway implements PaymentGateway { public PaymentProvider provider() { return PaymentProvider.MANUAL; } }
