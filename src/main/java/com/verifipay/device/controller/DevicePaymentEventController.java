package com.verifipay.device.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.device.dto.YapeDeviceEventRequest;
import com.verifipay.device.dto.YapeDeviceEventResponse;
import com.verifipay.device.service.DevicePaymentEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/device/payment-events")
@RequiredArgsConstructor
public class DevicePaymentEventController {

    private final DevicePaymentEventService devicePaymentEventService;

    @PostMapping("/yape")
    public ResponseEntity<
            ApiResponse<YapeDeviceEventResponse>
            > receiveYape(

            @RequestHeader("X-Device-Token")
            String deviceToken,

            @Valid
            @RequestBody
            YapeDeviceEventRequest request
    ) {

        YapeDeviceEventResponse response =
                devicePaymentEventService.receiveYapeEvent(
                        deviceToken,
                        request
                );

        String message =
                response.duplicate()
                        ? "Evento ya registrado"
                        : "Pago Yape recibido";

        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        response
                )
        );
    }
}