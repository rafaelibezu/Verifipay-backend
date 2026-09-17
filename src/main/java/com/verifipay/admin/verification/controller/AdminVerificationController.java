package com.verifipay.admin.verification.controller;

import com.verifipay.admin.verification.service.AdminVerificationService;
import com.verifipay.common.api.ApiResponse;
import com.verifipay.common.api.PageResponse;
import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.verification.dto.VerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/verifications")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<VerificationResponse>>
            > findAll(

            @RequestParam(required = false)
            Long branchId,

            @RequestParam(required = false)
            Long cashierId,

            @RequestParam(required = false)
            VerificationStatus status,

            @RequestParam(required = false)
            PaymentMethod method,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Verificaciones obtenidas correctamente",
                        adminVerificationService.findAll(
                                branchId,
                                cashierId,
                                status,
                                method,
                                from,
                                to,
                                page,
                                size
                        )
                )
        );
    }
}