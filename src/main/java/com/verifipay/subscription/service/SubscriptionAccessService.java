package com.verifipay.subscription.service;

import com.verifipay.common.enums.SubscriptionStatus;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.subscription.entity.Subscription;
import com.verifipay.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class SubscriptionAccessService {
    private final SubscriptionRepository subscriptions;

    public Subscription requireOperationalAccess(Long companyId) {
        Subscription subscription = subscriptions.findByCompanyIdAndEndedAtIsNull(companyId)
                .orElseThrow(() -> new BusinessException("La empresa no tiene una Subscription vigente"));
        if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT) {
            throw new BusinessException("La Subscription de la empresa está pendiente de pago");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            throw new BusinessException("La Subscription de la empresa no tiene acceso operativo");
        }
        return subscription;
    }
}
