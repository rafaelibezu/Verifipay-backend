package com.verifipay.branch.service;

import com.verifipay.branch.dto.BranchResponse;
import com.verifipay.branch.dto.CreateBranchRequest;
import com.verifipay.branch.entity.Branch;
import com.verifipay.branch.mapper.BranchMapper;
import com.verifipay.branch.repository.BranchRepository;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.company.entity.Company;
import com.verifipay.security.CurrentUserService;
import com.verifipay.common.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.verifipay.branch.dto.UpdateBranchRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final CurrentUserService currentUserService;
    private final BranchMapper branchMapper;

    @Transactional
    public BranchResponse create(CreateBranchRequest request) {

        var owner = currentUserService.requireRole(UserRole.OWNER);
        Company company = owner.getCompany();

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

        var owner = currentUserService.requireRole(UserRole.OWNER);

        Branch branch = branchRepository
                .findById(id)
                .filter(value -> value.getCompany().getId().equals(owner.getCompany().getId())).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Local no encontrado"
                        )
                );

        return branchMapper.toResponse(branch);
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> findAll() {
        var owner = currentUserService.requireRole(UserRole.OWNER);

        return branchRepository
                .findAllByCompanyId(owner.getCompany().getId())
                .stream()
                .map(branchMapper::toResponse)
                .toList();
    }

    @Transactional
    public BranchResponse update(
            Long id,
            UpdateBranchRequest request
    ) {

        var owner = currentUserService.requireRole(UserRole.OWNER);
        Branch branch = branchRepository
                .findById(id)
                .filter(value -> value.getCompany().getId().equals(owner.getCompany().getId()))
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

        var owner = currentUserService.requireRole(UserRole.OWNER);
        Branch branch = branchRepository
                .findById(id)
                .filter(value -> value.getCompany().getId().equals(owner.getCompany().getId()))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Local no encontrado"
                        )
                );

        branch.setActive(active);

        return branchMapper.toResponse(branch);
    }
}
