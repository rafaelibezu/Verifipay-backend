package com.verifipay.billing.controller;
import com.verifipay.billing.dto.PaymentOrderResponse;
import com.verifipay.billing.service.PaymentOrderService;
import com.verifipay.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/platform/companies/{companyId}/subscriptions/{subscriptionId}/payment-orders")
public class PaymentOrderController {
    private final PaymentOrderService orders;
    @PostMapping public ResponseEntity<ApiResponse<PaymentOrderResponse>> create(@PathVariable Long companyId, @PathVariable Long subscriptionId, @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orders.create(companyId, subscriptionId, key)));
    }
}
