package com.verifipay.plan.mapper;
import com.verifipay.plan.entity.Plan;
import com.verifipay.plan.dto.PlanResponse;
import org.springframework.stereotype.Component;
@Component
public class PlanMapper {
    public PlanResponse toResponse(Plan p) {
        return new PlanResponse(p.getId(), p.getCode(), p.getName(), p.getPrice(), p.getCurrency(),
            p.getBillingInterval(), p.getMaxCashiers(), p.getMaxReceiverDevices(), p.getActive(),
            p.getCreatedAt(), p.getUpdatedAt());
    }
}
