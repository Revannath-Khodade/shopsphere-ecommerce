package com.shopsphere.controller;

import com.shopsphere.dto.ApiResponse;
import com.shopsphere.dto.request.UserRequest;
import com.shopsphere.dto.response.UserResponse;
import com.shopsphere.security.CustomUserDetails;
import com.shopsphere.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Self-service profile management (any authenticated user) plus
 * administrative user management (ROLE_ADMIN only, enforced both at the
 * {@link com.shopsphere.config.SecurityConfig} path level and again here via
 * {@code @PreAuthorize} for defense in depth).
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile self-service and admin user management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get the current user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UserResponse response = userService.getUserById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update the current user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                                     @Valid @RequestBody UserRequest request) {
        log.info("Profile update requested by userId={}", currentUser.getId());
        UserResponse response = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete the current user's own account")
    public ResponseEntity<ApiResponse<Void>> deleteOwnProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Self-deletion requested by userId={}", currentUser.getId());
        userService.deleteUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.message("Account deleted successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any user by id (admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User id") @PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List every user in the system (admin only)")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete any user by id (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUserById(
            @Parameter(description = "User id") @PathVariable Long id) {
        log.info("Admin deletion requested for userId={}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.message("User deleted successfully"));
    }
}
