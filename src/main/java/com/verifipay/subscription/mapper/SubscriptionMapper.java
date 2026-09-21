package com.verifipay.subscription.mapper;
import com.verifipay.subscription.entity.Subscription;
import com.verifipay.subscription.dto.SubscriptionResponse;
import org.springframework.stereotype.Component;
@Component
public class SubscriptionMapper {
    public SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(s.getId(), s.getCompany().getId(), s.getPlan().getId(), s.getStatus(),
            s.getContractedPrice(), s.getCurrency(), s.getBillingInterval(), s.getStartedAt(),
            s.getEndedAt(), s.getCancelledAt(), s.getVersion(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
