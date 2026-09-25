package com.verifipay.device.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.device.dto.CreateReceiverDeviceRequest;
import com.verifipay.device.dto.ReceiverDeviceCurrentResponse;
import com.verifipay.device.dto.ReceiverDeviceCreatedResponse;
import com.verifipay.device.service.ReceiverDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/owner/receiver-devices"
)
@RequiredArgsConstructor
public class ReceiverDeviceAdminController {

    private final ReceiverDeviceService receiverDeviceService;

        @GetMapping("/current")
        public ApiResponse<ReceiverDeviceCurrentResponse> current() {
                return ApiResponse.success(
                                "Dispositivo receptor obtenido correctamente",
                                receiverDeviceService.current()
                );
        }

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

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        receiverDeviceService.deactivate(id);
        return ApiResponse.success("Dispositivo receptor desactivado correctamente", null);
    }
}
