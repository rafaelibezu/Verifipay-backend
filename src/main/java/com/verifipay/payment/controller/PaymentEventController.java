package com.verifipay.payment.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.payment.dto.CreateMockPaymentEventRequest;
import com.verifipay.payment.dto.PaymentEventResponse;
import com.verifipay.payment.service.PaymentEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-events")
@RequiredArgsConstructor
public class PaymentEventController {

    private final PaymentEventService paymentEventService;

    @PostMapping("/mock/yape")
    public ResponseEntity<ApiResponse<PaymentEventResponse>> createMockYapeEvent(
            @Valid @RequestBody CreateMockPaymentEventRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Evento Yape simulado correctamente",
                                paymentEventService
                                        .createMockYapeEvent(request)
                        )
                );
    }
}