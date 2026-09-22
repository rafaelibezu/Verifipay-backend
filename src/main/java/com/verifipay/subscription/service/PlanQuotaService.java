package com.verifipay.subscription.service;

import com.verifipay.common.enums.UserRole;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.company.entity.Company;
import com.verifipay.company.repository.CompanyRepository;
import com.verifipay.device.repository.ReceiverDeviceRepository;
import com.verifipay.subscription.repository.SubscriptionRepository;
import com.verifipay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PlanQuotaService {

    private final CompanyRepository companies;
    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final ReceiverDeviceRepository devices;

    public Company assertCanActivateCashier(Long companyId) {
        Company company = lockCompany(companyId);
        int limit = currentPlan(companyId).getPlan().getMaxCashiers();
        if (users.countByCompanyIdAndRoleAndActiveTrue(companyId, UserRole.CASHIER) >= limit) {
            throw new BusinessException("El Plan actual permite como máximo " + limit + " cajeros activos");
        }
        return company;
    }

    public Company assertCanActivateReceiverDevice(Long companyId) {
        Company company = lockCompany(companyId);
        int limit = currentPlan(companyId).getPlan().getMaxReceiverDevices();
        if (devices.countByCompanyIdAndActiveTrue(companyId) >= limit) {
            throw new BusinessException("El Plan actual permite como máximo " + limit + " dispositivos receptores activos");
        }
        return company;
    }

    private Company lockCompany(Long companyId) {
        return companies.findForUpdate(companyId)
                .orElseThrow(() -> new BusinessException("Empresa no encontrada"));
    }

    private com.verifipay.subscription.entity.Subscription currentPlan(Long companyId) {
        return subscriptions.findByCompanyIdAndEndedAtIsNull(companyId)
                .orElseThrow(() -> new BusinessException("La empresa no tiene una Subscription vigente"));
    }
}
