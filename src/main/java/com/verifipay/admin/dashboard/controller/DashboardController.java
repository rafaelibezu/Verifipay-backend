package com.verifipay.admin.dashboard.controller;

import com.verifipay.admin.dashboard.dto.DashboardResponse;
import com.verifipay.admin.dashboard.service.DashboardService;
import com.verifipay.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/owner/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dashboard obtenido correctamente",
                        dashboardService.getDashboard()
                )
        );
    }
}
