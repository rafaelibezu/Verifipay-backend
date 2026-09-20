package com.verifipay.device.dto;

public record ReceiverDeviceCreatedResponse(

        Long id,
        String name,

        Long branchId,
        String branchName,

        String deviceToken

) {
}