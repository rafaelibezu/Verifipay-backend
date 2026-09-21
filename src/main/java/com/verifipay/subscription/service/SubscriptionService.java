package com.verifipay.subscription.service;

import com.verifipay.common.enums.UserRole;
import com.verifipay.common.exception.*;
import com.verifipay.company.repository.CompanyRepository;
import com.verifipay.plan.repository.PlanRepository;
import com.verifipay.security.CurrentUserService;
import com.verifipay.subscription.dto.*;
import com.verifipay.subscription.entity.Subscription;
import com.verifipay.subscription.mapper.SubscriptionMapper;
import com.verifipay.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final CompanyRepository companies;
    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final SubscriptionMapper mapper;
    private final CurrentUserService currentUser;

    @Transactional
    public SubscriptionResponse create(Long companyId, CreateSubscriptionRequest request) {
        currentUser.requireRole(UserRole.PLATFORM_ADMIN);
        var company = companies.findForUpdate(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        if (subscriptions.existsByCompanyIdAndEndedAtIsNull(companyId))
            throw new BusinessException("La empresa ya tiene una Subscription vigente");
        var plan = plans.findById(request.planId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        if (!Boolean.TRUE.equals(plan.getActive())) throw new BusinessException("El Plan esta inactivo");
        return mapper.toResponse(subscriptions.saveAndFlush(new Subscription(company, plan)));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> history(Long companyId) {
        requireCompany(companyId);
        return subscriptions.findByCompanyIdOrderByCreatedAtDescIdDesc(companyId)
            .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse current(Long companyId) {
        requireCompany(companyId);
        return subscriptions.findByCompanyIdAndEndedAtIsNull(companyId).map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("La empresa no tiene una Subscription vigente"));
    }

    private void requireCompany(Long companyId) {
        currentUser.requireRole(UserRole.PLATFORM_ADMIN);
        if (!companies.existsById(companyId)) throw new ResourceNotFoundException("Empresa no encontrada");
    }
}
