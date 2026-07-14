package com.shopsphere.mapper;

import com.shopsphere.dto.response.UserResponse;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts between the User entity and its public-facing DTOs.
 * Manual mapping is used (rather than blind ModelMapper reflection) because
 * the password hash must never leak into a response, and roles need to be
 * flattened from Role entities to plain role-name strings.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .enabled(user.getEnabled())
                .roles(mapRoleNames(user.getRoles()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private Set<String> mapRoleNames(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}
