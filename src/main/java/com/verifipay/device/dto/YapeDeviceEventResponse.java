package com.verifipay.device.dto;

public record YapeDeviceEventResponse(

        Long paymentEventId,
        boolean duplicate

) {
}