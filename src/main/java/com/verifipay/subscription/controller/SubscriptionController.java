package com.verifipay.subscription.controller;
import com.verifipay.common.api.ApiResponse;
import com.verifipay.subscription.dto.*;
import com.verifipay.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/companies/{companyId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptions;
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> create(@PathVariable Long companyId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(subscriptions.create(companyId, request)));
    }
    @GetMapping
    public ApiResponse<List<SubscriptionResponse>> history(@PathVariable Long companyId) {
        return ApiResponse.success(subscriptions.history(companyId));
    }
    @GetMapping("/current")
    public ApiResponse<SubscriptionResponse> current(@PathVariable Long companyId) {
        return ApiResponse.success(subscriptions.current(companyId));
    }
}
