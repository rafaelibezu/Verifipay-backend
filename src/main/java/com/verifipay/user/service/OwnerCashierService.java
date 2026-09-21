package com.verifipay.user.service;

import com.verifipay.branch.repository.BranchRepository;
import com.verifipay.common.enums.UserRole;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.security.CurrentUserService;
import com.verifipay.user.dto.CreateCashierRequest;
import com.verifipay.user.dto.UpdateCashierRequest;
import com.verifipay.user.dto.UserResponse;
import com.verifipay.user.entity.User;
import com.verifipay.user.mapper.UserMapper;
import com.verifipay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerCashierService {
    private final UserRepository users; private final BranchRepository branches; private final CurrentUserService current;
    private final UserMapper mapper; private final PasswordEncoder encoder;
    private User owner() { return current.requireRole(UserRole.OWNER); }
    private User cashier(Long id, User owner) { return users.findById(id).filter(u -> u.getRole() == UserRole.CASHIER && u.getCompany().getId().equals(owner.getCompany().getId())).orElseThrow(() -> new ResourceNotFoundException("Cajero no encontrado")); }
    private com.verifipay.branch.entity.Branch branch(Long id, User owner) { return branches.findById(id).filter(b -> b.getCompany().getId().equals(owner.getCompany().getId())).orElseThrow(() -> new ResourceNotFoundException("Local no encontrado")); }
    @Transactional public UserResponse create(CreateCashierRequest request) { User owner=owner(); if(users.existsByEmailIgnoreCase(request.email())) throw new BusinessException("Ya existe un usuario registrado con este correo"); var branch=branch(request.branchId(),owner); var saved=users.save(User.builder().company(owner.getCompany()).branch(branch).name(request.name().trim()).email(request.email().trim().toLowerCase()).password(encoder.encode(request.password())).role(UserRole.CASHIER).active(true).build()); return mapper.toResponse(saved); }
    @Transactional(readOnly=true) public List<UserResponse> findAll(){User owner=owner();return users.findAllByCompanyId(owner.getCompany().getId()).stream().filter(u->u.getRole()==UserRole.CASHIER).map(mapper::toResponse).toList();}
    @Transactional(readOnly=true) public UserResponse find(Long id){return mapper.toResponse(cashier(id,owner()));}
    @Transactional public UserResponse update(Long id, UpdateCashierRequest request){User owner=owner();User cashier=cashier(id,owner);String email=request.email().trim().toLowerCase();if(users.existsByEmailIgnoreCaseAndIdNot(email,id))throw new BusinessException("Ya existe otro usuario registrado con este correo");cashier.setName(request.name().trim());cashier.setEmail(email);cashier.setBranch(branch(request.branchId(),owner));return mapper.toResponse(cashier);}
    @Transactional public UserResponse status(Long id, boolean active){User cashier=cashier(id,owner());cashier.setActive(active);return mapper.toResponse(cashier);}
}
