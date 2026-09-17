package com.verifipay.company.mapper;

import com.verifipay.company.dto.CompanyResponse;
import com.verifipay.company.dto.CreateCompanyRequest;
import com.verifipay.company.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public Company toEntity(CreateCompanyRequest request) {
        return Company.builder()
                .name(request.name().trim())
                .ruc(request.ruc())
                .active(true)
                .build();
    }

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getRuc(),
                company.getActive(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}