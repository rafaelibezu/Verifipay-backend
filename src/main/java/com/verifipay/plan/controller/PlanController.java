package com.verifipay.plan.controller;
import com.verifipay.common.api.ApiResponse;
import com.verifipay.plan.dto.*;
import com.verifipay.plan.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/plans")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService plans;
    @PostMapping
    public ResponseEntity<ApiResponse<PlanResponse>> create(@Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(plans.create(request)));
    }
    @GetMapping
    public ApiResponse<List<PlanResponse>> list() { return ApiResponse.success(plans.findAll()); }
    @GetMapping("/{id}")
    public ApiResponse<PlanResponse> get(@PathVariable Long id) { return ApiResponse.success(plans.findById(id)); }
    @PatchMapping("/{id}")
    public ApiResponse<PlanResponse> update(@PathVariable Long id, @Valid @RequestBody UpdatePlanRequest request) {
        return ApiResponse.success(plans.update(id, request));
    }
}
