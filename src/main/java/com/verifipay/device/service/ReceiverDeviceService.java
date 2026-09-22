package com.verifipay.device.service;

import com.verifipay.branch.entity.Branch;
import com.verifipay.branch.repository.BranchRepository;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.device.dto.CreateReceiverDeviceRequest;
import com.verifipay.device.dto.ReceiverDeviceCreatedResponse;
import com.verifipay.device.entity.ReceiverDevice;
import com.verifipay.device.repository.ReceiverDeviceRepository;
import com.verifipay.device.util.DeviceTokenUtil;
import com.verifipay.security.CurrentUserService;
import com.verifipay.user.entity.User;
import com.verifipay.common.enums.UserRole;
import com.verifipay.subscription.service.PlanQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceiverDeviceService {

    private final ReceiverDeviceRepository deviceRepository;
    private final BranchRepository branchRepository;
    private final CurrentUserService currentUserService;
    private final DeviceTokenUtil deviceTokenUtil;
    private final PlanQuotaService quotas;

    @Transactional
    public ReceiverDeviceCreatedResponse create(
            CreateReceiverDeviceRequest request
    ) {

        User admin = currentUserService.requireRole(UserRole.OWNER);

        var company = quotas.assertCanActivateReceiverDevice(admin.getCompany().getId());

        Branch branch =
                branchRepository
                        .findById(request.branchId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Local no encontrado"
                                )
                        );

        if (!Boolean.TRUE.equals(branch.getActive()) || !branch.getCompany()
                .getId()
                .equals(
                        admin.getCompany().getId()
                )) {

            throw new BusinessException(
                    "El local no pertenece a tu empresa"
            );
        }

        String token =
                deviceTokenUtil.generateToken();

        ReceiverDevice device =
                new ReceiverDevice();

        device.setCompany(
                company
        );

        device.setBranch(branch);

        device.setName(
                request.name().trim()
        );

        device.setTokenHash(
                deviceTokenUtil.hash(token)
        );

        device.setActive(true);

        device =
                deviceRepository.save(device);

        return new ReceiverDeviceCreatedResponse(
                device.getId(),
                device.getName(),
                branch.getId(),
                branch.getName(),
                token
        );
    }

    @Transactional
    public void deactivate(Long id) {
        User owner = currentUserService.requireRole(UserRole.OWNER);
        ReceiverDevice device = deviceRepository.findById(id)
                .filter(value -> value.getCompany().getId().equals(owner.getCompany().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo receptor no encontrado"));
        device.setActive(false);
    }
}
