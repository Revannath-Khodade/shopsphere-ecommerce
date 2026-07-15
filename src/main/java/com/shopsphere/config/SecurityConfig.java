package com.shopsphere.config;

import com.shopsphere.security.CustomUserDetailsService;
import com.shopsphere.security.JwtAccessDeniedHandler;
import com.shopsphere.security.JwtAuthenticationEntryPoint;
import com.shopsphere.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central Spring Security 6 configuration for ShopSphere's stateless,
 * JWT-based API.
 * <p>
 * Design summary:
 * <ul>
 *   <li>CSRF is disabled - CSRF protection defends session-cookie-based auth
 *       from cross-site form submission; it is irrelevant to a stateless API
 *       authenticated via an Authorization header bearer token, which browsers
 *       never attach automatically to cross-origin requests.</li>
 *   <li>Session creation policy is STATELESS - no HttpSession is created or
 *       consulted; every request re-authenticates from its bearer token via
 *       {@link JwtAuthenticationFilter}.</li>
 *   <li>{@link JwtAuthenticationFilter} runs before Spring Security's built-in
 *       {@link UsernamePasswordAuthenticationFilter} so a valid bearer token
 *       populates the SecurityContext before any form-login machinery would run.</li>
 *   <li>Authorization rules are declared here via {@code requestMatchers(...)}
 *       for coarse-grained, path-based rules; method-level {@code @PreAuthorize}
 *       / {@code @Secured} (enabled via {@link EnableMethodSecurity}) are
 *       available to controllers in the next phase for finer-grained checks.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Value("${app.security.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Endpoints reachable without authentication: registration/login/refresh,
     * error dispatch, and (once added in a later phase) API documentation.
     * Product/category browsing is public read-only, matching a typical
     * storefront's "browse without an account" expectation.
     * <p>
     * NOTE: {@code server.servlet.context-path=/api} is already configured in
     * application.properties, so the servlet container strips the "/api"
     * prefix before Spring Security (or Spring MVC) ever sees the request
     * path. These patterns are intentionally context-path-relative (e.g.
     * "/auth/**", not "/api/auth/**") - the externally-visible URL a client
     * calls is still e.g. http://host:8080/api/auth/login.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/products/**",
            "/categories/**",
            "/reviews/product/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Wires our own {@link CustomUserDetailsService} + {@link PasswordEncoder}
     * into a {@link DaoAuthenticationProvider}, which is what actually checks
     * credentials during the login flow's call to {@link AuthenticationManager#authenticate}.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes Spring Security's AuthenticationManager as a bean so
     * AuthenticationServiceImpl can inject it and delegate credential
     * checking to {@link #authenticationProvider()} instead of manually
     * comparing password hashes.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless bearer-token API: CSRF protection is unnecessary and
                // would only get in the way of non-browser clients.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()

                        // ---------------- Admin-only ----------------
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/**").hasRole("ADMIN")

                        // ---------------- Seller-only ----------------
                        .requestMatchers("/seller/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasAnyRole("SELLER", "ADMIN")

                        // ---------------- Customer + Seller + Admin (any authenticated shopper) ----------------
                        .requestMatchers("/cart/**", "/orders/**", "/wishlist/**",
                                "/addresses/**", "/payments/**")
                            .hasAnyRole("CUSTOMER", "SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/reviews/**").hasAnyRole("CUSTOMER", "SELLER", "ADMIN")

                        // Everything else requires, at minimum, a valid authenticated principal.
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS policy for the React SPA, sourced from
     * {@code app.security.cors.allowed-origins} so different environments
     * (local dev, staging, production) can each configure their own
     * allow-list without a code change.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
