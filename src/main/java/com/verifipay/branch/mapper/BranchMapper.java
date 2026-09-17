package com.verifipay.branch.mapper;

import com.verifipay.branch.dto.BranchResponse;
import com.verifipay.branch.dto.CreateBranchRequest;
import com.verifipay.branch.entity.Branch;
import com.verifipay.company.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {

    public Branch toEntity(
            CreateBranchRequest request,
            Company company
    ) {
        return Branch.builder()
                .company(company)
                .name(request.name().trim())
                .address(
                        request.address() != null
                                ? request.address().trim()
                                : null
                )
                .active(true)
                .build();
    }

    public BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getCompany().getId(),
                branch.getCompany().getName(),
                branch.getName(),
                branch.getAddress(),
                branch.getActive(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}