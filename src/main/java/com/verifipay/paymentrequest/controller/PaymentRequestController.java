package com.verifipay.paymentrequest.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.common.api.PageResponse;
import com.verifipay.paymentrequest.dto.CreatePaymentRequestRequest;
import com.verifipay.paymentrequest.dto.PaymentRequestResponse;
import com.verifipay.paymentrequest.service.PaymentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-requests")
@RequiredArgsConstructor
public class PaymentRequestController {
    private final PaymentRequestService service;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentRequestResponse>> create(
            @Valid @RequestBody CreatePaymentRequestRequest input) {
        var result = service.create(input);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(result.paymentRequest()));
    }

    @GetMapping
    public ApiResponse<PageResponse<PaymentRequestResponse>> findMine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.findMine(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentRequestResponse> findMineById(@PathVariable Long id) {
        return ApiResponse.success(service.findMineById(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<PaymentRequestResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(service.cancel(id));
    }
}
