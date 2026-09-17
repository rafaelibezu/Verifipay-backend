package com.verifipay.user.service;

import com.verifipay.branch.entity.Branch;
import com.verifipay.branch.repository.BranchRepository;
import com.verifipay.common.enums.UserRole;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.company.entity.Company;
import com.verifipay.company.repository.CompanyRepository;
import com.verifipay.user.dto.CreateUserRequest;
import com.verifipay.user.dto.UserResponse;
import com.verifipay.user.entity.User;
import com.verifipay.user.mapper.UserMapper;
import com.verifipay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.verifipay.user.dto.UpdateUserRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(
                    "Ya existe un usuario registrado con este correo"
            );
        }

        Company company = companyRepository
                .findById(request.companyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Empresa no encontrada"
                        )
                );

        Branch branch = resolveBranch(request, company);

        String encodedPassword =
                passwordEncoder.encode(request.password());

        User user = userMapper.toEntity(
                request,
                company,
                branch,
                encodedPassword
        );

        User saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }

    private Branch resolveBranch(
            CreateUserRequest request,
            Company company
    ) {

        if (request.role() == UserRole.CASHIER &&
                request.branchId() == null) {

            throw new BusinessException(
                    "Un cajero debe estar asignado a un local"
            );
        }

        if (request.branchId() == null) {
            return null;
        }

        Branch branch = branchRepository
                .findById(request.branchId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Local no encontrado"
                        )
                );

        if (!branch.getCompany().getId()
                .equals(company.getId())) {

            throw new BusinessException(
                    "El local no pertenece a la empresa indicada"
            );
        }

        return branch;
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {

        User user = userRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado"
                        )
                );

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAllByCompany(Long companyId) {

        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException(
                    "Empresa no encontrada"
            );
        }

        return userRepository
                .findAllByCompanyId(companyId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAllByBranch(Long branchId) {

        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException(
                    "Local no encontrado"
            );
        }

        return userRepository
                .findAllByBranchId(branchId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse update(
            Long id,
            UpdateUserRequest request
    ) {

        User user = userRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado"
                        )
                );

        String normalizedEmail =
                request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(
                normalizedEmail,
                id
        )) {
            throw new BusinessException(
                    "Ya existe otro usuario registrado con este correo"
            );
        }

        Branch branch = null;

        if (request.role() == UserRole.CASHIER) {

            if (request.branchId() == null) {
                throw new BusinessException(
                        "Un cajero debe estar asignado a un local"
                );
            }

            branch = branchRepository
                    .findById(request.branchId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Local no encontrado"
                            )
                    );

            if (!branch.getCompany().getId()
                    .equals(user.getCompany().getId())) {

                throw new BusinessException(
                        "El local no pertenece a la empresa del usuario"
                );
            }
        }

        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setRole(request.role());
        user.setBranch(branch);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse changeStatus(
            Long id,
            boolean active
    ) {

        User user = userRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado"
                        )
                );

        user.setActive(active);

        return userMapper.toResponse(user);
    }
}