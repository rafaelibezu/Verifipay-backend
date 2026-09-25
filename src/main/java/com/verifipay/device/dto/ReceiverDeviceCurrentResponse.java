package com.verifipay.device.dto;

import java.time.LocalDateTime;

public record ReceiverDeviceCurrentResponse(
        Long id,
        String name,
        Long branchId,
        String branchName,
        LocalDateTime lastSeenAt
) {
}