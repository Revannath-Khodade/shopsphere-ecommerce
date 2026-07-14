package com.shopsphere.service;

import com.shopsphere.dto.request.UserRequest;
import com.shopsphere.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse getUserById(Long id);

    UserResponse getUserByUsername(String username);

    List<UserResponse> getAllUsers();

    UserResponse updateProfile(Long userId, UserRequest request);

    void deleteUser(Long userId);

    /** Disables (soft-deletes) a user account without removing history/data. */
    void deactivateUser(Long userId);
}
