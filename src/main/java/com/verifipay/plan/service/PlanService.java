package com.verifipay.plan.service;

import com.verifipay.common.enums.UserRole;
import com.verifipay.common.exception.*;
import com.verifipay.plan.dto.*;
import com.verifipay.plan.entity.Plan;
import com.verifipay.plan.mapper.PlanMapper;
import com.verifipay.plan.repository.PlanRepository;
import com.verifipay.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final PlanRepository plans;
    private final PlanMapper mapper;
    private final CurrentUserService currentUser;

    @Transactional
    public PlanResponse create(CreatePlanRequest request) {
        currentUser.requireRole(UserRole.PLATFORM_ADMIN);
        if (plans.existsByCode(request.code())) throw new BusinessException("El code del Plan ya existe");
        Plan plan = new Plan(request.code());
        plan.setName(request.name());
        plan.setPrice(request.price());
        plan.setCurrency(request.currency());
        plan.setBillingInterval(request.billingInterval());
        plan.setMaxCashiers(request.maxCashiers());
        plan.setMaxReceiverDevices(request.maxReceiverDevices());
        plan.setActive(request.active());
        try {
            return mapper.toResponse(plans.saveAndFlush(plan));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("El Plan no cumple las restricciones o su code ya existe");
        }
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findAll() {
        currentUser.requireRole(UserRole.PLATFORM_ADMIN);
        return plans.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse findById(Long id) {
        currentUser.requireRole(UserRole.PLATFORM_ADMIN);
        return mapper.toResponse(required(id));
    }

    @Transactional
    public PlanResponse update(Long id, UpdatePlanRequest request) {
        currentUser.requireRole(UserRole.PLATFORM_ADMIN);
        Plan plan = required(id);
        if (request.name() != null) plan.setName(request.name());
        if (request.price() != null) plan.setPrice(request.price());
        if (request.currency() != null) plan.setCurrency(request.currency());
        if (request.billingInterval() != null) plan.setBillingInterval(request.billingInterval());
        if (request.maxCashiers() != null) plan.setMaxCashiers(request.maxCashiers());
        if (request.maxReceiverDevices() != null) plan.setMaxReceiverDevices(request.maxReceiverDevices());
        if (request.active() != null) plan.setActive(request.active());
        return mapper.toResponse(plans.saveAndFlush(plan));
    }

    private Plan required(Long id) {
        return plans.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }
}
