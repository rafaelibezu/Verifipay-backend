package com.verifipay.audit.controller;

import com.verifipay.audit.dto.AuditLogResponse;
import com.verifipay.audit.service.AdminAuditService;
import com.verifipay.common.api.ApiResponse;
import com.verifipay.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<AuditLogResponse>>
            > findAll(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Auditoría obtenida correctamente",
                        adminAuditService.findAll(
                                page,
                                size
                        )
                )
        );
    }
}