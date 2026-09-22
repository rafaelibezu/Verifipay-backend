package com.verifipay.device.repository;

import com.verifipay.device.entity.ReceiverDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiverDeviceRepository
        extends JpaRepository<ReceiverDevice, Long> {

    Optional<ReceiverDevice>
    findByTokenHashAndActiveTrue(String tokenHash);

    long countByCompanyIdAndActiveTrue(Long companyId);
}
