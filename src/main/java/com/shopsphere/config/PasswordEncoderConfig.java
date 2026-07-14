package com.shopsphere.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Exposes a {@link PasswordEncoder} bean so the service layer can hash and
 * verify passwords during registration/login.
 * <p>
 * This is intentionally NOT a full Spring Security configuration (no filter
 * chain, no authentication manager, no JWT) - that is out of scope for this
 * phase and will be added in the dedicated Security phase. This bean only
 * provides the one-way hashing primitive that AuthenticationServiceImpl needs
 * to do its job correctly and securely today.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
