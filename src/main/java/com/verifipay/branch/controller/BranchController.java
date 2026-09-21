package com.verifipay.branch.controller;

import com.verifipay.branch.dto.BranchResponse;
import com.verifipay.branch.dto.CreateBranchRequest;
import com.verifipay.branch.service.BranchService;
import com.verifipay.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.verifipay.branch.dto.UpdateBranchRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    public ResponseEntity<ApiResponse<BranchResponse>> create(
            @Valid @RequestBody CreateBranchRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Local creado correctamente",
                                branchService.create(request)
                        )
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        branchService.findById(id)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponse>>> findAll() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        branchService.findAll()
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBranchRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Local actualizado correctamente",
                        branchService.update(id, request)
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BranchResponse>> changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        active
                                ? "Local activado correctamente"
                                : "Local desactivado correctamente",
                        branchService.changeStatus(id, active)
                )
        );
    }
}
