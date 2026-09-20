package com.verifipay.device.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.device.dto.CreateReceiverDeviceRequest;
import com.verifipay.device.dto.ReceiverDeviceCreatedResponse;
import com.verifipay.device.service.ReceiverDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/admin/receiver-devices"
)
@RequiredArgsConstructor
public class ReceiverDeviceAdminController {

    private final ReceiverDeviceService receiverDeviceService;

    @PostMapping
    public ResponseEntity<
            ApiResponse<ReceiverDeviceCreatedResponse>
            > create(

            @Valid
            @RequestBody
            CreateReceiverDeviceRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dispositivo receptor creado correctamente",
                        receiverDeviceService.create(
                                request
                        )
                )
        );
    }
}