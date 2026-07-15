package com.shopsphere.security;

import com.shopsphere.entity.User;
import com.shopsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a {@link User} for Spring Security by username OR email (the same
 * dual-identifier lookup used by the login business logic), and wraps it in
 * a {@link CustomUserDetails}. Roles/authorities are eagerly fetched on the
 * User entity itself, so no additional query is needed here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    log.warn("UserDetailsService: no user found for identifier='{}'", usernameOrEmail);
                    return new UsernameNotFoundException("No user found with username/email: " + usernameOrEmail);
                });
        return new CustomUserDetails(user);
    }

    /** Convenience overload used internally (e.g. by JwtAuthenticationFilter) once the id is already known. */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with id: " + userId));
        return new CustomUserDetails(user);
    }
}
