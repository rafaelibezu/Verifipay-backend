package com.verifipay.user.controller;
import com.verifipay.common.api.ApiResponse; import com.verifipay.user.dto.*; import com.verifipay.user.service.OwnerCashierService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/v1/owner/cashiers") @RequiredArgsConstructor
public class OwnerCashierController { private final OwnerCashierService service;
 @PostMapping public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateCashierRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(r)));}
 @GetMapping public ApiResponse<List<UserResponse>> all(){return ApiResponse.success(service.findAll());}
 @GetMapping("/{id}") public ApiResponse<UserResponse> one(@PathVariable Long id){return ApiResponse.success(service.find(id));}
 @PutMapping("/{id}") public ApiResponse<UserResponse> update(@PathVariable Long id,@Valid @RequestBody UpdateCashierRequest r){return ApiResponse.success(service.update(id,r));}
 @PatchMapping("/{id}/status") public ApiResponse<UserResponse> status(@PathVariable Long id,@RequestParam boolean active){return ApiResponse.success(service.status(id,active));}}
