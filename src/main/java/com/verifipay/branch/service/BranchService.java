package com.verifipay.branch.service;

import com.verifipay.branch.dto.BranchResponse;
import com.verifipay.branch.dto.CreateBranchRequest;
import com.verifipay.branch.entity.Branch;
import com.verifipay.branch.mapper.BranchMapper;
import com.verifipay.branch.repository.BranchRepository;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.company.entity.Company;
import com.verifipay.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.verifipay.branch.dto.UpdateBranchRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final BranchMapper branchMapper;

    @Transactional
    public BranchResponse create(CreateBranchRequest request) {

        Company company = companyRepository
                .findById(request.companyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Empresa no encontrada"
                        )
                );

        boolean exists =
                branchRepository
                        .existsByCompanyIdAndNameIgnoreCase(
                                company.getId(),
                                request.name().trim()
                        );

        if (exists) {
            throw new BusinessException(
                    "Ya existe un local con este nombre en la empresa"
            );
        }

        Branch branch =
                branchMapper.toEntity(request, company);

        Branch saved =
                branchRepository.save(branch);

        return branchMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BranchResponse findById(Long id) {

        Branch branch = branchRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Local no encontrado"
                        )
                );

        return branchMapper.toResponse(branch);
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> findAllByCompany(Long companyId) {

        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException(
                    "Empresa no encontrada"
            );
        }

        return branchRepository
                .findAllByCompanyId(companyId)
                .stream()
                .map(branchMapper::toResponse)
                .toList();
    }

    @Transactional
    public BranchResponse update(
            Long id,
            UpdateBranchRequest request
    ) {

        Branch branch = branchRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Local no encontrado"
                        )
                );

        boolean duplicate =
                branchRepository
                        .existsByCompanyIdAndNameIgnoreCase(
                                branch.getCompany().getId(),
                                request.name().trim()
                        );

        if (duplicate &&
                !branch.getName()
                        .equalsIgnoreCase(
                                request.name().trim()
                        )) {

            throw new BusinessException(
                    "Ya existe un local con este nombre"
            );
        }

        branch.setName(request.name().trim());
        branch.setAddress(
                request.address() != null
                        ? request.address().trim()
                        : null
        );

        return branchMapper.toResponse(branch);
    }

    @Transactional
    public BranchResponse changeStatus(
            Long id,
            boolean active
    ) {

        Branch branch = branchRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Local no encontrado"
                        )
                );

        branch.setActive(active);

        return branchMapper.toResponse(branch);
    }
}