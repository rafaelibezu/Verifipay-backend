package com.verifipay.billing.controller;
import com.verifipay.billing.dto.PaymentOrderResponse;
import com.verifipay.billing.service.PaymentOrderService;
import com.verifipay.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
@ConditionalOnProperty(prefix = "billing.manual", name = "enabled", havingValue = "true")
@RequestMapping("/api/v1/platform/companies/{companyId}/subscriptions/{subscriptionId}/payment-orders")
public class ManualPaymentOrderController {
    private final PaymentOrderService orders;
    @PostMapping("/{paymentOrderId}/confirm-paid") public ApiResponse<PaymentOrderResponse> confirm(@PathVariable Long companyId, @PathVariable Long subscriptionId, @PathVariable Long paymentOrderId) {
        return ApiResponse.success(orders.confirmPaid(companyId, subscriptionId, paymentOrderId));
    }
}
