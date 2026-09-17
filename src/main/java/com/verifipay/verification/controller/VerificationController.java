package com.verifipay.verification.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.verification.dto.CreateVerificationRequest;
import com.verifipay.verification.dto.VerificationResponse;
import com.verifipay.verification.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<VerificationResponse>> verify(
            @Valid @RequestBody CreateVerificationRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Verificación realizada",
                        verificationService.verify(request)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VerificationResponse>>> findMyHistory() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        verificationService.findMyHistory()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VerificationResponse>> findMyVerificationById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        verificationService
                                .findMyVerificationById(id)
                )
        );
    }
}