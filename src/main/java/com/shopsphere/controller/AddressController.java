package com.shopsphere.controller;

import com.shopsphere.dto.ApiResponse;
import com.shopsphere.dto.request.AddressRequest;
import com.shopsphere.dto.response.AddressResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Address book management for the authenticated user")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @Operation(summary = "Add a new address", description = "The first address a user adds is automatically set as their default.")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                        @Valid @RequestBody AddressRequest request) {
        log.info("Adding address for userId={}", currentUser.getId());
        AddressResponse response = addressService.addAddress(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                        @Parameter(description = "Address id") @PathVariable Long id,
                                                                        @Valid @RequestBody AddressRequest request) {
        log.info("Updating addressId={} for userId={}", id, currentUser.getId());
        AddressResponse response = addressService.updateAddress(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                             @Parameter(description = "Address id") @PathVariable Long id) {
        log.info("Deleting addressId={} for userId={}", id, currentUser.getId());
        addressService.deleteAddress(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.message("Address deleted successfully"));
    }

    @GetMapping
    @Operation(summary = "List every address for the current user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<AddressResponse> response = addressService.getAddressesForUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/default")
    @Operation(summary = "Get the current user's default address")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        AddressResponse response = addressService.getDefaultAddress(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set an existing address as the default", description = "Clears the previous default, if any.")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Address id") @PathVariable Long id) {
        log.info("Setting addressId={} as default for userId={}", id, currentUser.getId());
        AddressResponse response = addressService.setDefaultAddress(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Default address updated successfully", response));
    }
}
