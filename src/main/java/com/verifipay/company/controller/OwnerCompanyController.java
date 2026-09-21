package com.verifipay.company.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.company.dto.CompanyResponse;
import com.verifipay.company.mapper.CompanyMapper;
import com.verifipay.common.enums.UserRole;
import com.verifipay.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/company")
@RequiredArgsConstructor
public class OwnerCompanyController {
    private final CurrentUserService currentUserService;
    private final CompanyMapper companyMapper;

    @GetMapping
    public ApiResponse<CompanyResponse> mine() {
        return ApiResponse.success(companyMapper.toResponse(currentUserService.requireRole(UserRole.OWNER).getCompany()));
    }
}
