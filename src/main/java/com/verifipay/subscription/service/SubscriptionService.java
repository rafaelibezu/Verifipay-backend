package com.verifipay.subscription.service;

import com.verifipay.common.enums.UserRole;
import com.verifipay.common.enums.SubscriptionStatus;
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
import java.time.Instant;

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

    @Transactional public SubscriptionResponse activate(Long companyId, Long subscriptionId) { return transition(companyId, subscriptionId, SubscriptionStatus.ACTIVE); }
    @Transactional public SubscriptionResponse markPastDue(Long companyId, Long subscriptionId) { return transition(companyId, subscriptionId, SubscriptionStatus.PAST_DUE); }
    @Transactional public SubscriptionResponse cancel(Long companyId, Long subscriptionId) { return transition(companyId, subscriptionId, SubscriptionStatus.CANCELLED); }
    @Transactional public SubscriptionResponse expire(Long companyId, Long subscriptionId) { return transition(companyId, subscriptionId, SubscriptionStatus.EXPIRED); }

    private SubscriptionResponse transition(Long companyId, Long subscriptionId, SubscriptionStatus target) {
        currentUser.requireRole(UserRole.PLATFORM_ADMIN);
        companies.findForUpdate(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        Subscription subscription = subscriptions.findByIdForUpdate(subscriptionId)
                .filter(value -> value.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Subscription no encontrada"));
        if (subscription.getStatus() == target) return mapper.toResponse(subscription);
        Instant now = Instant.now();
        switch (target) {
            case ACTIVE -> {
                if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT && subscription.getStatus() != SubscriptionStatus.PAST_DUE)
                    throw invalid(subscription.getStatus(), target);
                subscription.activate(now);
            }
            case PAST_DUE -> {
                if (subscription.getStatus() != SubscriptionStatus.ACTIVE) throw invalid(subscription.getStatus(), target);
                subscription.markPastDue();
            }
            case CANCELLED -> {
                if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT && subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.PAST_DUE)
                    throw invalid(subscription.getStatus(), target);
                subscription.cancel(now);
            }
            case EXPIRED -> {
                if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.PAST_DUE)
                    throw invalid(subscription.getStatus(), target);
                subscription.expire(now);
            }
            default -> throw invalid(subscription.getStatus(), target);
        }
        return mapper.toResponse(subscription);
    }

    private BusinessException invalid(SubscriptionStatus from, SubscriptionStatus target) {
        return new BusinessException("No se puede cambiar una Subscription de " + from + " a " + target);
    }

    private void requireCompany(Long companyId) {
        currentUser.requireRole(UserRole.PLATFORM_ADMIN);
        if (!companies.existsById(companyId)) throw new ResourceNotFoundException("Empresa no encontrada");
    }
}
