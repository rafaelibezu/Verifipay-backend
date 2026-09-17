package com.verifipay.company.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.company.dto.CompanyResponse;
import com.verifipay.company.dto.CreateCompanyRequest;
import com.verifipay.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> create(
            @Valid @RequestBody CreateCompanyRequest request
    ) {

        CompanyResponse company =
                companyService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Empresa creada correctamente",
                                company
                        )
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        companyService.findById(id)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> findAll() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        companyService.findAll()
                )
        );
    }
}