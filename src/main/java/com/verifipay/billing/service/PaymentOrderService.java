package com.verifipay.billing.service;

import com.verifipay.billing.dto.PaymentOrderResponse;
import com.verifipay.billing.entity.PaymentOrder;
import com.verifipay.billing.gateway.PaymentGateway;
import com.verifipay.billing.repository.PaymentOrderRepository;
import com.verifipay.common.enums.*;
import com.verifipay.common.exception.*;
import com.verifipay.company.entity.Company;
import com.verifipay.company.repository.CompanyRepository;
import com.verifipay.security.CurrentUserService;
import com.verifipay.subscription.entity.Subscription;
import com.verifipay.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentOrderService {

    private final CompanyRepository companies;
    private final SubscriptionRepository subscriptions;
    private final PaymentOrderRepository orders;
    private final CurrentUserService current;
    private final ObjectProvider<PaymentGateway> gateways;

    @Transactional
    public PaymentOrderResponse create(Long companyId, Long subscriptionId, String idempotencyKey) {
        current.requireRole(UserRole.PLATFORM_ADMIN);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException("Idempotency-Key es obligatorio");
        }
        if (idempotencyKey.length() > 100) {
            throw new BusinessException("Idempotency-Key no puede exceder 100 caracteres");
        }

        Company company = companies.findForUpdate(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Subscription subscription = subscriptions.findByIdForUpdate(subscriptionId)
                .filter(value -> value.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Subscription no encontrada"));

        var existing = orders.findByCompanyIdAndIdempotencyKey(companyId, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getSubscription().getId().equals(subscriptionId)) {
                throw new BusinessException("La Idempotency-Key ya fue usada para otra Subscription");
            }
            return response(existing.get());
        }

        if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT || subscription.getEndedAt() != null) {
            throw new BusinessException("La Subscription debe estar pendiente de pago y vigente");
        }

        if (orders.findBySubscriptionIdAndStatus(subscriptionId, PaymentOrderStatus.PENDING).isPresent()) {
            throw new BusinessException("Ya existe una orden de pago pendiente para esta Subscription");
        }

        PaymentGateway gateway = gateways.getIfAvailable();
        if (gateway == null) {
            throw new BusinessException("No hay un gateway de pago habilitado");
        }

        return response(orders.save(new PaymentOrder(company, subscription, idempotencyKey, UUID.randomUUID().toString(), gateway.provider())));
    }

    @Transactional
    public PaymentOrderResponse confirmPaid(Long companyId, Long subscriptionId, Long orderId) {
        current.requireRole(UserRole.PLATFORM_ADMIN);

        companies.findForUpdate(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Subscription subscription = subscriptions.findByIdForUpdate(subscriptionId)
                .filter(value -> value.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Subscription no encontrada"));

        PaymentOrder order = orders.findByIdForUpdate(orderId)
                .filter(value -> value.getCompany().getId().equals(companyId) && value.getSubscription().getId().equals(subscriptionId))
                .orElseThrow(() -> new ResourceNotFoundException("Orden de pago no encontrada"));

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            return response(order);
        }

        if (order.getStatus() != PaymentOrderStatus.PENDING) {
            throw new BusinessException("La orden de pago no puede confirmarse");
        }

        if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
            throw new BusinessException("La Subscription no está pendiente de pago; la orden requiere revisión");
        }

        Instant now = Instant.now();
        order.markPaid(now);
        subscription.activate(now);

        return response(order);
    }

    private PaymentOrderResponse response(PaymentOrder p) {
        return new PaymentOrderResponse(
                p.getId(),
                p.getCompany().getId(),
                p.getSubscription().getId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getProvider(),
                p.getExternalReference(),
                p.getProviderTransactionId(),
                p.getPaidAt(),
                p.getFailedAt(),
                p.getCancelledAt(),
                p.getExpiresAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
