package com.verifipay.user.controller;

import com.verifipay.common.api.ApiResponse;
import com.verifipay.user.dto.CreateUserRequest;
import com.verifipay.user.dto.UpdateUserRequest;
import com.verifipay.user.dto.UserResponse;
import com.verifipay.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateUserRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Usuario creado correctamente",
                                userService.create(request)
                        )
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.findById(id)
                )
        );
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findAllByCompany(
            @PathVariable Long companyId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.findAllByCompany(companyId)
                )
        );
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findAllByBranch(
            @PathVariable Long branchId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.findAllByBranch(branchId)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Usuario actualizado correctamente",
                        userService.update(id, request)
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        active
                                ? "Usuario activado correctamente"
                                : "Usuario desactivado correctamente",
                        userService.changeStatus(id, active)
                )
        );
    }
}